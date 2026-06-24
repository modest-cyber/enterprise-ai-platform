import logging
import sys

from fastapi import FastAPI, HTTPException
from pydantic_settings import BaseSettings

# ── 日志配置 ──────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[logging.StreamHandler(sys.stdout)],
)
# 抑制第三方库的 DEBUG 日志
logging.getLogger("httpx").setLevel(logging.WARNING)
logging.getLogger("httpcore").setLevel(logging.WARNING)
logging.getLogger("urllib3").setLevel(logging.WARNING)
logging.getLogger("pymilvus").setLevel(logging.WARNING)
# ──────────────────────────────────────────────────────────


class Settings(BaseSettings):
    app_name: str = "Enterprise AI Agent Service"
    debug: bool = False
    openai_api_key: str = ""
    openai_model: str = "gpt-4o"
    chroma_persist_dir: str = "./chroma_db"

    # Embedding
    embedding_provider: str = "ollama"
    embedding_device: str = "cpu"
    ollama_base_url: str = "http://localhost:11434"
    ollama_embedding_model: str = "nomic-embed-text"

    # Milvus
    milvus_mode: str = "docker"
    milvus_host: str = "localhost"
    milvus_port: str = "19530"

    # JWT / 内部认证
    jwt_secret: str = "abcdefghijklmnopqrstuvwxyz"  # 与 Spring Boot token.secret 一致
    internal_secret: str = "internal-shared-secret"  # 预共享密钥
    spring_boot_url: str = "http://localhost:8080"   # Spring Boot 地址

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"


settings = Settings()


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    # 启动自检（延迟导入，不影响路由注册）
    _startup_ok = False

    @app.on_event("startup")
    async def on_startup():
        nonlocal _startup_ok
        from app.health import get_health_checker
        checker = get_health_checker()
        status = checker.run_startup_check()
        _startup_ok = status.overall != "unavailable"

    @app.get("/health")
    async def health():
        from app.health import get_health_checker
        checker = get_health_checker()
        status = checker.get_status()
        result = {
            "python": status.python,
            "embedding": status.embedding,
            "milvus": status.milvus,
            "rag": status.rag,
            "upload_dir": status.upload_dir,
            "dependencies": status.dependencies,
            "overall": status.overall,
        }
        if status.overall == "unavailable":
            raise HTTPException(status_code=503, detail=result)
        return result

    from api.routes import router
    app.include_router(router, prefix="/api/v1")

    from api.knowledge import router as knowledge_router
    app.include_router(knowledge_router, prefix="/api/v1/knowledge")

    from api.tools import router as tools_router
    app.include_router(tools_router, prefix="/api/v1/tools")

    from api.preview import router as preview_router
    app.include_router(preview_router, prefix="/api/v1")

    from api.forge_routes import router as forge_router
    app.include_router(forge_router, prefix="/api/v1")

    return app