# services/question_generator.py

import os
import re
import random
from openai import OpenAI
from dotenv import load_dotenv
from pathlib import Path

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def load_prompt_template(template_path: str) -> str:
    with open(template_path, "r", encoding="utf-8") as f:
        return f.read()

def generate_questions(resume: str, position: str, question_count: int) -> list:
    prompt_template = load_prompt_template("prompt_templates/question_prompt.txt")

    full_prompt = prompt_template.format(
        resume=resume,
        position=position,
        question_count=question_count
    )

    system_prompt = (
        "너는 훌륭한 면접관이야. 아래 정보를 참고해서, 실제 개발자 면접에서 나올 법한 구체적인 질문 "
        f"{question_count}개를 만들어줘. 각 질문은 지원자의 경험, 기술 역량, 프로젝트 기반 실무성에 초점을 맞춰야 해. "
        "반드시 각 질문 앞에 '1. ', '2. '와 같은 숫자를 붙여서 번호를 매겨줘. 다른 형식은 허용되지 않아."
    )

    response = client.chat.completions.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": full_prompt}
        ],
        temperature=0.7
    )

    content = response.choices[0].message.content.strip()

    import logging
    logging.basicConfig(level=logging.INFO)
    logging.info("[DEBUG] GPT 응답:")
    logging.info(content)

    # 질문 리스트 파싱
    question_lines = [
        re.sub(r"^\s*[\d]+[\.\)]\s*", "", line).strip()
        for line in content.split("\n")
        if re.match(r"^\s*[\d]+[\.\)]\s*", line)
    ]

    if not question_lines:
        raise ValueError("GPT 응답에서 질문을 추출하지 못했습니다. 번호가 있는 질문 형식을 확인하세요.")

    # 임시 매핑: 랜덤 타입/카테고리/난이도
    TYPES = ["TECHNICAL", "PERSONALITY", "PROJECT", "SITUATION"]
    CATEGORIES = {
        "TECHNICAL": ["Java", "Spring", "SQL", "React", "Python"],
        "PERSONALITY": ["Teamwork", "Leadership", "Communication"],
        "PROJECT": ["Project Experience"],
        "SITUATION": ["Conflict Resolution", "Decision Making"]
    }

    questions = []
    for q in question_lines[:question_count]:
        q_type = random.choice(TYPES)
        category = random.choice(CATEGORIES[q_type])
        difficulty = random.randint(1, 3)

        questions.append({
            "content": q,
            "type": q_type,
            "category": category,
            "difficultyLevel": difficulty
        })

    return questions
