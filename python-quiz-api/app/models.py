from pydantic import BaseModel, ConfigDict, Field


class QuizGenerationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    skill: str = Field(min_length=1)
    level: int = Field(ge=1, le=5)
    questionCount: int = Field(ge=1, le=20)
    instructions: str | None = Field(default=None, max_length=1000)


class GeneratedQuestion(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: str
    topic: str
    targetedOutcome: str
    text: str
    optionA: str
    optionB: str
    optionC: str
    optionD: str
    explanation: str


class ExpectedAnswer(BaseModel):
    model_config = ConfigDict(extra="forbid")

    questionId: str
    option: str = Field(pattern="^[ABCD]$")


class Difficulty(BaseModel):
    model_config = ConfigDict(extra="forbid")

    level: int
    label: str
    description: str


class QuizGenerationResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    title: str
    questions: list[GeneratedQuestion]
    expectedAnswers: list[ExpectedAnswer]
    difficulty: Difficulty
    durationMinutes: int
    evaluationCriteria: list[str]
    generationSource: str
    fallbackReason: str | None = None


class ErrorBody(BaseModel):
    model_config = ConfigDict(extra="forbid")

    code: str
    message: str
    details: dict[str, object] = Field(default_factory=dict)


class ErrorEnvelope(BaseModel):
    model_config = ConfigDict(extra="forbid")

    error: ErrorBody


class HealthResponse(BaseModel):
    status: str
