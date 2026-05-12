import os
import pathlib
import sys
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.models import QuizGenerationRequest  # noqa: E402
from app.service import QuizApiError, generate_block, health_payload  # noqa: E402


class QuizServiceTest(unittest.TestCase):
    def setUp(self):
        os.environ["QUIZ_REFERENTIAL_DIR"] = str(ROOT.parent / "referentials")
        os.environ["QUIZ_ENABLE_LLM_GENERATION"] = "false"
        os.environ["QUIZ_ENABLE_LLM_VERIFIER"] = "false"
        os.environ.pop("OPENAI_API_KEY", None)

    def test_health_payload(self):
        self.assertEqual(health_payload(), {"status": "ok"})

    def test_generate_block_uses_fallback(self):
        block = generate_block(QuizGenerationRequest(skill="python", level=3, questionCount=5, instructions=None))
        self.assertEqual(block.generationSource, "fallback")
        self.assertEqual(len(block.questions), 5)
        self.assertEqual(len(block.expectedAnswers), 5)

    def test_generate_block_rejects_unknown_skill(self):
        with self.assertRaises(QuizApiError) as context:
            generate_block(QuizGenerationRequest(skill="go", level=3, questionCount=5, instructions=None))
        self.assertEqual(context.exception.code, "skill-unknown")


if __name__ == "__main__":
    unittest.main()
