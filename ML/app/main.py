from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.exception_handlers import http_exception_handler
import logging
import traceback
from app.routes import interview, evaluation, keyword, simulate

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="InterV API",
    description="LLM 기반 개발 면접 질문 생성 및 평가 시스템",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS 설정 (Spring Boot 연동을 위해)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:8080",  # Spring Boot 기본 포트
        "http://localhost:3000",  # React 개발 서버
        "http://127.0.0.1:8080",
        "*"  # 개발 환경에서만 사용, 운영에서는 구체적 도메인 지정
    ],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

# 전역 예외 핸들러
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Global exception: {str(exc)}")
    logger.error(f"Traceback: {traceback.format_exc()}")
    
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "message": str(exc),
            "detail": "서버에서 오류가 발생했습니다."
        }
    )

@app.exception_handler(HTTPException)
async def custom_http_exception_handler(request: Request, exc: HTTPException):
    logger.warning(f"HTTP exception: {exc.status_code} - {exc.detail}")
    return await http_exception_handler(request, exc)

# 라우터 등록
app.include_router(interview.router, tags=["Interview"])
app.include_router(evaluation.router, tags=["Evaluation"])
app.include_router(keyword.router, tags=["Keyword"])
app.include_router(simulate.router, prefix="/simulate", tags=["Simulation"])

# 헬스 체크 루트
@app.get("/health")
def check_health():
    return {
        "status": "healthy",
        "message": "InterV API is running!",
        "version": "1.0.0"
    }

# 서버 정보 엔드포인트
@app.get("/info")
def get_info():
    return {
        "title": "InterV API",
        "description": "LLM 기반 개발 면접 질문 생성 및 평가 시스템",
        "version": "1.0.0",
        "endpoints": {
            "health": "/health",
            "interview_questions": "/interview/questions",
            "evaluate": "/evaluate",
            "keyword_similarity": "/similarity/keyword",
            "semantic_similarity": "/similarity/semantic"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )
