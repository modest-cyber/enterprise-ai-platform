from fastapi import FastAPI
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Enterprise AI Agent Service"
    debug: bool = False
    openai_api_key: str = ""
    openai_model: str = "gpt-4o"
    chroma_persist_dir: str = "./chroma_db"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    @app.get("/health")
    async def root_health():
        return {"status": "ok"}

    from api.routes import router
    app.include_router(router, prefix="/api/v1")

    from api.knowledge import router as knowledge_router
    app.include_router(knowledge_router, prefix="/api/v1/knowledge")

    return app
