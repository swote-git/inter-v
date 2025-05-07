# app/routes/keyword.py

from fastapi import APIRouter
from schemas.similarity_schema import (
    KeywordSimilarityRequest,
    KeywordSimilarityResponse,
    SemanticSimilarityRequest,
    SemanticSimilarityResponse,
)
from services.keyword_matcher import calculate_keyword_similarity
from services.semantic_matcher import calculate_semantic_similarity

router = APIRouter()

@router.post("/similarity/keyword", response_model=KeywordSimilarityResponse)
def keyword_similarity_route(request: KeywordSimilarityRequest):
    score, keywords = calculate_keyword_similarity(
        resume=request.resume,
        cover_letter=request.cover_letter,
        question=request.question
    )
    return {
        "matched_keywords": keywords,
        "keyword_match_score": score
    }

@router.post("/similarity/semantic", response_model=SemanticSimilarityResponse)
def semantic_similarity_route(request: SemanticSimilarityRequest):
    score = calculate_semantic_similarity(
        resume=request.resume,
        cover_letter=request.cover_letter,
        question=request.question
    )
    return {
        "similarity_score": score
    }
