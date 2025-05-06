# app/routes/evaluation.py

from fastapi import APIRouter
from schemas.evaluation_schema import EvaluationRequest, EvaluationResponse
from services.answer_evaluator import evaluate_answer
router = APIRouter()

@router.post("/evaluate", response_model=EvaluationResponse)
def evaluate_answer_route(request: EvaluationRequest):
    raw_result = evaluate_answer(
        answer=request.answer,
        question=request.question,
        resume=request.resume,
        cover_letter=request.cover_letter
    )
    return summarize_evaluation_scores(raw_result)


def summarize_similarity_scores(keyword_score: float, semantic_score: float) -> dict:
    """
    키워드 및 의미론 유사도를 딕셔너리 형태로 정리
    """
    return {
        "keyword_similarity": round(keyword_score, 4),
        "semantic_similarity": round(semantic_score, 4)
    }


def summarize_evaluation_scores(raw: dict) -> dict:
    """
    LLM 평가 응답에서 점수 및 피드백을 구조화
    """
    keys = ["관련성", "구체성", "실무성", "유효성", "총점", "피드백"]
    result = {}
    for key in keys:
        if key in raw:
            value = raw[key]
            try:
                result[key] = int(value) if key != "피드백" else value
            except ValueError:
                result[key] = value
    return result

