from pydantic import BaseModel, Field
from typing import List

class InterviewRequest(BaseModel):
    resume: str = Field(..., description="사용자의 이력서 내용")
    cover_letter: str = Field(..., description="사용자의 자기소개서 내용")
    job_description: str = Field(..., description="지원하는 회사의 채용 공고 내용")
    num_questions: int = Field(3, description="생성할 질문 개수")

    class Config:
        schema_extra = {
            "example": {
                "resume": "저는 백엔드 개발자로서 Spring과 MySQL 기반의 쇼핑몰 프로젝트를 수행했습니다. JWT 인증과 Redis를 활용한 토큰 관리 경험도 있습니다.",
                "cover_letter": "문제 해결에 집중하는 성격이며 팀 프로젝트 경험이 풍부합니다. 책임감 있는 자세로 개발에 임하고 있습니다.",
                "job_description": "Spring 기반 백엔드 서버 개발자 모집. REST API 개발, MySQL 사용 경험 필수. 협업 역량 중요.",
                "num_questions": 3
            }
        }

class InterviewResponse(BaseModel):
    questions: List[str]

class SimulationRequest(BaseModel):
    resume: str = Field(..., description="이력서")
    cover_letter: str = Field(..., description="자기소개서")
    job_description: str = Field(..., description="채용 공고")
    user_answer: str = Field(..., description="사용자의 답변")
    num_questions: int = Field(3, description="생성할 질문 수")

class SimulationResponse(BaseModel):
    generated_questions: List[str]
    selected_question: str
    user_answer: str
    evaluation_result: dict
