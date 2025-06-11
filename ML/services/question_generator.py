import os
import re
import json
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

    # 두번째 GPT 호출을 위한 프롬프트 생성 (새로운 프롬프트로 교체)
    classification_prompt = (
        "다음은 개발자 면접에서 사용될 수 있는 질문 목록입니다. 각 질문에 대해 아래 형식의 JSON 배열로 다음 항목들을 추론해 주세요:\n"
        "- content: 질문 내용 그대로\n"
        "- type: 질문 유형 (TECHNICAL, PERSONALITY, PROJECT, SITUATION 중 하나)\n"
        "- category: 질문 내용에 가장 적절한 항목을 선택 (Java, Spring, SQL, React, Python, Teamwork, Leadership, Communication, Project Experience, Conflict Resolution, Decision Making)\n"
        "- difficultyLevel: 1(쉬움), 2(중간), 3(어려움) 중 하나\n\n"
        "응답은 반드시 다음 JSON 배열 형식으로만 주세요:\n"
        "[\n"
        "  {\n"
        "    \"content\": \"...\",\n"
        "    \"type\": \"...\",\n"
        "    \"category\": \"...\",\n"
        "    \"difficultyLevel\": 1\n"
        "  },\n"
        "  ...\n"
        "]"
    )

    classification_response = client.chat.completions.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": classification_prompt},
            {"role": "user", "content": json.dumps(question_lines, ensure_ascii=False)}
        ],
        temperature=0.0
    )

    classification_content = classification_response.choices[0].message.content.strip()

    try:
        parsed_questions = json.loads(classification_content)
    except json.JSONDecodeError as e:
        raise ValueError(f"GPT에서 반환된 JSON 파싱 실패: {e}\n응답 내용: {classification_content}")

    # 스키마에 맞게 타입 강제 변환 및 필드 확인
    valid_types = {"TECHNICAL", "PERSONALITY", "PROJECT", "SITUATION"}
    valid_categories = {
        "Java", "Spring", "SQL", "React", "Python",
        "Teamwork", "Leadership", "Communication",
        "Project Experience", "Conflict Resolution", "Decision Making"
    }

    cleaned_questions = []
    for q in parsed_questions:
        content = str(q.get("content", "")).strip()
        q_type = str(q.get("type", "")).strip().upper()
        category_raw = str(q.get("category", "")).strip()
        category = category_raw.split(",")[0].strip() if "," in category_raw else category_raw
        difficulty = q.get("difficultyLevel")

        if q_type not in valid_types:
            raise ValueError(f"Invalid type value: {q_type}")
        if category not in valid_categories:
            raise ValueError(f"Invalid category value: {category}")
        if not isinstance(difficulty, int) or difficulty not in {1,2,3}:
            raise ValueError(f"Invalid difficultyLevel value: {difficulty}")

        cleaned_questions.append({
            "content": content,
            "type": q_type,
            "category": category,
            "difficultyLevel": difficulty
        })

    return cleaned_questions
