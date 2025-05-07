# services/question_generator.py

import os
from openai import OpenAI
from dotenv import load_dotenv
from pathlib import Path
import re
load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def load_prompt_template(template_path: str) -> str:
    with open(template_path, "r", encoding="utf-8") as f:
        return f.read()

def generate_interview_questions(resume: str, cover_letter: str, job_description: str, num_questions: int = 3) -> list:
    prompt_template = load_prompt_template("prompt_templates/question_prompt.txt")

    full_prompt = prompt_template.format(
        resume=resume,
        cover_letter=cover_letter,
        job_description=job_description,
        num_questions=num_questions
    )

    system_prompt = (
        "너는 훌륭한 면접관이야. 아래 정보를 참고해서, 실제 개발자 면접에서 나올 법한 구체적인 질문 "
        f"{num_questions}개를 만들어줘. 각 질문은 지원자의 경험, 기술 역량, 프로젝트 기반 실무성에 초점을 맞춰야 해."
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
    questions = [
        re.sub(r"^\s*[\d]+[\.\)]\s*", "", line).strip()
        for line in content.split("\n")
        if re.match(r"^\s*[\d]+[\.\)]\s*", line)
    ]

    return questions
