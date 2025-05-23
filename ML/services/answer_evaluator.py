# services/answer_evaluator.py

import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def evaluate_answer(answer: str, question: str, position: str, resume: str = "", cover_letter: str = "") -> dict:
    system_prompt = (
        "너는 인사담당자야. 아래 문항에 대해 지원자의 답변이 얼마나 관련성 있고, 구체적이며, 실무성과 유효성을 갖췄는지 평가해줘. "
        "각 항목은 10점 만점이고 총점은 40점 만점이야. 각 항목 점수와 함께 간단한 피드백도 같이 줘."
    )

    user_input = f"""
    [지원 직무]
    {position}

    [이력서]
    {resume if resume else "없음"}

    [자기소개서]
    {cover_letter if cover_letter else "없음"}

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

    # 점수 파싱
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


# 로컬 테스트용 예시 실행
if __name__ == "__main__":
    question = "스프링 부트 기반 프로젝트에서 본인의 역할은 무엇이었나요?"
    answer = "JWT 기반 인증 기능을 맡아서 구현했으며, Redis를 이용한 리프레시 토큰 저장도 처리했습니다."
    position = "백엔드 개발자"

    result = evaluate_answer(answer, question, position)
    print("\n[LLM 평가 결과]")
    for k, v in result.items():
        print(f"{k}: {v}")
