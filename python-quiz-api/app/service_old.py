from __future__ import annotations

import json
import os
import random
import re
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any

from .models import QuizGenerationRequest, QuizGenerationResponse


VALID_OPTIONS = {"A", "B", "C", "D"}
VAGUE_PATTERNS = (
    "quelle approche",
    "concept clé",
    "règle métier",
    "point d'architecture",
    "mécanisme de sécurité",
)
NON_CHALLENGING_OPTION_PATTERNS = (
    "je ne sais pas",
    "toutes les réponses",
    "aucune des réponses",
    "au hasard",
    "sans analyse",
)
META_OPTION_PATTERNS = (
    "l'option qui",
    "une option qui",
    "la réponse qui",
    "une formulation",
    "une approche",
    "une interprétation",
)
JAVA_LEVEL_BLUEPRINT: dict[int, list[str]] = {
    1: [
        "difference entre == et equals()",
        "encapsulation en Java",
        "classe vs objet",
        "mot-clé static",
        "exception non capturée",
    ],
    2: [
        "ArrayList vs LinkedList",
        "interface vs classe abstraite",
        "fonctionnement HashMap",
        "polymorphisme",
        "checked vs unchecked exceptions",
    ],
    3: [
        "Garbage Collector",
        "synchronized vs Lock vs volatile",
        "cycle de création d'objet avec new",
        "prévention deadlock",
        "Thread vs ExecutorService",
    ],
    4: [
        "Spring Dependency Injection interne",
        "optimisation mémoire Java",
        "CAP theorem en microservices",
        "architecture pour 1M messages/jour",
        "investigation API Spring Boot lente",
    ],
}
JAVA_LEVEL_DOMAINS: dict[int, str] = {
    1: "fondamentaux Java",
    2: "POO, collections et exceptions",
    3: "concurrence, JVM et performance",
    4: "architecture, Spring et microservices",
}

GENERATION_PROMPT = """
Tu es un générateur senior de QCM techniques.
Génère un nouveau quiz à chaque appel.
Ne réutilise jamais les mêmes questions et ne dépends pas d'un référentiel statique.
Retourne uniquement du JSON avec ce schéma :
{
  "quizTitle": "string",
  "questions": [
    {
      "question": "string",
      "options": [
        {"code":"A","text":"string","correct":true|false},
        {"code":"B","text":"string","correct":true|false},
        {"code":"C","text":"string","correct":true|false},
        {"code":"D","text":"string","correct":true|false}
      ]
    }
  ]
}
Règles :
- exactement une seule bonne réponse par question
- exactement 4 options A/B/C/D
- générer exactement le nombre de questions demandé
- respecter strictement le niveau déclaré :
  - niveau 1 : débutant (fondamentaux uniquement)
  - niveau 2 : intermédiaire (implémentation autonome)
  - niveau 3 : confirmé (arbitrages, debugging, optimisation)
  - niveau 4/5 : expert (architecture, fiabilité, cas complexes)
- le texte doit être en français (termes techniques autorisés en anglais)
- adapter les questions à la compétence demandée (java, python, etc.)
- formulation professionnelle et concrète, sans trivia
- questions variées et différentes à chaque appel
- ne pas produire des questions expertes pour un niveau débutant
- les mauvaises réponses doivent être plausibles, proches de la bonne et nuancées
- éviter les distracteurs évidents, jokers, placeholders et réponses vagues
- mélanger l'ordre des options
- éviter de répéter les mêmes distracteurs entre questions
- pour Java, style certification :
  - amorces concises ("Quelle est...", "Quelle affirmation est correcte...", "Quel est le résultat...")
  - inclure du comportement API/code concret, avec parfois un snippet
  - les options doivent être des affirmations techniques directes (pas de méta-commentaire)
""".strip()


@dataclass
class QuizApiError(Exception):
    code: str
    message: str
    details: dict[str, Any] | None = None


def generate_block(request: QuizGenerationRequest) -> QuizGenerationResponse:
    generated = None
    fallback_reason = None
    if llm_generation_enabled():
        try:
            generated = generate_with_llm(request)
        except QuizApiError as error:
            fallback_reason = error.message
    if generated is None:
        generated = generate_fallback_payload(request)
        fallback_reason = fallback_reason or "Synthetic fallback quiz generation was used"
    validate_generated_payload(generated, request.questionCount)
    return build_response(generated, request.level, fallback_reason)


def health_payload() -> dict[str, str]:
    return {"status": "ok"}


def llm_generation_enabled() -> bool:
    return (
        os.getenv("QUIZ_ENABLE_LLM_GENERATION", "true").lower() == "true"
        and bool(os.getenv("OPENAI_API_KEY"))
    )


def generate_with_llm(request: QuizGenerationRequest) -> dict[str, Any]:
    level_name = level_name_for(request.level)
    attempts = int(os.getenv("QUIZ_OPENAI_ATTEMPTS", "3"))
    last_error: QuizApiError | None = None

    for _ in range(max(1, attempts)):
        nonce = f"{uuid.uuid4()}-{datetime.now(timezone.utc).isoformat()}"
        focus_areas = build_focus_areas(request.skill, request.level, request.questionCount)
        question_blueprint = build_question_blueprint(request.skill, request.level, request.questionCount)
        user_prompt = json.dumps(
            {
                "skill": request.skill,
                "level": request.level,
                "declaredLevel": level_name,
                "questionCount": request.questionCount,
                "instructions": request.instructions,
                "requiredFocusAreas": focus_areas,
                "questionBlueprint": question_blueprint,
                "nonce": nonce,
                "strictUniquenessInstruction": "Questions must be different from prior generations for same skill/level.",
                "difficultyGuardrail": f"All questions must match declared level '{level_name}' only.",
                "qualityGuardrail": "Use concrete technical scenarios (code, debugging, design decisions), avoid generic/vague wording.",
                "blueprintGuardrail": "Generate exactly one question per blueprint topic in order, without repeating wording.",
                "optionGuardrail": "A/B/C/D must all look plausible to a trained engineer; avoid obvious wrong choices.",
                "outputLanguage": "français",
                "skillAdaptationGuardrail": "Question wording and technical content must fit the requested skill only (java, python, etc.).",
                "styleExamples": build_style_examples(request.skill, request.level),
            },
            ensure_ascii=False,
        )
        response_text = call_openai_chat_completion(GENERATION_PROMPT, user_prompt)
        try:
            generated = json.loads(extract_json(response_text))
            validate_generated_payload(generated, request.questionCount)
            validate_quality_payload(
                generated,
                expected_focus_areas=focus_areas,
                skill=request.skill,
                level=request.level,
            )
            return generated
        except json.JSONDecodeError as error:
            last_error = QuizApiError("invalid-generator-output", "The LLM returned malformed JSON")
            continue
        except QuizApiError as error:
            last_error = error
            continue

    raise last_error or QuizApiError("invalid-generator-output", "LLM generation failed quality checks")


def call_openai_chat_completion(system_prompt: str, user_prompt: str) -> str:
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise QuizApiError("llm-error", "OPENAI_API_KEY is required for LLM generation")

    request_body = {
        "model": os.getenv("QUIZ_OPENAI_MODEL", "gpt-4.1-mini"),
        "temperature": float(os.getenv("QUIZ_OPENAI_TEMPERATURE", "0.85")),
        "top_p": float(os.getenv("QUIZ_OPENAI_TOP_P", "0.9")),
        "response_format": {"type": "json_object"},
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
    }
    endpoint = os.getenv("QUIZ_OPENAI_BASE_URL", "https://api.openai.com/v1/chat/completions")
    http_request = urllib.request.Request(
        endpoint,
        data=json.dumps(request_body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        },
        method="POST",
    )

    try:
        timeout = float(os.getenv("QUIZ_OPENAI_TIMEOUT_SECONDS", "12"))
        with urllib.request.urlopen(http_request, timeout=timeout) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise QuizApiError("llm-error", f"OpenAI request failed: {detail}") from error
    except OSError as error:
        raise QuizApiError("llm-error", f"OpenAI request failed: {error}") from error

    try:
        return payload["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as error:
        raise QuizApiError("invalid-generator-output", "The OpenAI response did not contain JSON content") from error


def extract_json(content: str) -> str:
    stripped = content.strip()
    if stripped.startswith("```"):
        stripped = stripped.strip("`")
        stripped = stripped.replace("json\n", "", 1).strip()
    return stripped


def validate_generated_payload(generated: dict[str, Any], expected_question_count: int) -> None:
    questions = generated.get("questions")
    if not isinstance(questions, list) or len(questions) != expected_question_count:
        raise QuizApiError("invalid-generator-output", "The generated payload does not contain the expected number of questions")

    for question in questions:
        if not isinstance(question, dict):
            raise QuizApiError("invalid-generator-output", "A generated question has an invalid format")
        if not question.get("question"):
            raise QuizApiError("invalid-generator-output", "A generated question is missing question text")
        options = question.get("options")
        if not isinstance(options, list) or len(options) != 4:
            raise QuizApiError("invalid-generator-output", "A generated question must contain exactly 4 options")
        seen_codes = set()
        correct_count = 0
        for option in options:
            code = str(option.get("code", "")).upper()
            if code not in VALID_OPTIONS:
                raise QuizApiError("invalid-generator-output", "An option code is invalid")
            if code in seen_codes:
                raise QuizApiError("invalid-generator-output", "Option codes must be unique per question")
            seen_codes.add(code)
            if not option.get("text"):
                raise QuizApiError("invalid-generator-output", "An option text is missing")
            if option.get("correct") is True:
                correct_count += 1
        if correct_count != 1:
            raise QuizApiError("invalid-generator-output", "Each question must contain exactly one correct option")


def validate_quality_payload(
    generated: dict[str, Any],
    expected_focus_areas: list[str] | None = None,
    skill: str | None = None,
    level: int | None = None,
) -> None:
    questions = generated.get("questions", [])
    normalized_questions: list[str] = []
    seen_wrong_options: set[str] = set()

    for question in questions:
        text = str(question.get("question", "")).strip()
        if len(text) < 35:
            raise QuizApiError("invalid-generator-output", "A generated question is too short and likely vague")

        lowered = text.lower()
        if any(pattern in lowered for pattern in VAGUE_PATTERNS):
            raise QuizApiError("invalid-generator-output", "A generated question is too generic")

        compact = re.sub(r"[^a-z0-9]+", " ", lowered)
        normalized_questions.append(compact.strip())

        options = question.get("options", [])
        option_texts = [str(opt.get("text", "")).strip() for opt in options]
        if len(set(t.lower() for t in option_texts)) != len(option_texts):
            raise QuizApiError("invalid-generator-output", "A question contains repetitive answer options")

        correct_text = next((str(opt.get("text", "")).strip() for opt in options if opt.get("correct") is True), "")
        if not correct_text:
            raise QuizApiError("invalid-generator-output", "A question is missing a valid correct option")

        for opt in options:
            if opt.get("correct") is True:
                continue
            wrong_text = str(opt.get("text", "")).strip()
            wrong_lower = wrong_text.lower()
            if any(pattern in wrong_lower for pattern in NON_CHALLENGING_OPTION_PATTERNS):
                raise QuizApiError("invalid-generator-output", "A distractor is too weak or non-pertinent")
            if len(wrong_text) < max(12, int(len(correct_text) * 0.45)):
                raise QuizApiError("invalid-generator-output", "A distractor is too short to be challenging")
            if any(pattern in wrong_lower for pattern in META_OPTION_PATTERNS):
                raise QuizApiError("invalid-generator-output", "Distractor uses meta wording instead of technical content")
            normalized_wrong = re.sub(r"[^a-z0-9]+", " ", wrong_lower).strip()
            if normalized_wrong in seen_wrong_options:
                raise QuizApiError("invalid-generator-output", "Distractors are repetitive across questions")
            if is_similar_to_seen_distractor(normalized_wrong, seen_wrong_options):
                raise QuizApiError("invalid-generator-output", "Distractors are too similar across questions")
            seen_wrong_options.add(normalized_wrong)

    if len(set(normalized_questions)) != len(normalized_questions):
        raise QuizApiError("invalid-generator-output", "Generated questions are repetitive")

    for i in range(len(normalized_questions)):
        for j in range(i + 1, len(normalized_questions)):
            if text_similarity(normalized_questions[i], normalized_questions[j]) >= 0.68:
                raise QuizApiError("invalid-generator-output", "Generated questions are too similar")

    normalized_skill = (skill or "").strip().lower().replace(" ", "_")
    if normalized_skill == "java" and expected_focus_areas:
        validate_java_blueprint_coverage(generated, expected_focus_areas, level)


def build_response(generated: dict[str, Any], level: int, fallback_reason: str | None) -> QuizGenerationResponse:
    raw_questions = generated.get("questions", [])
    questions = []
    expected_answers = []

    for index, question in enumerate(raw_questions, start=1):
        randomized_options = randomize_options(question["options"])
        options = {code: text for code, text, _ in randomized_options}
        correct_option = next(code for code, _, correct in randomized_options if correct is True)
        question_id = f"q{index}"
        expected_answers.append({"questionId": question_id, "option": correct_option})
        questions.append(
            {
                "id": question_id,
                "topic": str(generated.get("quizTitle") or "General"),
                "targetedOutcome": f"Level {level} mastery",
                "text": question["question"],
                "optionA": options.get("A"),
                "optionB": options.get("B"),
                "optionC": options.get("C"),
                "optionD": options.get("D"),
                "explanation": "Generated by LLM",
            }
        )

    return QuizGenerationResponse.model_validate(
        {
            "title": generated.get("quizTitle") or "Quiz technique",
            "questions": questions,
            "expectedAnswers": expected_answers,
            "difficulty": {
                "level": level,
                "label": f"Niveau {level}",
                "description": f"Quiz généré dynamiquement niveau {level}",
            },
            "durationMinutes": max(10, len(questions) * 3),
            "evaluationCriteria": [
                "Exactly one correct answer per question",
                "Difficulty aligned with requested level",
                "Freshly generated questions at each call",
            ],
            "generationSource": "llm" if fallback_reason is None else "fallback",
            "fallbackReason": fallback_reason,
        }
    )


def generate_fallback_payload(request: QuizGenerationRequest) -> dict[str, Any]:
    rng = random.Random(f"{request.skill}:{request.level}:{uuid.uuid4().hex}")
    title = f"{humanize_skill_name(request.skill)} - {level_name_for(request.level)}"
    focus_areas = build_focus_areas(request.skill, request.level, request.questionCount)
    if request.skill.strip().lower().replace(" ", "_") == "java":
        return generate_java_fallback_payload(request, title, focus_areas, rng)

    questions: list[dict[str, Any]] = []
    template = build_template_for_fallback(request.skill, request.level)
    used_wrong_options: set[str] = set()

    for index in range(request.questionCount):
        area = focus_areas[index % len(focus_areas)]
        question_text, correct_text, wrong_texts = template(rng, index + 1, area, used_wrong_options)
        options = [
            {"code": "A", "text": correct_text, "correct": True},
            {"code": "B", "text": wrong_texts[0], "correct": False},
            {"code": "C", "text": wrong_texts[1], "correct": False},
            {"code": "D", "text": wrong_texts[2], "correct": False},
        ]
        rng.shuffle(options)
        questions.append({"question": question_text, "options": options})

    return {"quizTitle": title, "questions": questions}


def humanize_skill_name(skill: str) -> str:
    cleaned = skill.replace("_", " ").strip()
    return cleaned[:1].upper() + cleaned[1:] if cleaned else "Quiz"


def build_template_for_fallback(skill: str, level: int):
    skill_label = humanize_skill_name(skill)
    level_label = level_name_for(level)
    difficulty_hint = level_hint_for(level)

    def template(rng: random.Random, number: int, area: str, used_wrong_options: set[str]):
        contexts = [
            "revue de code",
            "incident en production",
            "pull request critique",
            "optimisation d'un service",
            "mise en place d'un nouveau module",
        ]
        context = rng.choice(contexts)
        question = (
            f"Question {number} : en {skill_label}, dans un contexte de {context}, "
            f"quelle décision est la plus adaptée pour '{area}' au niveau {level_label} ?"
        )
        correct = f"Choisir une solution concrète et justifiée sur {area}, alignée avec {difficulty_hint}"
        wrong = build_challenging_wrong_options(area, level, rng, used_wrong_options)
        return question, correct, wrong

    return template


def level_name_for(level: int) -> str:
    if level <= 1:
        return "Débutant"
    if level == 2:
        return "Intermédiaire"
    if level == 3:
        return "Confirmé"
    return "Expert"


def level_hint_for(level: int) -> str:
    if level <= 1:
        return "fondamentaux et cas simples"
    if level == 2:
        return "implémentation autonome et cas usuels"
    if level == 3:
        return "raisonnement, arbitrages et diagnostic"
    return "architecture, fiabilité et complexité avancée"


def level_domain_for(level: int) -> str:
    return JAVA_LEVEL_DOMAINS.get(max(1, min(4, level)), JAVA_LEVEL_DOMAINS[1])


def build_focus_areas(skill: str, level: int, count: int) -> list[str]:
    normalized_skill = skill.strip().lower().replace(" ", "_")
    if normalized_skill == "java":
        base = list(JAVA_LEVEL_BLUEPRINT.get(max(1, min(4, level)), JAVA_LEVEL_BLUEPRINT[1]))
        if len(base) < count:
            while len(base) < count:
                base.extend(base[: max(1, count - len(base))])
        return base[:count]

    catalog = {
        "java": {
            1: ["types primitifs", "conditions et boucles", "collections de base", "exceptions simples", "POO basique"],
            2: ["streams simples", "gestion des exceptions", "API collections", "concurrence de base", "tests unitaires"],
            3: ["optimisation JVM", "thread safety", "conception orientée interfaces", "profiling mémoire", "tests d'intégration"],
            4: ["architecture hexagonale", "résilience distribuée", "performance sous charge", "patterns avancés", "sécurité applicative"],
        },
        "python": {
            1: ["types et structures", "conditions et boucles", "fonctions simples", "modules standards", "gestion d'erreurs basique"],
            2: ["list/dict comprehensions", "itérateurs et générateurs", "OOP Python", "tests pytest", "gestion des dépendances"],
            3: ["asyncio", "profiling performance", "typing avancé", "packaging", "qualité de code"],
            4: ["architecture de services", "concurrence avancée", "optimisation IO/CPU", "sécurité Python", "observabilité"],
        },
    }

    level_key = max(1, min(4, level))
    skill_catalog = catalog.get(normalized_skill, {})
    selected = list(skill_catalog.get(level_key, []))
    if not selected:
        selected = [
            "diagnostic de bug concret",
            "décision d'implémentation",
            "optimisation de performance",
            "sécurité applicative",
            "qualité et tests",
        ]

    random.shuffle(selected)
    if len(selected) < count:
        while len(selected) < count:
            selected.extend(selected[: max(1, count - len(selected))])
    return selected[:count]


def build_question_blueprint(skill: str, level: int, count: int) -> list[dict[str, str]]:
    focus_areas = build_focus_areas(skill, level, count)
    level_name = level_name_for(level)
    normalized_skill = skill.strip().lower().replace(" ", "_")
    return [
        {
            "questionNumber": str(index + 1),
            "topic": focus_areas[index],
            "difficultyLevel": level_name,
            "expectation": (
                f"Question concrète de type certification sur '{focus_areas[index]}'"
                if normalized_skill == "java"
                else f"Question concrète, non vague, sur '{focus_areas[index]}'"
            ),
        }
        for index in range(len(focus_areas))
    ]


def build_style_examples(skill: str, level: int) -> list[dict[str, Any]]:
    normalized_skill = skill.strip().lower().replace(" ", "_")
    domain = level_domain_for(level)
    if normalized_skill == "java":
        return [
            {
                "format": "verification-concept",
                "stem": "Quelle est la différence entre == et .equals() lors de la comparaison d'objets Java ?",
                "option_style": "toutes les options sont plausibles, une seule est totalement correcte",
                "difficulty_domain": domain,
            },
            {
                "format": "resultat-code",
                "stem": "Quel est le résultat de ce snippet Java ?",
                "snippet": "List<String> list = new ArrayList<>(); list.add(\"A\"); list.add(\"B\"); list.add(1, \"C\"); System.out.println(list);",
                "option_style": "inclure des sorties proches mais incorrectes",
                "difficulty_domain": domain,
            },
            {
                "format": "affirmation-correcte",
                "stem": "Quelle affirmation sur les interfaces, collections ou la concurrence est correcte ?",
                "option_style": "nuances techniques, pas de réponse évidente",
                "difficulty_domain": domain,
            },
        ]
    if normalized_skill == "python":
        return [
            {
                "format": "verification-concept",
                "stem": "Quelle est la différence entre une copie superficielle et une copie profonde en Python ?",
                "option_style": "distracteurs proches portant sur mutabilité et références",
                "difficulty_domain": level_hint_for(level),
            },
            {
                "format": "resultat-code",
                "stem": "Quel est le résultat de ce snippet Python ?",
                "snippet": "items = [1, 2, 3]; print(items[1:])",
                "option_style": "sorties plausibles avec pièges d'indexation/slicing",
                "difficulty_domain": level_hint_for(level),
            },
        ]
    return []


def randomize_options(options: list[dict[str, Any]]) -> list[tuple[str, str, bool]]:
    shuffled = [
        (str(opt.get("text", "")).strip(), opt.get("correct") is True)
        for opt in options
    ]
    random.SystemRandom().shuffle(shuffled)
    codes = ["A", "B", "C", "D"]
    return [(codes[index], text, correct) for index, (text, correct) in enumerate(shuffled)]


def build_challenging_wrong_options(area: str, level: int, rng: random.Random, used_wrong_options: set[str]) -> list[str]:
    area_lower = area.lower()

    if "sécurité" in area_lower:
        pool = [
            "Désactiver les contrôles en production pour simplifier le déploiement",
            "Centraliser tous les secrets directement dans le code source",
            "Reporter la gestion des vulnérabilités après la mise en production",
            "Accorder des permissions larges à tous les services internes",
        ]
    elif "performance" in area_lower or "profiling" in area_lower or "optimisation" in area_lower:
        pool = [
            "Augmenter les ressources sans identifier le goulot d'étranglement",
            "Optimiser des parties non critiques sans mesurer l'impact",
            "Mettre en cache globalement sans stratégie d'invalidation",
            "Paralléliser systématiquement sans vérifier la contention",
        ]
    elif "tests" in area_lower:
        pool = [
            "Se limiter aux tests manuels en préproduction",
            "Tester uniquement les cas nominaux sans cas limites",
            "Valider la qualité uniquement par la couverture de ligne",
            "Supprimer les tests instables sans analyser leur cause",
        ]
    else:
        pool = [
            "Appliquer le même pattern partout sans contexte",
            "Modifier plusieurs couches sans isoler les impacts",
            "Faire une correction rapide sans vérifier les effets de bord",
            "Ignorer la lisibilité du code au profit de la vitesse immédiate",
            "Choisir une implémentation sans critères de décision explicites",
        ]

    qualifiers = [
        "sans plan de mitigation",
        "sans vérifier les contraintes du système",
        "sans mesurer l'impact en production",
        "sans valider avec des tests ciblés",
        "sans traiter les cas limites",
    ]

    rng.shuffle(pool)
    rng.shuffle(qualifiers)

    selected: list[str] = []
    for candidate in pool:
        variant = candidate
        if level >= 3:
            qualifier = qualifiers[len(selected) % len(qualifiers)]
            variant = f"{candidate} {qualifier}"
        if try_add_wrong_option(variant, selected, used_wrong_options):
            if len(selected) == 3:
                return selected

    fallback_pool = [
        f"Traiter {area} avec une solution ad hoc sans critères de validation",
        f"Introduire un changement sur {area} sans plan de rollback",
        f"Corriger {area} en contournant les standards de qualité",
        f"Optimiser {area} sans métriques comparatives avant/après",
        f"Déployer une modification sur {area} sans revue technique",
        f"Appliquer une stratégie unique à {area} quel que soit le contexte",
        f"Masquer le problème de {area} plutôt que traiter la cause racine",
    ]
    rng.shuffle(fallback_pool)
    for fallback in fallback_pool:
        if try_add_wrong_option(fallback, selected, used_wrong_options):
            if len(selected) == 3:
                return selected

    # Guaranteed completion to avoid runtime failures.
    seed = 1
    while len(selected) < 3:
        guaranteed = f"Choix non pertinent pour {area} basé sur une hypothèse invalide variante {seed}"
        if try_add_wrong_option(guaranteed, selected, used_wrong_options, strict_similarity=False):
            seed += 1
            continue
        seed += 1

    return selected


def try_add_wrong_option(
    candidate: str,
    selected: list[str],
    used_wrong_options: set[str],
    strict_similarity: bool = True,
) -> bool:
    normalized = re.sub(r"[^a-z0-9]+", " ", candidate.lower()).strip()
    if not normalized:
        return False
    if normalized in used_wrong_options:
        return False
    if strict_similarity and is_similar_to_seen_distractor(normalized, used_wrong_options):
        return False
    selected.append(candidate)
    used_wrong_options.add(normalized)
    return True


def is_similar_to_seen_distractor(candidate: str, seen: set[str]) -> bool:
    candidate_tokens = token_set(candidate)
    if not candidate_tokens:
        return False
    for existing in seen:
        existing_tokens = token_set(existing)
        if not existing_tokens:
            continue
        overlap = len(candidate_tokens & existing_tokens)
        union = len(candidate_tokens | existing_tokens)
        if union == 0:
            continue
        jaccard = overlap / union
        if jaccard >= 0.75:
            return True
    return False


def token_set(text: str) -> set[str]:
    stop_words = {
        "de", "des", "du", "la", "le", "les", "et", "en", "un", "une", "sans", "avec",
        "pour", "sur", "dans", "au", "aux", "a", "par", "plus", "moins", "non"
    }
    return {token for token in text.split() if len(token) > 2 and token not in stop_words}


def text_similarity(text_a: str, text_b: str) -> float:
    tokens_a = token_set(text_a)
    tokens_b = token_set(text_b)
    if not tokens_a or not tokens_b:
        return 0.0
    intersection = len(tokens_a & tokens_b)
    union = len(tokens_a | tokens_b)
    if union == 0:
        return 0.0
    return intersection / union


def validate_java_blueprint_coverage(
    generated: dict[str, Any],
    expected_focus_areas: list[str],
    level: int | None,
) -> None:
    questions = generated.get("questions", [])
    if len(questions) < len(expected_focus_areas):
        raise QuizApiError("invalid-generator-output", "Not enough Java questions for expected coverage")

    for idx, area in enumerate(expected_focus_areas):
        question = questions[idx]
        joined = " ".join(
            [str(question.get("question", ""))]
            + [str(opt.get("text", "")) for opt in question.get("options", [])]
        ).lower()
        area_tokens = token_set(area.lower())
        if not area_tokens:
            continue
        if not (area_tokens & token_set(joined)):
            raise QuizApiError(
                "invalid-generator-output",
                f"Java question {idx + 1} does not match required topic '{area}'",
            )

    if level is not None:
        expected_domain = level_domain_for(level)
        joined_quiz = " ".join(str(q.get("question", "")) for q in questions).lower()
        if text_similarity(joined_quiz, expected_domain.lower()) < 0.05:
            raise QuizApiError("invalid-generator-output", "Java quiz difficulty domain mismatch")


def generate_java_fallback_payload(
    request: QuizGenerationRequest,
    title: str,
    focus_areas: list[str],
    rng: random.Random,
) -> dict[str, Any]:
    questions: list[dict[str, Any]] = []
    used_wrong_options: set[str] = set()
    for index in range(request.questionCount):
        area = focus_areas[index % len(focus_areas)]
        question_text, correct_text, wrong_texts = build_java_certification_item(
            area, request.level, index + 1, rng, used_wrong_options
        )
        options = [
            {"code": "A", "text": correct_text, "correct": True},
            {"code": "B", "text": wrong_texts[0], "correct": False},
            {"code": "C", "text": wrong_texts[1], "correct": False},
            {"code": "D", "text": wrong_texts[2], "correct": False},
        ]
        rng.shuffle(options)
        questions.append({"question": question_text, "options": options})
    return {"quizTitle": title, "questions": questions}


def build_java_certification_item(
    area: str,
    level: int,
    number: int,
    rng: random.Random,
    used_wrong_options: set[str],
) -> tuple[str, str, list[str]]:
    bank = build_java_item_bank(level)
    key = normalize_area_key(area)
    variants = bank.get(key, [])
    if variants:
        question, correct, wrongs = rng.choice(variants)
        selected: list[str] = []
        for wrong in wrongs:
            try_add_wrong_option(wrong, selected, used_wrong_options, strict_similarity=False)
        while len(selected) < 3:
            guaranteed = f"Option incorrecte mais plausible sur {area} ({level_domain_for(level)}) variante {len(selected) + 1}"
            try_add_wrong_option(guaranteed, selected, used_wrong_options, strict_similarity=False)
        return question, correct, selected[:3]

    question = f"En Java ({level_domain_for(level)}), quelle affirmation est la plus exacte concernant {area} ?"
    correct = f"Elle décrit correctement {area} avec la bonne nuance compilation/exécution."
    wrongs = [
        f"Elle confond le comportement à la compilation et à l'exécution pour {area}.",
        f"Elle n'est vraie que dans un cas limite mais est présentée comme générale pour {area}.",
        f"Elle ignore une contrainte technique qui change le résultat pour {area}.",
    ]
    return question, correct, wrongs


def normalize_area_key(area: str) -> str:
    normalized = area.lower().strip()
    normalized = normalized.replace("é", "e").replace("è", "e").replace("ê", "e").replace("à", "a")
    normalized = normalized.replace("(", " ").replace(")", " ").replace("/", " ").replace("'", " ")
    normalized = re.sub(r"\s+", " ", normalized)
    return normalized


def build_java_item_bank(level: int) -> dict[str, list[tuple[str, str, list[str]]]]:
    if level <= 1:
        return {
            "difference entre == et equals ": [
                (
                    "Quelle est la différence entre == et .equals() lors de la comparaison d'objets Java ?",
                    "== compare les références ; .equals() compare le contenu logique si la méthode est redéfinie.",
                    [
                        "Il n'y a aucune différence : les deux comparent toujours le contenu.",
                        "== compare le contenu et .equals() compare les adresses mémoire.",
                        ".equals() ne peut être utilisé qu'avec les chaînes String.",
                    ],
                )
            ],
            "encapsulation en java": [
                (
                    "Quelle affirmation décrit le mieux l'encapsulation en Java ?",
                    "Les données sont protégées via les modificateurs d'accès et exposées via des méthodes contrôlées.",
                    [
                        "Tous les attributs doivent être publics pour simplifier l'accès.",
                        "Elle remplace l'héritage en imposant une classe par package.",
                        "Elle sert surtout à améliorer le démarrage de la JVM.",
                    ],
                )
            ],
            "classe vs objet": [
                (
                    "Quelle est la relation correcte entre une classe et un objet en Java ?",
                    "Une classe est un modèle ; un objet est une instance créée à partir de ce modèle.",
                    [
                        "Une classe est une instance d'exécution ; un objet est sa définition source.",
                        "Les deux termes sont identiques sans différence sémantique.",
                        "Un objet peut exister sans aucune définition de classe.",
                    ],
                )
            ],
            "mot-cle static": [
                (
                    "À quoi sert le mot-clé static en Java ?",
                    "Il indique qu'un membre appartient à la classe et non à chaque instance.",
                    [
                        "Il rend les méthodes thread-safe par défaut.",
                        "Il force une méthode à être immuable à l'exécution.",
                        "Il ne peut être utilisé que sur les constructeurs.",
                    ],
                )
            ],
            "exception non capturee": [
                (
                    "Que se passe-t-il lorsqu'une exception n'est pas capturée en Java ?",
                    "Elle remonte la pile d'appels ; si elle reste non gérée, le thread se termine avec une stack trace.",
                    [
                        "La JVM l'ignore silencieusement et continue l'exécution.",
                        "Elle est automatiquement convertie en exception checked.",
                        "Le compilateur empêche toujours l'exécution du programme.",
                    ],
                )
            ],
        }
    if level == 2:
        return {
            "arraylist vs linkedlist": [
                (
                    "Quelle affirmation sur ArrayList et LinkedList est correcte ?",
                    "ArrayList est en général plus rapide pour l'accès indexé, tandis que LinkedList peut être plus adapté aux insertions/suppressions fréquentes au milieu.",
                    [
                        "LinkedList surpasse toujours ArrayList en accès aléatoire.",
                        "ArrayList stocke des nœuds chaînés et LinkedList un tableau dynamique.",
                        "Les deux ont la même structure mémoire et les mêmes complexités.",
                    ],
                )
            ],
            "interface vs classe abstraite": [
                (
                    "Quelle affirmation sur les interfaces Java est correcte ?",
                    "Une interface peut déclarer des méthodes abstraites et aussi fournir des méthodes default/static.",
                    [
                        "Une interface peut être instanciée directement avec new.",
                        "Une interface peut définir des méthodes d'instance protected.",
                        "Une interface ne peut contenir que des méthodes implémentées.",
                    ],
                )
            ],
            "fonctionnement hashmap": [
                (
                    "Comment HashMap retrouve-t-elle une valeur à partir d'une clé ?",
                    "Elle calcule le hash de la clé, cible un bucket, puis gère les collisions par vérification d'égalité de clé.",
                    [
                        "Elle parcourt toutes les entrées séquentiellement comme une liste.",
                        "Elle trie les clés avant chaque insertion pour garantir un accès en O(log n).",
                        "Elle utilise uniquement key.equals() sans hashCode().",
                    ],
                )
            ],
            "polymorphisme": [
                (
                    "Que permet le polymorphisme en Java ?",
                    "Une référence de type parent peut cibler des objets enfants et invoquer dynamiquement le comportement redéfini.",
                    [
                        "Il impose que toutes les surcharges retournent le même type.",
                        "Il autorise l'héritage multiple de classes.",
                        "Il résout tous les appels de méthodes uniquement à la compilation.",
                    ],
                )
            ],
            "checked vs unchecked exceptions": [
                (
                    "Quelle est la différence entre exceptions checked et unchecked ?",
                    "Les checked doivent être gérées ou déclarées ; les unchecked héritent de RuntimeException.",
                    [
                        "Les exceptions unchecked doivent toujours être déclarées dans la signature.",
                        "Les exceptions checked apparaissent uniquement à l'exécution.",
                        "Il n'existe aucune différence de comportement entre elles.",
                    ],
                )
            ],
        }
    if level == 3:
        return {
            "garbage collector": [
                (
                    "Quelle affirmation décrit le mieux le Garbage Collector Java ?",
                    "Le GC récupère les objets du heap qui ne sont plus fortement accessibles, souvent via une stratégie générationnelle.",
                    [
                        "Le GC libère immédiatement tout objet dès qu'une méthode se termine.",
                        "Les développeurs doivent piloter manuellement les cycles GC pour garantir la validité.",
                        "Le GC nettoie uniquement la pile (stack), pas le heap.",
                    ],
                )
            ],
            "synchronized vs lock vs volatile": [
                (
                    "Quelle différence clé existe entre synchronized, Lock et volatile ?",
                    "synchronized/Lock apportent l'exclusion mutuelle ; volatile garantit surtout visibilité/ordre sans atomicité des opérations composées.",
                    [
                        "volatile garantit l'exclusion mutuelle sur les sections critiques.",
                        "Lock est juste un alias syntaxique plus rapide de volatile.",
                        "synchronized fonctionne uniquement avec des champs static.",
                    ],
                )
            ],
            "cycle de creation d objet avec new": [
                (
                    "Que se passe-t-il lorsqu'un objet Java est créé avec new ?",
                    "La mémoire est allouée dans le heap, les champs sont initialisés, le constructeur s'exécute puis une référence est renvoyée.",
                    [
                        "L'objet est d'abord créé dans la stack puis déplacé dans le heap.",
                        "Aucune initialisation n'a lieu avant le corps du constructeur.",
                        "new crée par défaut une copie profonde d'un objet existant.",
                    ],
                )
            ],
            "prevention deadlock": [
                (
                    "Quelle stratégie aide à prévenir les deadlocks en Java multithread ?",
                    "Acquérir les verrous dans un ordre global cohérent et utiliser des tentatives temporisées lorsque pertinent.",
                    [
                        "Utiliser volatile sur toutes les variables partagées et supprimer les verrous.",
                        "Créer plus de threads réduit automatiquement la contention.",
                        "Encapsuler toutes les méthodes dans synchronized sans ordre de verrous.",
                    ],
                )
            ],
            "thread vs executorservice": [
                (
                    "Quel est l'avantage principal d'ExecutorService par rapport à la création manuelle de Thread ?",
                    "Il fournit un pool de threads, le pilotage du cycle de vie et une abstraction de soumission de tâches.",
                    [
                        "Il garantit l'absence totale de coût de changement de contexte.",
                        "Il exécute les tâches uniquement sur le thread principal.",
                        "Il supprime le besoin de gérer les exceptions dans les tâches.",
                    ],
                )
            ],
        }
    return {
        "spring dependency injection interne": [
            (
                "Comment fonctionne globalement l'injection de dépendances Spring en interne ?",
                "Le conteneur IoC crée les beans, résout les dépendances et les assemble selon la configuration/les annotations et le cycle de vie.",
                [
                    "Spring DI injecte les dépendances uniquement à la compilation via réflexion.",
                    "Les beans sont résolus uniquement par des singletons statiques sans cycle de vie.",
                    "La DI fonctionne seulement si tous les beans sont créés avec new dans les contrôleurs.",
                ],
            )
        ],
        "optimisation memoire java": [
            (
                "Un service Java consomme trop de mémoire. Quelle première investigation est la plus pertinente ?",
                "Capturer et analyser des heap dumps/profils pour identifier les allocations dominantes et les chemins de rétention.",
                [
                    "Augmenter immédiatement Xmx sans mesurer les hotspots d'allocation.",
                    "Désactiver les logs GC pour réduire le coût d'exécution avant analyse.",
                    "Remplacer toutes les collections par des tableaux sans preuve de profilage.",
                ],
            )
        ],
        "cap theorem en microservices": [
            (
                "En système distribué, qu'implique le théorème CAP lors d'une partition réseau ?",
                "Il faut arbitrer entre cohérence et disponibilité ; la tolérance au partitionnement est incontournable.",
                [
                    "On peut toujours garantir cohérence et disponibilité simultanément même en partition.",
                    "La tolérance au partitionnement est optionnelle en environnement distribué réel.",
                    "CAP ne s'applique qu'aux bases relationnelles, pas aux microservices.",
                ],
            )
        ],
        "architecture pour 1m messages jour": [
            (
                "Pour traiter environ 1M de messages/jour, quel choix d'architecture est le plus adapté ?",
                "Utiliser une messagerie asynchrone avec consommateurs partitionnés, handlers idempotents et scalabilité horizontale.",
                [
                    "Utiliser un unique endpoint synchrone avec une file en mémoire.",
                    "Stocker chaque message en fichier local et traiter séquentiellement par cron.",
                    "Désactiver les retries pour éviter la complexité des doublons.",
                ],
            )
        ],
        "investigation api spring boot lente": [
            (
                "Une API Spring Boot répond en 10s alors que la base répond en 100ms. Quel chemin d'investigation est le meilleur ?",
                "Corréler logs applicatifs, thread dumps, traces de profilage et métriques de latence des dépendances avant de modifier le code.",
                [
                    "Optimiser d'abord les index SQL car la base est toujours la cause principale.",
                    "Augmenter à l'aveugle la taille du thread pool jusqu'à baisse de latence.",
                    "Désactiver l'observabilité pour réduire le coût avant diagnostic.",
                ],
            )
        ],
    }
