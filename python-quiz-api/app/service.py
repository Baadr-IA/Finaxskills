from __future__ import annotations

import json
import os
import random
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any

from .models import QuizGenerationRequest, QuizGenerationResponse


VALID_OPTIONS = {"A", "B", "C", "D"}

GENERATION_PROMPT = """
You are a senior technical quiz generator.
Generate a brand-new multiple-choice quiz every time.
Do not reuse previous questions, and do not rely on any static referential.
Return JSON only with this schema:
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
Rules:
- exactly one correct option per question
- exactly 4 options A/B/C/D
- generate exactly the requested question count
- keep difficulty aligned to requested level (1 beginner -> 5 expert)
- practical/professional wording, no trivia
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
    nonce = f"{uuid.uuid4()}-{datetime.now(timezone.utc).isoformat()}"
    user_prompt = json.dumps(
        {
            "skill": request.skill,
            "level": request.level,
            "questionCount": request.questionCount,
            "instructions": request.instructions,
            "nonce": nonce,
            "strictUniquenessInstruction": "Questions must be different from prior generations for same skill/level.",
        },
        ensure_ascii=False,
    )
    response_text = call_openai_chat_completion(GENERATION_PROMPT, user_prompt)
    try:
        return json.loads(extract_json(response_text))
    except json.JSONDecodeError as error:
        raise QuizApiError("invalid-generator-output", "The LLM returned malformed JSON") from error


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


def build_response(generated: dict[str, Any], level: int, fallback_reason: str | None) -> QuizGenerationResponse:
    raw_questions = generated.get("questions", [])
    questions = []
    expected_answers = []

    for index, question in enumerate(raw_questions, start=1):
        options = {str(opt["code"]).upper(): opt["text"] for opt in question["options"]}
        correct_option = next(str(opt["code"]).upper() for opt in question["options"] if opt.get("correct") is True)
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
    title = f"{humanize_skill_name(request.skill)} - Niveau {request.level}"
    templates = build_templates(request.skill, request.level)
    questions: list[dict[str, Any]] = []

    for index in range(request.questionCount):
        template = rng.choice(templates)
        question_text, correct_text, wrong_texts = template(rng, index + 1)
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


def build_templates(skill: str, level: int):
    skill_label = humanize_skill_name(skill)
    level_label = f"Niveau {level}"

    def template_concept(rng: random.Random, number: int):
        concepts = [
            "architecture", "bonne pratique", "sécurité", "performance",
            "maintenance", "tests", "déploiement", "diagnostic"
        ]
        concept = rng.choice(concepts)
        question = f"Question {number} : quelle approche est la plus adaptée pour {concept} en {skill_label} ?"
        correct = f"Adopter une solution {concept} cohérente avec le contexte {level_label}"
        wrong = [
            f"Ignorer totalement les contraintes de {concept}",
            f"Appliquer une réponse générique sans analyse",
            f"Choisir une option contraire aux besoins métier",
        ]
        return question, correct, wrong

    def template_scenario(rng: random.Random, number: int):
        scenario = rng.choice([
            "une application critique",
            "un service exposé sur Internet",
            "une mise en production",
            "un problème de qualité",
        ])
        question = f"Question {number} : dans {scenario}, quel réflexe est le plus pertinent en {skill_label} ?"
        correct = f"Vérifier les prérequis et valider la solution au {level_label}"
        wrong = [
            "Déployer sans contrôle",
            "Contourner les validations",
            "Choisir la première option venue",
        ]
        return question, correct, wrong

    def template_definition(rng: random.Random, number: int):
        topic = rng.choice([
            "un concept clé", "une règle métier", "un point d'architecture", "un mécanisme de sécurité"
        ])
        question = f"Question {number} : comment expliquer {topic} en {skill_label} ?"
        correct = f"En donnant une explication précise et adaptée au {level_label}"
        wrong = [
            "En restant volontairement vague",
            "En mélangeant plusieurs notions incompatibles",
            "En évitant tout exemple concret",
        ]
        return question, correct, wrong

    return [template_concept, template_scenario, template_definition]
