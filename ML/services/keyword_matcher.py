from konlpy.tag import Okt
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

okt = Okt()

def extract_nouns(text: str) -> str:
    """주어진 텍스트에서 명사만 추출하여 문자열로 반환"""
    nouns = okt.nouns(text)
    return " ".join(nouns)

def calculate_keyword_similarity(resume: str, cover_letter: str, question: str) -> tuple[float, list[str]]:
    """사용자 정보와 질문 간 키워드 기반 유사도 계산 및 공통 명사 반환"""
    # 명사 추출
    user_text = extract_nouns(resume + "\n" + cover_letter)
    question_text = extract_nouns(question)

    # 공통 키워드 추출
    user_keywords = set(user_text.split())
    question_keywords = set(question_text.split())
    matched_keywords = list(user_keywords & question_keywords)

    # TF-IDF 기반 유사도
    docs = [user_text, question_text]
    vectorizer = TfidfVectorizer()
    tfidf_matrix = vectorizer.fit_transform(docs)
    score = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:2])[0][0]

    return round(score, 4), matched_keywords


# 🧪 테스트 블록
if __name__ == "__main__":
    from pathlib import Path

    # 샘플 데이터 경로
    base_path = Path(__file__).resolve().parent.parent / "sample_inputs"
    resume_path = base_path / "sample_resume.txt"
    cover_path = base_path / "sample_cover_letter.txt"

    with open(resume_path, "r", encoding="utf-8") as f:
        resume = f.read()
    with open(cover_path, "r", encoding="utf-8") as f:
        cover_letter = f.read()

    sample_question = "Spring Boot와 JPA를 활용한 백엔드 개발 경험이 있다면 설명해주세요."

    score, keywords = calculate_keyword_similarity(resume, cover_letter, sample_question)

    print("\n[키워드 유사도 분석 결과]")
    print("유사도 점수:", score)
    print("공통 키워드:", keywords)
