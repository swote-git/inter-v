# schemas/evaluation_schema.py

from pydantic import BaseModel, Field

class EvaluationRequest(BaseModel):
    question: str = Field(..., description="면접 질문")
    answer: str = Field(..., description="지원자의 답변")
    resume: str = Field(..., description="이력서")
    cover_letter: str = Field(..., description="자기소개서")

class EvaluationResponse(BaseModel):
    관련성: int
    구체성: int
    실무성: int
    유효성: int
    총점: int
    피드백: str
