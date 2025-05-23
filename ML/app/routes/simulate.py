# app/routes/simulate.py

from fastapi import APIRouter
from schemas.interview_schema import SimulationRequest, SimulationResponse
from services.question_generator import generate_questions
from services.answer_evaluator import evaluate_answer
import random

router = APIRouter()

@router.post("/simulate", response_model=SimulationResponse)
def simulate_interview(request: SimulationRequest):
    # 1. 질문 생성
    questions = generate_questions(
        resume=request.resume,
        cover_letter=request.cover_letter,
        job_description=request.job_description,
        num_questions=request.num_questions
    )

    # 2. 질문 중 하나 선택 (랜덤 or 첫 번째)
    selected_question = questions[0] if questions else "질문 생성 실패"

    # 3. 답변 평가 수행
    result = evaluate_answer(
        answer=request.user_answer,
        question=selected_question,
        resume=request.resume,
        cover_letter=request.cover_letter
    )

    return {
        "generated_questions": questions,
        "selected_question": selected_question,
        "user_answer": request.user_answer,
        "evaluation_result": result
    }
