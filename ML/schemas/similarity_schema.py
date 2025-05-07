# app/schemas/similarity_schema.py

from pydantic import BaseModel
from typing import List


class KeywordSimilarityRequest(BaseModel):
    resume: str
    cover_letter: str
    question: str


class KeywordSimilarityResponse(BaseModel):
    matched_keywords: List[str]
    keyword_match_score: float


class SemanticSimilarityRequest(BaseModel):
    resume: str
    cover_letter: str
    question: str


class SemanticSimilarityResponse(BaseModel):
    similarity_score: float
