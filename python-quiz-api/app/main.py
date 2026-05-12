from fastapi import FastAPI
from fastapi.responses import JSONResponse

from .models import ErrorEnvelope, HealthResponse, QuizGenerationRequest, QuizGenerationResponse
from .service import QuizApiError, generate_block, health_payload


app = FastAPI(title="Finaxskills Quiz API", version="0.1.0")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse.model_validate(health_payload())


@app.post("/api/v1/quiz-blocks/generate", response_model=QuizGenerationResponse)
def generate_quiz_block(request: QuizGenerationRequest) -> QuizGenerationResponse:
    return generate_block(request)


@app.exception_handler(QuizApiError)
def handle_quiz_api_error(_, exception: QuizApiError) -> JSONResponse:
    status_code = 422 if exception.code in {"skill-unknown", "invalid-level", "invalid-request"} else 502
    payload = ErrorEnvelope.model_validate(
        {
            "error": {
                "code": exception.code,
                "message": exception.message,
                "details": exception.details or {},
            }
        }
    )
    return JSONResponse(status_code=status_code, content=payload.model_dump())
