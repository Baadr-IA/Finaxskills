"""
High-quality quiz question generator following certification standards.
"""

from __future__ import annotations

import json
import os
import random
import socket
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any

from .models import QuizGenerationRequest, QuizGenerationResponse


@dataclass
class QuizApiError(Exception):
    code: str
    message: str
    details: dict[str, Any] | None = None


# ============================================================================
# CONCEPT CATALOGS BY LEVEL AND SUBJECT
# ============================================================================

JAVA_CONCEPTS = {
    1: [
        "== vs equals()", "variables et portée", "types primitifs",
        "opérateurs arithmétiques", "conditions if/else", "boucles for/while",
        "méthodes: signature et appel", "classes et objets", "constructeurs",
        "this", "access modifiers: public/private", "static", "final",
        "null et NullPointerException", "try/catch simples", "ArrayList basique",
        "boucle foreach", "String: immutabilité", "array: accès et longueur",
        "compareTo pour String",
    ],
    2: [
        "override vs overload", "héritage et super", "interfaces vs classes abstraites",
        "polymorphisme", "instanceof", "casting (cast et ClassCastException)",
        "Comparable et compareTo", "Comparator", "Collections.sort()",
        "HashMap: clés et valeurs", "TreeMap: ordre naturel", "HashSet: unicité",
        "LinkedList: performances", "generics: <T>", "wildcards: <? extends>, <? super>",
        "Optional", "Stream.map()", "Stream.filter()", "Stream.collect()",
        "lambda expressions", "functional interfaces", "checked vs unchecked exceptions",
        "try-with-resources", "Spring @Autowired", "Spring @Component", "JPA @Entity",
        "JPA @Id", "Comparable.equals() et hashCode()", "immutable objects", "enum",
    ],
    3: [
        "synchronized et race conditions", "volatility", "locks (ReentrantLock)",
        "CountDownLatch", "CyclicBarrier", "deadlock", "livelock",
        "ThreadPoolExecutor", "ExecutorService", "Future et CompletableFuture",
        "Thread lifecycle", "GC: young/old/perm generation", "GC: mark-sweep-compact",
        "memory model (happens-before)", "visibility", "atomicity", "ordering",
        "JVM tuning: Xmx, Xms", "heap dump analysis", "jstat et jmap",
        "reflection basics", "annotations processing", "design patterns: Singleton",
        "design patterns: Factory", "design patterns: Builder", "design patterns: Strategy",
        "design patterns: Observer", "database transactions: ACID", "Spring @Transactional",
        "Hibernate lazy loading", "N+1 query problem",
    ],
    4: [
        "JVM bytecode", "ClassLoader hierarchy", "class loading stages",
        "object layout in memory", "GC algorithms (G1, ZGC, Shenandoah)",
        "soft/weak/phantom references", "Virtual Threads (Project Loom)",
        "Unsafe API", "CAS (Compare And Swap)", "AtomicInteger/AtomicReference",
        "AQS (AbstractQueuedSynchronizer)", "memory barriers", "CPU cache invalidation",
        "false sharing", "JIT compilation", "escape analysis", "tiered compilation",
        "Java modules (JPMS)", "sealed classes", "records", "pattern matching",
        "foreign function & memory API", "microservices architecture",
        "distributed tracing", "eventual consistency", "circuit breaker pattern",
        "bulkhead pattern",
    ],
}

PYTHON_CONCEPTS = {
    1: [
        "variables et types", "int, float, str, bool", "listes: index et append",
        "dictionnaires: clés/valeurs", "boucles for", "boucles while",
        "conditions if/elif/else", "fonctions: def et return", "paramètres et arguments",
        "None", "strings: concatenation et f-strings", "len()", "range()",
        "list comprehension simple", "try/except simples", "imports", "modules",
        "slicing", "in et not in", "and/or/not",
    ],
    2: [
        "classes et __init__", "self", "methods", "héritage", "super()",
        "property decorator", "@staticmethod", "@classmethod", "dunder methods: __str__, __repr__",
        "dunder methods: __eq__, __lt__", "__len__", "__getitem__", "iterators: __iter__, __next__",
        "generators: yield", "decorators", "functools.wraps", "type hints", "dataclasses",
        "namedtuple", "set et frozenset", "defaultdict", "Counter", "lambda",
        "map, filter, reduce", "functools.reduce", "closures", "EAFP vs LBYL",
        "duck typing", "ABC (Abstract Base Class)", "context managers: with",
        "__enter__ et __exit__",
    ],
    3: [
        "asyncio", "async/await", "coroutines", "futures", "threading.Thread",
        "threading.Lock", "threading.RLock", "threading.Semaphore", "threading.Event",
        "threading.Barrier", "multiprocessing", "Process vs Thread", "GIL (Global Interpreter Lock)",
        "memory profiling", "CPU profiling", "cProfile", "timeit", "metaclasses",
        "descriptors", "protocol (typing.Protocol)", "type checking avec mypy", "slots",
        "__new__ vs __init__", "object.__new__", "weak references",
        "pickle et serialization", "copy vs deepcopy", "immutable objects",
        "functools.lru_cache",
    ],
    4: [
        "CPython internals", "PyObject structure", "reference counting", "cycle collector",
        "frame objects", "code objects", "bytecode instructions", "dis module",
        "sys.setrecursionlimit()", "optimization: string interning", "optimization: integer caching",
        "ctypes et FFI", "ctypes callbacks", "extension modules (C API)", "Cython", "PyPy",
        "JIT compilation", "peephole optimization", "PEG parser", "abstract syntax tree (AST)",
        "ast module", "compile() et eval()", "importlib", "import hooks", "sys.modules",
        "PYTHONPATH", "wheel et setuptools", "entry_points",
    ],
}

QUESTION_TYPES = [
    "code_analysis", "output_prediction", "compilation_check",
    "exception_prediction", "correct_implementation", "identify_bug",
    "refactoring", "performance_comparison", "best_practice", "completion",
]


# ============================================================================
# MAIN GENERATION
# ============================================================================

def generate_block(request: QuizGenerationRequest) -> QuizGenerationResponse:
    """Main entry point: generate a quiz block."""
    
    # Validate input
    if request.level < 1 or request.level > 4:
        raise QuizApiError("invalid-level", f"Level must be 1-4, got {request.level}")
    
    if request.questionCount < 1 or request.questionCount > 20:
        raise QuizApiError("invalid-request", f"Question count must be 1-20, got {request.questionCount}")
    
    skill = request.skill.strip().lower()
    if skill not in ["java", "python"]:
        raise QuizApiError("skill-unknown", f"Unsupported skill: {request.skill}")
    
    # Get concepts for this skill/level
    concept_catalog = JAVA_CONCEPTS if skill == "java" else PYTHON_CONCEPTS
    allowed_concepts = concept_catalog.get(request.level, [])
    
    if not allowed_concepts:
        raise QuizApiError("invalid-request", f"No concepts available for {skill} level {request.level}")
    
    # Try LLM first, fall back to synthetic
    generated = None
    fallback_reason = None
    
    if llm_generation_enabled():
        try:
            print(f"[DEBUG] Attempting LLM generation for {skill} level {request.level}")
            generated = generate_with_llm(
                skill,
                request.level,
                request.questionCount,
                allowed_concepts,
            )
            print(f"[DEBUG] LLM generation successful: {len(generated['questions'])} questions")
        except QuizApiError as e:
            fallback_reason = f"LLM failed: {e.message}"
            print(f"[DEBUG] LLM generation failed: {fallback_reason}")
        except Exception as e:
            fallback_reason = f"LLM error: {str(e)}"
            print(f"[DEBUG] LLM unexpected error: {fallback_reason}")
    
    if generated is None:
        print(f"[DEBUG] Using fallback generation for {skill} level {request.level}")
        generated = generate_fallback(
            skill,
            request.level,
            request.questionCount,
            allowed_concepts,
        )
    
    # Build response
    return build_response(generated, request.level, fallback_reason)


def llm_generation_enabled() -> bool:
    """Check if LLM generation is enabled."""
    enabled = os.getenv("QUIZ_ENABLE_LLM_GENERATION", "true").lower() == "true"
    has_key = bool(os.getenv("OPENAI_API_KEY"))
    return enabled and has_key


def generate_with_llm(
    subject: str,
    level: int,
    count: int,
    allowed_concepts: list[str],
) -> dict[str, Any]:
    """Generate questions via OpenAI LLM with a single batched call."""
    
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise QuizApiError("llm-error", "OPENAI_API_KEY not set")
    
    attempts = int(os.getenv("QUIZ_OPENAI_ATTEMPTS", "2"))
    
    for attempt in range(attempts):
        print(f"[DEBUG] LLM attempt {attempt + 1}/{attempts}")
        selected_concepts = random.choices(allowed_concepts, k=count)
        selected_types = random.choices(QUESTION_TYPES, k=count)

        system_prompt = get_system_prompt(subject, level, count)
        user_prompt = get_user_prompt(subject, level, selected_concepts, selected_types)

        try:
            print(f"[DEBUG] Generating {count} questions in one LLM call")
            response_text = call_openai(system_prompt, user_prompt)
            payload = json.loads(extract_json(response_text))
            raw_questions = payload["questions"] if isinstance(payload, dict) and "questions" in payload else payload
            if not isinstance(raw_questions, list):
                raise QuizApiError("invalid-question", "LLM payload must contain a questions array")
            if len(raw_questions) < count:
                raise QuizApiError("invalid-question", f"LLM returned only {len(raw_questions)}/{count} questions")

            questions_generated: list[dict[str, Any]] = []
            for q_idx, question_json in enumerate(raw_questions[:count], start=1):
                validate_question(question_json)
                questions_generated.append(question_json)
                print(f"[DEBUG] Question {q_idx} generated successfully")

            return {
                "quizTitle": f"{subject} - Niveau {level}",
                "questions": questions_generated,
                "source": "llm",
            }
        except json.JSONDecodeError as e:
            print(f"[DEBUG] JSON parse error in batch response: {str(e)}")
        except QuizApiError as e:
            print(f"[DEBUG] Validation error in batch response: {e.message}")
        except Exception as e:
            print(f"[DEBUG] Unexpected batch generation error: {str(e)}")

    raise QuizApiError(
        "llm-generation-failed",
        f"Failed to generate {count} valid questions after {attempts} attempts"
    )


def get_system_prompt(subject: str, level: int, count: int) -> str:
    """Generate system prompt for LLM."""
    level_desc = {
        1: "Débutant (syntaxe, variables, OOP basique)",
        2: "Intermédiaire (POO, collections, APIs)",
        3: "Avancé (concurrence, JVM, patterns)",
        4: "Expert (internals, GC, bytecode, architecture)",
    }.get(level, "Niveau inconnu")
    
    return f"""Tu es un expert en certification technique {subject} (style Oracle Java, Microsoft, AWS).

Génère EXACTEMENT {count} questions QCM de très haute qualité en JSON.

Sujet: {subject}
Niveau: {level_desc}
Langue: Français (termes techniques en anglais)

Exigences:
1. Chaque question doit tester un seul concept
2. Exactement 4 options (une correcte, trois plausibles)
3. Code compilable et réaliste si applicable
4. Réponses représentent des erreurs courantes
5. Pas de réponses évidentes, pas de blagues
6. Questions d'examen de certification

Format JSON OBLIGATOIRE (valide):
{{
  "questions": [
    {{
      "question": "...",
      "concept": "...",
      "questionType": "...",
      "level": {level},
      "subject": "{subject}",
      "code": null,
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "correctAnswer": 0,
      "explanation": "...",
      "tags": ["tag1"]
    }}
  ]
}}

Retourne UNIQUEMENT le JSON, pas de markdown."""


def get_user_prompt(subject: str, level: int, concepts: list[str], question_types: list[str]) -> str:
    """Generate user prompt for LLM."""
    assignments = [
        f"{idx + 1}. concept='{concept}', type='{question_types[idx]}'"
        for idx, concept in enumerate(concepts)
    ]
    return (
        f"Génère {len(concepts)} questions {subject} niveau {level} en respectant ce plan exact:\n"
        + "\n".join(assignments)
        + "\n\nRetourne UNIQUEMENT du JSON valide avec la clé top-level 'questions'."
    )


def call_openai(system_prompt: str, user_prompt: str) -> str:
    """Call OpenAI API with comprehensive error handling."""
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise QuizApiError("llm-error", "OPENAI_API_KEY not set")
    
    request_body = {
        "model": os.getenv("QUIZ_OPENAI_MODEL", "gpt-4o-mini"),
        "temperature": 0.8,
        "top_p": 0.95,
        "response_format": {"type": "json_object"},
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
    }
    
    endpoint = "https://api.openai.com/v1/chat/completions"
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
        timeout = int(os.getenv("QUIZ_OPENAI_TIMEOUT_SECONDS", "60"))
        print(f"[DEBUG] OpenAI call with timeout {timeout}s")
        with urllib.request.urlopen(http_request, timeout=timeout) as response:
            payload = json.loads(response.read().decode("utf-8"))
            return payload["choices"][0]["message"]["content"]
    except (urllib.error.HTTPError, socket.timeout, TimeoutError) as e:
        raise QuizApiError("llm-error", f"OpenAI request failed (timeout/HTTP): {str(e)}")
    except (KeyError, IndexError, TypeError) as e:
        raise QuizApiError("llm-error", f"Invalid OpenAI response structure: {str(e)}")
    except Exception as e:
        raise QuizApiError("llm-error", f"Unexpected error: {type(e).__name__}: {str(e)}")


def extract_json(text: str) -> str:
    """Extract JSON from LLM response."""
    text = text.strip()
    if text.startswith("```"):
        # Remove markdown code blocks
        text = text.split("```")[1]
        if text.startswith("json"):
            text = text[4:]
        text = text.strip()
    return text


def validate_question(q: dict[str, Any]) -> None:
    """Validate generated question."""
    required = ["question", "concept", "questionType", "level", "subject", "options", "correctAnswer", "explanation"]
    for field in required:
        if field not in q:
            raise QuizApiError("invalid-question", f"Missing field: {field}")
    
    options = q.get("options", [])
    if not isinstance(options, list) or len(options) != 4:
        raise QuizApiError("invalid-question", "Must have exactly 4 options")
    
    correct_idx = q.get("correctAnswer")
    if not isinstance(correct_idx, int) or correct_idx < 0 or correct_idx > 3:
        raise QuizApiError("invalid-question", "correctAnswer must be 0-3")
    
    for i, opt in enumerate(options):
        if not isinstance(opt, str) or len(opt.strip()) < 3:
            raise QuizApiError("invalid-question", f"Option {i} too short (min 3 chars)")
    
    if len(str(q.get("question", "")).strip()) < 15:
        raise QuizApiError("invalid-question", "Question text too short (min 15 chars)")


def generate_fallback(
    subject: str,
    level: int,
    count: int,
    allowed_concepts: list[str],
) -> dict[str, Any]:
    """Fallback: synthetic questions with realistic content."""
    questions = []
    
    # Pre-built fallback questions per subject/level
    fallback_templates = get_fallback_templates(subject, level)
    
    for i in range(count):
        template = random.choice(fallback_templates)
        concept = random.choice(allowed_concepts)
        
        # Customize template with concept
        q = {
            "question": template["question"].replace("{concept}", concept),
            "concept": concept,
            "questionType": template["questionType"],
            "level": level,
            "subject": subject,
            "code": template.get("code"),
            "options": [
                template["options"][0].replace("{concept}", concept),
                template["options"][1].replace("{concept}", concept),
                template["options"][2].replace("{concept}", concept),
                template["options"][3].replace("{concept}", concept),
            ],
            "correctAnswer": template["correctAnswer"],
            "explanation": template["explanation"].replace("{concept}", concept),
            "tags": [concept],
        }
        questions.append(q)
    
    return {
        "quizTitle": f"{subject} - Niveau {level}",
        "questions": questions,
        "source": "fallback",
    }


def get_fallback_templates(subject: str, level: int) -> list[dict]:
    """Get pre-built fallback question templates."""
    
    if subject.lower() == "java":
        if level == 1:
            return [
                {
                    "question": "Quel est le résultat de comparaison entre deux objets String avec '{concept}'?",
                    "questionType": "output_prediction",
                    "code": None,
                    "options": [
                        "Elles comparent l'adresse mémoire",
                        "Elles comparent le contenu",
                        "Une exception est levée",
                        "Le résultat dépend de la JVM"
                    ],
                    "correctAnswer": 0,
                    "explanation": "{concept} teste les références mémoire, pas le contenu."
                },
                {
                    "question": "En Java, quelle est la portée d'une variable locale déclarée dans une méthode selon '{concept}'?",
                    "questionType": "code_analysis",
                    "code": None,
                    "options": [
                        "Accessible dans toute la classe",
                        "Accessible uniquement dans la méthode",
                        "Accessible dans le package",
                        "Accessible partout après sa déclaration"
                    ],
                    "correctAnswer": 1,
                    "explanation": "{concept} s'applique: les variables locales ne sont accessibles que dans leur portée."
                },
                {
                    "question": "Quel mot-clé Java rend un attribut partagé entre toutes les instances de la classe?",
                    "questionType": "best_practice",
                    "code": None,
                    "options": [
                        "final",
                        "private",
                        "static",
                        "synchronized"
                    ],
                    "correctAnswer": 2,
                    "explanation": "Le mot-clé 'static' rend une variable de classe, applicable au concept '{concept}'."
                },
            ]
        elif level == 2:
            return [
                {
                    "question": "Quelle interface Java ordonne naturellement les éléments selon '{concept}'?",
                    "questionType": "best_practice",
                    "code": None,
                    "options": [
                        "Comparator",
                        "Iterator",
                        "Comparable",
                        "Collection"
                    ],
                    "correctAnswer": 2,
                    "explanation": "Comparable fournit un ordre naturel; Comparator fournit des ordres personnalisés pour '{concept}'."
                },
                {
                    "question": "En Java, quelle exception est levée lors du déréférencement d'une variable null?",
                    "questionType": "exception_prediction",
                    "code": None,
                    "options": [
                        "NullAccessException",
                        "NullPointerException",
                        "InvalidReferenceException",
                        "NullValueException"
                    ],
                    "correctAnswer": 1,
                    "explanation": "NullPointerException est levée pour '{concept}' lors du déréférencement de null."
                },
            ]
        elif level == 3:
            return [
                {
                    "question": "Comment éviter une condition de course (race condition) avec '{concept}' en Java?",
                    "questionType": "best_practice",
                    "code": None,
                    "options": [
                        "Utiliser volatile uniquement",
                        "Utiliser synchronized ou Lock",
                        "Augmenter la priorité du thread",
                        "Rien, c'est impossible"
                    ],
                    "correctAnswer": 1,
                    "explanation": "synchronized et Lock assurent l'exclusion mutuelle pour '{concept}'."
                },
                {
                    "question": "Le Garbage Collector en Java récupère la mémoire en utilisant quel algorithme par défaut?",
                    "questionType": "code_analysis",
                    "code": None,
                    "options": [
                        "Reference counting",
                        "Mark-sweep-compact",
                        "Copying collector",
                        "Stop-the-world uniquement"
                    ],
                    "correctAnswer": 1,
                    "explanation": "Mark-sweep-compact est l'algorithme principal pour '{concept}' en Java."
                },
            ]
        else:  # level 4
            return [
                {
                    "question": "Quel composant JVM charge les classes selon '{concept}'?",
                    "questionType": "code_analysis",
                    "code": None,
                    "options": [
                        "Bytecode verifier",
                        "ClassLoader",
                        "JIT compiler",
                        "Memory allocator"
                    ],
                    "correctAnswer": 1,
                    "explanation": "Le ClassLoader gère le chargement des classes pour '{concept}' en Java."
                },
                {
                    "question": "Comment fonctionne CAS (Compare-And-Swap) pour '{concept}' en Java?",
                    "questionType": "performance_comparison",
                    "code": None,
                    "options": [
                        "Échange atomiquement si la valeur n'a pas changé",
                        "Vérifie puis échange (deux opérations)",
                        "Bloque le thread",
                        "Utilise un lock interne"
                    ],
                    "correctAnswer": 0,
                    "explanation": "CAS compare atomiquement et échange sans lock pour '{concept}'."
                },
            ]
    
    # Python fallbacks
    else:
        if level == 1:
            return [
                {
                    "question": "Quel type de données Python représente '{concept}'?",
                    "questionType": "best_practice",
                    "code": None,
                    "options": [
                        "entier (int)",
                        "chaîne (str)",
                        "liste (list)",
                        "dictionnaire (dict)"
                    ],
                    "correctAnswer": random.randint(0, 3),
                    "explanation": "Python classifie les types de données incluant '{concept}'."
                },
            ]
        else:
            return [
                {
                    "question": "En Python, quel décorateur rend une méthode indépendante des instances pour '{concept}'?",
                    "questionType": "code_analysis",
                    "code": None,
                    "options": [
                        "@property",
                        "@staticmethod",
                        "@classmethod",
                        "@decorator"
                    ],
                    "correctAnswer": 1,
                    "explanation": "@staticmethod rend une méthode statique pour '{concept}' en Python."
                },
            ]
    
    return [
        {
            "question": f"Quels énoncés concernant '{concept}' sont vrais?",
            "questionType": "best_practice",
            "code": None,
            "options": [
                f"Option 1 sur {concept}",
                f"Option 2 sur {concept}",
                f"Option 3 sur {concept}",
                f"Option 4 sur {concept}"
            ],
            "correctAnswer": 0,
            "explanation": f"Explication sur {concept}."
        }
    ]


def build_response(
    generated: dict[str, Any],
    level: int,
    fallback_reason: str | None,
) -> QuizGenerationResponse:
    """Transform internal format to QuizGenerationResponse."""
    from .models import GeneratedQuestion, ExpectedAnswer, Difficulty
    
    questions = []
    expected_answers = []
    
    for idx, q in enumerate(generated.get("questions", []), start=1):
        q_id = f"q{idx}"
        options = q.get("options", [])
        correct_idx = q.get("correctAnswer", 0)
        option_codes = ["A", "B", "C", "D"]
        correct_code = option_codes[correct_idx] if correct_idx < len(option_codes) else "A"
        
        generated_q = GeneratedQuestion(
            id=q_id,
            topic=q.get("concept", "Général"),
            targetedOutcome=f"Test {q.get('concept')} niveau {level}",
            text=q.get("question", ""),
            code=q.get("code"),
            optionA=options[0] if len(options) > 0 else "",
            optionB=options[1] if len(options) > 1 else "",
            optionC=options[2] if len(options) > 2 else "",
            optionD=options[3] if len(options) > 3 else "",
            explanation=q.get("explanation", ""),
        )
        questions.append(generated_q)
        expected_answers.append(ExpectedAnswer(questionId=q_id, option=correct_code))
    
    level_desc = {
        1: "Débutant",
        2: "Intermédiaire",
        3: "Confirmé",
        4: "Expert",
    }.get(level, "Inconnu")
    
    return QuizGenerationResponse(
        title=generated.get("quizTitle", "Quiz"),
        questions=questions,
        expectedAnswers=expected_answers,
        difficulty=Difficulty(
            level=level,
            label=f"Niveau {level} ({level_desc})",
            description=f"Questions de certification {level_desc}",
        ),
        durationMinutes=max(10, len(questions) * 3),
        evaluationCriteria=[
            "Une seule réponse correcte par question",
            "Difficulté alignée au niveau demandé",
            "Questions testent des concepts uniques",
            "Réponses techniquement plausibles",
        ],
        generationSource=generated.get("source", "fallback"),
        fallbackReason=fallback_reason,
    )


def health_payload() -> dict[str, str]:
    """Health check."""
    return {"status": "ok"}
