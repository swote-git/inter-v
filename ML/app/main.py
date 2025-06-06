from fastapi import FastAPI, HTTPException, Request, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.exception_handlers import http_exception_handler
import logging
import traceback
import os
from dotenv import load_dotenv
from app.routes import interview, evaluation, keyword, simulate
from app.database import test_database_connection, get_db

# 환경변수 로드
load_dotenv()

# 로깅 설정
log_level = os.getenv("LOG_LEVEL", "INFO")
logging.basicConfig(
    level=getattr(logging, log_level),
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="InterV API",
    description="LLM 기반 개발 면접 질문 생성 및 평가 시스템",
    version="1.0.0",
    docs_url="/docs" if os.getenv("APP_ENV") != "production" else None,
    redoc_url="/redoc" if os.getenv("APP_ENV") != "production" else None
)

# CORS 설정 개선
allowed_origins = os.getenv("ALLOWED_ORIGINS", "").split(",")
if not allowed_origins or allowed_origins == [""]:
    # 개발 환경용 기본값
    allowed_origins = [
        "http://localhost:8080",
        "http://localhost:3000",
        "http://127.0.0.1:8080",
        "http://127.0.0.1:5173",
        "https://api.interv.swote.dev",
        "https://ml.interv.swote.dev",
        "*"
    ]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
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
            "message": str(exc) if os.getenv("DEBUG", "false").lower() == "true" else "서버에서 오류가 발생했습니다.",
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

# 개선된 헬스 체크
@app.get("/health")
def check_health():
    """시스템 전체 헬스체크"""
    health_status = {
        "status": "healthy",
        "message": "InterV API is running!",
        "version": "1.0.0",
        "checks": {}
    }
    
    # 데이터베이스 연결 확인
    try:
        db_connected = test_database_connection()
        health_status["checks"]["database"] = "connected" if db_connected else "disconnected"
    except Exception as e:
        health_status["checks"]["database"] = f"error: {str(e)}"
        health_status["status"] = "unhealthy"
    
    # OpenAI API 키 확인
    openai_key = os.getenv("OPENAI_API_KEY")
    health_status["checks"]["openai"] = "configured" if openai_key else "not_configured"
    
    # ML 모델 상태 확인
    try:
        from services.semantic_matcher import sbert_model
        health_status["checks"]["ml_models"] = "loaded"
    except Exception as e:
        health_status["checks"]["ml_models"] = f"error: {str(e)}"
        health_status["status"] = "degraded"
    
    return health_status

# 라이브니스 프로브 (간단한 버전)
@app.get("/health/live")
def liveness_check():
    """Kubernetes liveness probe용"""
    return {"status": "alive"}

# 레디니스 프로브
@app.get("/health/ready")
def readiness_check():
    """Kubernetes readiness probe용"""
    try:
        # 중요한 서비스들이 준비되었는지 확인
        db_ready = test_database_connection()
        openai_ready = bool(os.getenv("OPENAI_API_KEY"))
        
        if db_ready and openai_ready:
            return {"status": "ready"}
        else:
            raise HTTPException(status_code=503, detail="Service not ready")
    except Exception as e:
        logger.error(f"Readiness check failed: {e}")
        raise HTTPException(status_code=503, detail="Service not ready")

# 서버 정보 엔드포인트
@app.get("/info")
def get_info():
    return {
        "title": "InterV API",
        "description": "LLM 기반 개발 면접 질문 생성 및 평가 시스템",
        "version": "1.0.0",
        "environment": os.getenv("APP_ENV", "development"),
        "endpoints": {
            "health": "/health",
            "interview_questions": "/interview/questions",
            "evaluate": "/evaluate",
            "keyword_similarity": "/similarity/keyword",
            "semantic_similarity": "/similarity/semantic"
        }
    }

# 애플리케이션 시작 이벤트
@app.on_event("startup")
async def startup_event():
    logger.info("Starting InterV API...")
    
    # 필수 환경변수 확인
    required_env_vars = ["OPENAI_API_KEY"]
    missing_vars = [var for var in required_env_vars if not os.getenv(var)]
    
    if missing_vars:
        logger.error(f"Missing required environment variables: {missing_vars}")
        raise RuntimeError(f"Missing required environment variables: {missing_vars}")
    
    # 데이터베이스 연결 테스트
    if not test_database_connection():
        logger.warning("Database connection failed, but continuing startup...")
    
    # ML 모델 사전 로딩 (선택사항)
    try:
        logger.info("Pre-loading ML models...")
        from services.semantic_matcher import sbert_model
        logger.info("ML models loaded successfully")
    except Exception as e:
        logger.warning(f"Failed to pre-load ML models: {e}")
    
    logger.info("InterV API started successfully")

# 애플리케이션 종료 이벤트
@app.on_event("shutdown")
async def shutdown_event():
    logger.info("Shutting down InterV API...")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=int(os.getenv("PORT", 8000)),
        reload=os.getenv("APP_ENV") == "development",
        log_level=log_level.lower()
    )