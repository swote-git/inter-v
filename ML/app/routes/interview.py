from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import JSONResponse
from schemas.interview_schema import (
    InterviewQuestionRequest,
    InterviewQuestionResponse,
    InterviewQuestionItem,
    QuestionType
)
from services.question_generator import generate_questions
import logging

logger = logging.getLogger(__name__)
router = APIRouter()

@router.post("/interview/questions", 
             response_model=InterviewQuestionResponse,
             summary="면접 질문 생성",
             description="이력서와 지원 포지션을 기반으로 맞춤형 면접 질문을 생성합니다.")
async def generate_interview_questions(request: InterviewQuestionRequest):
    try:
        logger.info(f"면접 질문 생성 요청 - 포지션: {request.position}, 질문 수: {request.question_count}")
        
        # 질문 생성 서비스 호출
        questions_raw = generate_questions(
            resume=request.resume,
            position=request.position,
            question_count=request.question_count
        )
        
        if not questions_raw:
            raise HTTPException(
                status_code=500,
                detail="질문 생성에 실패했습니다. 다시 시도해주세요."
            )
        
        # 응답 형식에 맞게 변환
        questions = []
        for q in questions_raw[:request.question_count]:
            try:
                question_item = InterviewQuestionItem(
                    content=q["content"],
                    type=QuestionType(q["type"]),
                    category=q["category"],
                    difficultyLevel=int(q["difficultyLevel"])
                )
                questions.append(question_item)
            except Exception as e:
                logger.warning(f"질문 변환 실패: {e}, 질문: {q}")
                continue
        
        if not questions:
            raise HTTPException(
                status_code=500,
                detail="유효한 질문을 생성하지 못했습니다."
            )
        
        logger.info(f"성공적으로 {len(questions)}개의 질문을 생성했습니다.")
        
        return InterviewQuestionResponse(questions=questions)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"면접 질문 생성 중 오류: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"면접 질문 생성 중 오류가 발생했습니다: {str(e)}"
        )

@router.get("/interview/health")
async def interview_health():
    """면접 질문 생성 서비스 상태 확인"""
    return {"status": "healthy", "service": "interview_questions"}