# app/routes/interview.py

from fastapi import APIRouter
from schemas.interview_schema import InterviewRequest, InterviewResponse
from services.question_generator import generate_interview_questions

router = APIRouter()

@router.post("/generate", response_model=InterviewResponse)
def generate_questions(request: InterviewRequest):
    questions = generate_interview_questions(
        resume=request.resume,
        cover_letter=request.cover_letter,
        job_description=request.job_description,
        num_questions=request.num_questions
    )
    return {"questions": questions}
