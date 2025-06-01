from pydantic import BaseModel, Field
from typing import List

# ===============================
# 질문 생성 요청/응답 모델
# ===============================

class InterviewQuestionRequest(BaseModel):
    resume: str               # 사용자의 이력서 텍스트
    position: str             # 지원 직무명
    questionCount: int = Field(..., alias="question_count")  # 생성할 질문 개수 (Spring 연동에 맞춤)

    class Config:
        allow_population_by_field_name = True

class InterviewQuestionItem(BaseModel):
    content: str              # 생성된 질문 본문
    type: str                 # 질문 유형: TECHNICAL / PERSONALITY / PROJECT / SITUATION
    category: str             # 질문 카테고리 (예: Java, Teamwork 등)
    difficultyLevel: int      # 난이도 (1~3)

class InterviewQuestionResponse(BaseModel):
    questions: List[InterviewQuestionItem]


# ===============================
# 피드백 요청/응답 모델
# ===============================

class InterviewFeedbackRequest(BaseModel):
    question: str             # 질문 텍스트
    answer: str               # 사용자의 답변 텍스트
    position: str             # 지원 직무명 (컨텍스트 정보)

class InterviewFeedbackResponse(BaseModel):
    feedback: str             # 생성된 피드백 문자열

class SimulationRequest(BaseModel):
    resume: str
    cover_letter: str
    job_description: str
    temperature: float = 0.7
    num_questions: int = 3

class SimulationResponse(BaseModel):
    questions: List[str]
