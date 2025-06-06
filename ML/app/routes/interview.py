# ML/app/routes/interview.py

from fastapi import APIRouter
from schemas.interview_schema import (
    InterviewQuestionRequest,
    InterviewQuestionResponse,
    InterviewQuestionItem,
    InterviewFeedbackRequest,
    InterviewFeedbackResponse
)
from services.question_generator import generate_questions
from services.answer_evaluator import evaluate_answer  # 피드백용
from fastapi.responses import JSONResponse

router = APIRouter()

@router.post("/interview/questions", response_model=InterviewQuestionResponse)
def generate_interview_questions(request: InterviewQuestionRequest):
    questions_raw = generate_questions(
        resume=request.resume,
        position=request.position,
        question_count=request.questionCount
    )

    questions = [
        InterviewQuestionItem(
            content=q["content"],
            type=q["type"],
            category=q["category"],
            difficultyLevel=int(q["difficultyLevel"])
        )
        for q in questions_raw[:request.questionCount]
    ]

    return InterviewQuestionResponse(questions=questions)


@router.post("/interview/feedback", response_model=InterviewFeedbackResponse)
def generate_interview_feedback(request: InterviewFeedbackRequest):
    feedback = evaluate_answer(
        question=request.question,
        answer=request.answer,
        position=request.position
    )
    return InterviewFeedbackResponse(feedback=feedback)
