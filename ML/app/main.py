from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routes import interview, evaluation, keyword,simulate

app = FastAPI(
    title="InterV API",
    description="LLM 기반 개발 면접 질문 생성 및 평가 시스템",
    version="1.0.0"
)

# CORS 설정 (프론트엔드 연동 시 허용 도메인 지정 필요)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # TODO: 운영 시 구체 도메인 지정
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(interview.router)
app.include_router(evaluation.router)
app.include_router(keyword.router)
app.include_router(simulate.router, prefix="/simulate")
# 헬스 체크 루트
@app.get("/health")
def check_health():
    return {"message": "InterV API is running!"}
