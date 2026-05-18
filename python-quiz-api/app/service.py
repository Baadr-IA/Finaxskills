from __future__ import annotations

import json
import os
import re
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .models import QuizGenerationRequest, QuizGenerationResponse


VALID_OPTIONS = {"A", "B", "C", "D"}
GENERATION_PROMPT_PREFIX = """
You are a senior technical assessment designer specialized in building fair, level-based multiple-choice quizzes for employee technical evaluation.

Your task is to generate a quiz block for the requested skill and level.

You must use the provided referential as the single source of truth:
- use the skill description
- use the target level description
- use the listed topics
- use the expected outcomes ("attendus")
- align the real difficulty with the requested level only
- do not generate questions outside the referential scope

Question design rules:
- generate exactly the requested number of questions
- each question must assess one concrete expected outcome from the referential
- each question must have exactly 4 options: A, B, C, D
- exactly one option must be correct
- incorrect options must be plausible and technically credible
- avoid ambiguous wording
- avoid trivia and overly academic questions
- prioritize professional, practical understanding
- explanations must clearly justify why the correct answer is correct

Difficulty rules:
- do not mix beginner and expert expectations in the same block
- keep the vocabulary, traps, and reasoning depth aligned with the requested level
- if the level is intermediate, questions must require understanding, not simple memorization

Output rules:
- return valid JSON only
- no markdown
- no comments
- no prose outside JSON
- strictly follow the required schema

Context payload:
{
  "skill": "...",
  "requestedLevel": ...,
  "questionCount": ...,
  "optionalInstructions": "...",
  "difficulty": { ... },
  "topics": [...],
  "generationRules": {...},
  "fewShotExamples": [...],
  "outputSchema": {...}
}
""".strip()


@dataclass
class QuizApiError(Exception):
    code: str
    message: str
    details: dict[str, Any] | None = None


def generate_block(request: QuizGenerationRequest) -> QuizGenerationResponse:
    referential = load_referential(normalize_skill_key(request.skill))
    level_definition = get_level_definition(referential, request.level)
    fallback_reason = None
    generation_source = "fallback"

    if llm_generation_enabled():
        try:
            generated = generate_with_llm(request, referential, level_definition)
            validate_generated_payload(generated, request.questionCount)
            verify_generated_payload(generated, referential, request.level)
            generation_source = "llm"
            return build_response(generated, level_definition, generation_source, fallback_reason)
        except QuizApiError as error:
            fallback_reason = error.message

    generated = build_fallback_payload(referential, request.level, request.questionCount)
    return build_response(generated, level_definition, generation_source, fallback_reason)


def health_payload() -> dict[str, str]:
    return {"status": "ok"}


def load_referential(skill_key: str) -> dict[str, Any]:
    referential_dir = Path(os.getenv("QUIZ_REFERENTIAL_DIR", "referentials"))
    referential_path = referential_dir / f"{skill_key}_referential.json"
    if not referential_path.exists():
        raise QuizApiError("skill-unknown", f"No referential exists for skill '{skill_key}'")

    with referential_path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def normalize_skill_key(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")


def get_level_definition(referential: dict[str, Any], level: int) -> dict[str, Any]:
    for level_definition in referential.get("levels", []):
        if int(level_definition.get("level", 0)) == level:
            return level_definition
    raise QuizApiError("invalid-level", f"Level '{level}' is not available in the referential")


def llm_generation_enabled() -> bool:
    return (
        os.getenv("QUIZ_ENABLE_LLM_GENERATION", "true").lower() == "true"
        and bool(os.getenv("OPENAI_API_KEY"))
    )


def llm_verifier_enabled() -> bool:
    return (
        os.getenv("QUIZ_ENABLE_LLM_VERIFIER", "true").lower() == "true"
        and bool(os.getenv("OPENAI_API_KEY"))
    )


def generate_with_llm(
    request: QuizGenerationRequest,
    referential: dict[str, Any],
    level_definition: dict[str, Any],
) -> dict[str, Any]:
    system_prompt = GENERATION_PROMPT_PREFIX
    user_prompt = build_generation_prompt(request, referential, level_definition)
    response_text = call_openai_chat_completion(system_prompt, user_prompt)

    try:
        return json.loads(extract_json(response_text))
    except json.JSONDecodeError as error:
        raise QuizApiError("invalid-generator-output", "The LLM returned malformed JSON") from error


def build_generation_prompt(
    request: QuizGenerationRequest,
    referential: dict[str, Any],
    level_definition: dict[str, Any],
) -> str:
    topics = level_definition.get("topics", [])
    few_shot_examples = []
    for topic in topics[: min(3, len(topics))]:
        sample = topic.get("exemple_question", {})
        few_shot_examples.append(
            {
                "topic": topic.get("name"),
                "targetedOutcome": first_expected_outcome(topic),
                "text": sample.get("enonce"),
                "options": {
                    "A": sample.get("choix_A"),
                    "B": sample.get("choix_B"),
                    "C": sample.get("choix_C"),
                    "D": sample.get("choix_D"),
                },
                "correctOption": sample.get("bonne_reponse"),
                "explanation": sample.get("explication"),
            }
        )

    prompt_payload = {
        "skill": referential.get("skill"),
        "requestedLevel": request.level,
        "questionCount": request.questionCount,
        "optionalInstructions": request.instructions,
        "difficulty": {
            "label": level_definition.get("label"),
            "description": level_definition.get("description"),
        },
        "topics": [
            {
                "name": topic.get("name"),
                "expectedOutcomes": topic.get("attendus", []),
            }
            for topic in topics
        ],
        "generationRules": referential.get("test_generation_instructions", {}),
        "fewShotExamples": few_shot_examples,
        "outputSchema": {
            "title": "string",
            "questions": [
                {
                    "id": "string",
                    "topic": "string",
                    "targetedOutcome": "string",
                    "text": "string",
                    "optionA": "string",
                    "optionB": "string",
                    "optionC": "string",
                    "optionD": "string",
                    "correctOption": "A|B|C|D",
                    "explanation": "string",
                }
            ],
        },
    }

    return json.dumps(prompt_payload, ensure_ascii=False)


def verify_generated_payload(generated: dict[str, Any], referential: dict[str, Any], level: int) -> None:
    if not llm_verifier_enabled():
        return

    system_prompt = (
        "You validate generated technical multiple-choice questions. "
        "Return JSON only with fields approved:boolean and reason:string."
    )
    user_prompt = json.dumps(
        {
            "skill": referential.get("skill"),
            "level": level,
            "questions": generated.get("questions", []),
        },
        ensure_ascii=False,
    )
    response_text = call_openai_chat_completion(system_prompt, user_prompt)

    try:
        verdict = json.loads(extract_json(response_text))
    except json.JSONDecodeError as error:
        raise QuizApiError("coherence-check-failed", "The coherence verifier returned malformed JSON") from error

    if verdict.get("approved") is not True:
        raise QuizApiError(
            "coherence-check-failed",
            str(verdict.get("reason", "The generated quiz block was rejected by the coherence verifier")),
        )


def call_openai_chat_completion(system_prompt: str, user_prompt: str) -> str:
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise QuizApiError("llm-error", "OPENAI_API_KEY is required for LLM generation")

    request_body = {
        "model": os.getenv("QUIZ_OPENAI_MODEL", "gpt-4.1-mini"),
        "temperature": 0.2,
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
        timeout = float(os.getenv("QUIZ_OPENAI_TIMEOUT_SECONDS", "8"))
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
        required_fields = {
            "id",
            "topic",
            "targetedOutcome",
            "text",
            "optionA",
            "optionB",
            "optionC",
            "optionD",
            "correctOption",
            "explanation",
        }
        if not required_fields.issubset(question):
            raise QuizApiError("invalid-generator-output", "A generated question is missing required fields")
        if str(question["correctOption"]).upper() not in VALID_OPTIONS:
            raise QuizApiError("invalid-generator-output", "A generated question contains an invalid correct option")


def build_fallback_payload(referential: dict[str, Any], level: int, question_count: int) -> dict[str, Any]:
    selected_questions: list[dict[str, Any]] = []
    seen_texts: set[str] = set()
    candidate_levels = [level]
    for distance in range(1, 5):
        if level - distance >= 1:
            candidate_levels.append(level - distance)
        if level + distance <= 5:
            candidate_levels.append(level + distance)

    index = 1
    for candidate_level in candidate_levels:
        try:
            level_definition = get_level_definition(referential, candidate_level)
        except QuizApiError:
            continue

        for topic in level_definition.get("topics", []):
            sample = topic.get("exemple_question") or {}
            text = sample.get("enonce")
            if not text or text in seen_texts:
                continue

            selected_questions.append(
                {
                    "id": f"q{index}",
                    "topic": topic.get("name"),
                    "targetedOutcome": first_expected_outcome(topic),
                    "text": text,
                    "optionA": sample.get("choix_A"),
                    "optionB": sample.get("choix_B"),
                    "optionC": sample.get("choix_C"),
                    "optionD": sample.get("choix_D"),
                    "correctOption": sample.get("bonne_reponse"),
                    "explanation": sample.get("explication"),
                }
            )
            seen_texts.add(text)
            index += 1
            if len(selected_questions) == question_count:
                return {
                    "title": f"{referential.get('skill')} - Niveau {level}",
                    "questions": selected_questions,
                }

    raise QuizApiError("invalid-generator-output", "The referential does not contain enough fallback questions")


def first_expected_outcome(topic: dict[str, Any]) -> str:
    outcomes = topic.get("attendus", [])
    return outcomes[0] if outcomes else str(topic.get("name", "Expected outcome"))


def build_response(
    generated: dict[str, Any],
    level_definition: dict[str, Any],
    generation_source: str,
    fallback_reason: str | None,
) -> QuizGenerationResponse:
    questions = []
    expected_answers = []
    for question in generated.get("questions", []):
        correct_option = str(question["correctOption"]).upper()
        expected_answers.append({"questionId": question["id"], "option": correct_option})
        questions.append(
            {
                "id": question["id"],
                "topic": question["topic"],
                "targetedOutcome": question["targetedOutcome"],
                "text": question["text"],
                "optionA": question["optionA"],
                "optionB": question["optionB"],
                "optionC": question["optionC"],
                "optionD": question["optionD"],
                "explanation": question["explanation"],
            }
        )

    return QuizGenerationResponse.model_validate(
        {
            "title": generated.get("title") or f"Quiz niveau {level_definition.get('level')}",
            "questions": questions,
            "expectedAnswers": expected_answers,
            "difficulty": {
                "level": level_definition.get("level"),
                "label": level_definition.get("label"),
                "description": level_definition.get("description"),
            },
            "durationMinutes": max(10, len(questions) * 3),
            "evaluationCriteria": [
                "Verify the expected outcomes of the target level",
                "Reward technical accuracy rather than guesswork",
                "Keep the difficulty aligned with the requested level",
            ],
            "generationSource": generation_source,
            "fallbackReason": fallback_reason,
        }
    )
