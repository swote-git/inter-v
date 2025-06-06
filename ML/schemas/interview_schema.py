from pydantic import BaseModel, Field, validator
from typing import List, Optional
from enum import Enum

class QuestionType(str, Enum):
    TECHNICAL = "TECHNICAL"
    PERSONALITY = "PERSONALITY"
    PROJECT = "PROJECT"
    SITUATION = "SITUATION"

class InterviewQuestionRequest(BaseModel):
    resume: str = Field(..., description="사용자의 이력서 텍스트", min_length=10)
    position: str = Field(..., description="지원 직무명", min_length=1)
    question_count: int = Field(..., description="생성할 질문 개수", ge=1, le=20)

    @validator('resume')
    def resume_must_not_be_empty(cls, v):
        if not v.strip():
            raise ValueError('이력서 내용이 비어있습니다.')
        return v.strip()

    @validator('position')
    def position_must_not_be_empty(cls, v):
        if not v.strip():
            raise ValueError('지원 직무가 비어있습니다.')
        return v.strip()

    class Config:
        json_schema_extra = {
            "example": {
                "resume": "Java, Spring Boot 경험이 있는 백엔드 개발자입니다...",
                "position": "백엔드 개발자",
                "question_count": 5
            }
        }

class InterviewQuestionItem(BaseModel):
    content: str = Field(..., description="생성된 질문 본문")
    type: QuestionType = Field(..., description="질문 유형")
    category: str = Field(..., description="질문 카테고리")
    difficultyLevel: int = Field(..., description="난이도 (1~3)", ge=1, le=3)

    class Config:
        json_schema_extra = {
            "example": {
                "content": "Spring Boot와 JPA를 활용한 프로젝트 경험에 대해 설명해주세요.",
                "type": "TECHNICAL",
                "category": "Spring",
                "difficultyLevel": 2
            }
        }

class InterviewQuestionResponse(BaseModel):
    questions: List[InterviewQuestionItem] = Field(..., description="생성된 질문 목록")
    
    class Config:
        json_schema_extra = {
            "example": {
                "questions": [
                    {
                        "content": "Spring Boot와 JPA를 활용한 프로젝트 경험에 대해 설명해주세요.",
                        "type": "TECHNICAL",
                        "category": "Spring",
                        "difficultyLevel": 2
                    }
                ]
            }
        }