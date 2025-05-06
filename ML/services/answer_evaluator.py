# services/answer_evaluator.py

import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def evaluate_answer(answer: str, question: str, resume: str, cover_letter: str) -> dict:
    system_prompt = (
        "너는 인사담당자야. 아래 문항에 대해 지원자의 답변이 얼마나 관련성 있고, 구체적이며, 실무성과 유효성을 갖췄는지 평가해줘. "
        "각 항목은 10점 만점이고 총점은 40점 만점이야. 각 항목 점수와 함께 간단한 피드백도 같이 줘."
    )

    user_input = f"""
    [이력서]
    {resume}

    [자기소개서]
    {cover_letter}

    [면접 질문]
    {question}

    [지원자 답변]
    {answer}

    [평가 양식]
    관련성: (1~10점)
    구체성: (1~10점)
    실무성: (1~10점)
    유효성: (1~10점)
    총점: (합계)
    피드백: (문장 1~2개로 요약)
    """

    response = client.chat.completions.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_input}
        ],
        temperature=0.5
    )

    content = response.choices[0].message.content.strip()

    # 점수 파싱 (정수 처리)
    result = {}
    for line in content.split("\n"):
        if ":" in line:
            key, value = line.split(":", 1)
            key = key.strip()
            value = value.strip()

            if key in {"관련성", "구체성", "실무성", "유효성", "총점"}:
                digits = ''.join(filter(str.isdigit, value))
                result[key] = int(digits) if digits else 0
            else:
                result[key] = value

    return result

if __name__ == "__main__":
    from pathlib import Path

    base_path = Path(__file__).resolve().parent.parent / "sample_inputs"
    resume_path = base_path / "sample_resume.txt"
    cover_path = base_path / "sample_cover_letter.txt"

    with open(resume_path, "r", encoding="utf-8") as f:
        resume = f.read()
    with open(cover_path, "r", encoding="utf-8") as f:
        cover_letter = f.read()

    question = "자바와 스프링 부트를 이용한 프로젝트에서 본인의 역할과 해결한 문제를 설명해주세요."
    answer = "저는 백엔드 개발을 담당했고, 로그인 기능에서 JWT 인증과 리프레시 토큰 관리를 맡았습니다. 토큰 만료 이슈가 발생해 Redis를 활용하여 해결했습니다."

    result = evaluate_answer(answer, question, resume, cover_letter)

    print("\n[LLM 평가 결과]")
    for k, v in result.items():
        print(f"{k}: {v}")
