import os
import re
import json
from openai import OpenAI
from dotenv import load_dotenv
from pathlib import Path
from langchain_community.chat_models import ChatOpenAI
from langchain.prompts import PromptTemplate
from langchain.chains import LLMChain

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def load_prompt_template(template_path: str) -> str:
    with open(template_path, "r", encoding="utf-8") as f:
        return f.read()

def generate_questions(resume: str, position: str, question_count: int) -> list:
    llm = ChatOpenAI(model="gpt-4", temperature=0.7, top_p=0.95, max_tokens=512)

    # Load question generation prompt
    prompt_template_str = load_prompt_template("prompt_templates/question_prompt.txt")
    question_chain = LLMChain(
        llm=llm,
        prompt=PromptTemplate(
            input_variables=["resume", "position", "question_count"],
            template=prompt_template_str + "\n각 질문은 반드시 '1. ', '2. '처럼 숫자로 시작해야 하며 줄 바꿈으로 구분해 주세요."
        )
    )

    result_raw = question_chain.invoke({
        "resume": resume,
        "position": position,
        "question_count": question_count
    })
    result = result_raw["text"] if isinstance(result_raw, dict) and "text" in result_raw else str(result_raw)
    print("GPT raw response:\n", result)

    import re
    question_lines = [
        re.sub(r"^\s*[\d]+[\.\)]\s*", "", line).strip()
        for line in result.split("\n")
        if re.match(r"^\s*[\d]+[\.\)]\s*", line)
    ]

    if not question_lines:
        raise ValueError("GPT 응답에서 질문을 추출하지 못했습니다. 번호가 있는 질문 형식을 확인하세요.")

    # Classification Chain
    classification_prompt = PromptTemplate(
    input_variables=["questions"],
    template="""
    다음은 개발자 면접 질문 목록입니다:

    {questions}

    각 질문에 대해 아래 형식의 JSON 배열로 다음 항목들을 추론해 주세요:
    - content: 질문 내용 그대로
    - type: 질문 유형 (TECHNICAL, PERSONALITY, PROJECT, SITUATION 중 하나)
    - category: 질문 내용에 가장 적절한 항목을 선택 (Java, Spring, SQL, React, Python, Teamwork, Leadership, Communication, Project Experience, Conflict Resolution, Decision Making)
    - difficultyLevel: 1(쉬움), 2(중간), 3(어려움) 중 하나

    응답은 반드시 다음 JSON 배열 형식으로만 주세요:
    [
    {{
        "content": "...",
        "type": "...",
        "category": "...",
        "difficultyLevel": 1
    }},
    ...
    ]
    """
    )

    classification_chain = LLMChain(llm=ChatOpenAI(model="gpt-4", temperature=0.7, top_p=0.95, max_tokens=512), prompt=classification_prompt)
    classification_result = classification_chain.invoke({"questions": "\n".join(question_lines)})

    try:
        parsed_questions = classification_result if isinstance(classification_result, list) else classification_result.get("text", classification_result)
        if isinstance(parsed_questions, str):
            parsed_questions = json.loads(parsed_questions)
    except json.JSONDecodeError as e:
        raise ValueError(f"GPT에서 반환된 JSON 파싱 실패: {e}\n응답 내용: {classification_result}")

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
