"""服务健康检查 — Embedding / Milvus / RAG / 依赖"""

import logging
import os
from dataclasses import dataclass, field
from typing import Optional

logger = logging.getLogger(__name__)


@dataclass
class HealthStatus:
    python: bool = True
    embedding: dict = field(default_factory=lambda: {"status": False, "error": "未初始化"})
    milvus: dict = field(default_factory=lambda: {"status": False, "error": "未初始化"})
    rag: dict = field(default_factory=lambda: {"status": False, "error": "未初始化"})
    upload_dir: dict = field(default_factory=lambda: {"status": False, "error": "未初始化"})
    dependencies: dict = field(default_factory=dict)
    overall: str = "unavailable"


class HealthChecker:
    """启动时自检 + 运行时 /health 查询"""

    _instance: Optional["HealthChecker"] = None
    _status: Optional[HealthStatus] = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def check_dependencies(self) -> dict[str, bool]:
        deps = {}
        for mod_name in [
            "sentence_transformers",
            "pymilvus",
            "pdfplumber",
            "docx",
            "fitz",
            "openai",
            "httpx",
            "jsonschema",
            "numpy",
        ]:
            try:
                __import__(mod_name)
                deps[mod_name] = True
            except ImportError:
                deps[mod_name] = False
                logger.warning("缺少依赖: %s", mod_name)
        return deps

    def check_embedding(self) -> dict:
        try:
            from app.rag.embedding_service import EmbeddingService
            svc = EmbeddingService()
            model = svc._get_model()
            result = {
                "status": True,
                "provider": svc.provider,
                "device": os.getenv("EMBEDDING_DEVICE", "cpu"),
            }
            if svc.provider == "sentence-transformers":
                result["model"] = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-zh-v1.5")
            elif svc.provider == "openai":
                result["model"] = os.getenv("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small")
            elif svc.provider == "ollama":
                result["model"] = os.getenv("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text")
            return result
        except Exception as e:
            logger.error("Embedding 自检失败: %s", e)
            return {"status": False, "error": str(e)[:200]}

    def check_milvus(self) -> dict:
        try:
            from app.rag.milvus_service import MilvusService, COLLECTION_NAME
            svc = MilvusService()
            client = svc.client
            has_coll = svc.has_collection()
            result = {
                "status": True,
                "mode": svc._mode,
                "host": svc._host if svc._mode == "docker" else "local",
                "port": svc._port if svc._mode == "docker" else "-",
                "collection": COLLECTION_NAME,
                "exists": has_coll,
                "total_vectors": svc.count() if has_coll else 0,
            }
            return result
        except Exception as e:
            logger.error("Milvus 自检失败: %s", e)
            return {"status": False, "error": str(e)[:200]}

    def check_rag(self) -> dict:
        try:
            from app.rag.chunk_service import split_text
            from app.rag.index_service import IndexService
            result = {
                "status": True,
                "chunk_service": True,
                "index_service": True,
            }
            return result
        except Exception as e:
            logger.error("RAG 自检失败: %s", e)
            return {"status": False, "error": str(e)[:200]}

    def check_upload_dir(self) -> dict:
        try:
            upload_dir = os.getenv("UPLOAD_DIR", os.path.join(os.getcwd(), "upload"))
            os.makedirs(upload_dir, exist_ok=True)
            return {"status": True, "path": os.path.abspath(upload_dir)}
        except Exception as e:
            logger.error("上传目录自检失败: %s", e)
            return {"status": False, "error": str(e)[:200]}

    def run_startup_check(self) -> HealthStatus:
        status = HealthStatus()
        status.dependencies = self.check_dependencies()

        missing = [k for k, v in status.dependencies.items() if not v]
        if missing:
            logger.error("关键依赖缺失: %s", missing)
            status.overall = "unavailable"
            return status

        status.embedding = self.check_embedding()
        status.milvus = self.check_milvus()
        status.rag = self.check_rag()
        status.upload_dir = self.check_upload_dir()

        all_ok = all([
            status.embedding.get("status", False),
            status.milvus.get("status", False),
            status.rag.get("status", False),
            status.upload_dir.get("status", False),
        ])
        some_ok = any([
            status.embedding.get("status", False),
            status.milvus.get("status", False),
        ])

        if all_ok:
            status.overall = "ok"
        elif some_ok:
            status.overall = "degraded"
        else:
            status.overall = "unavailable"

        self._status = status
        logger.info("启动自检完成: overall=%s", status.overall)
        return status

    def get_status(self) -> HealthStatus:
        if self._status is None:
            self._status = self.run_startup_check()
        return self._status


def get_health_checker() -> HealthChecker:
    return HealthChecker()