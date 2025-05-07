from sentence_transformers import SentenceTransformer, util
import numpy as np

sbert_model = SentenceTransformer("jhgan/ko-sbert-sts")

def calculate_semantic_similarity(resume: str, cover_letter: str, question: str) -> float:
    user_text = resume + "\n" + cover_letter
    embeddings = sbert_model.encode([user_text, question], convert_to_tensor=True)
    similarity = util.pytorch_cos_sim(embeddings[0], embeddings[1]).item()
    return round(similarity, 4)



