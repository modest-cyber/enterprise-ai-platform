"""Milvus 服务 — 支持 Docker Milvus + Milvus Lite，Collection 生命周期管理 + CRUD"""

import logging
import os
from pathlib import Path

from pymilvus import MilvusClient, DataType, connections

from .exceptions import MilvusException

logger = logging.getLogger(__name__)

COLLECTION_NAME = "ai_knowledge"
DIM = 768


class MilvusService:

    def __init__(self):
        self._mode = os.getenv("MILVUS_MODE", "lite").lower()
        self._host = os.getenv("MILVUS_HOST", "localhost")
        self._port = os.getenv("MILVUS_PORT", "19530")
        self._db_path = os.getenv("MILVUS_DB_PATH", "./data/milvus.db")
        self._uri = os.getenv("MILVUS_URI", "")
        self._token = os.getenv("MILVUS_TOKEN", "")
        self._client: MilvusClient | None = None

    def _connect_docker(self) -> MilvusClient:
        uri = self._uri or f"http://{self._host}:{self._port}"
        logger.info("连接 Docker Milvus: %s", uri)
        client = MilvusClient(uri=uri, token=self._token or None)
        return client

    def _connect_lite(self) -> MilvusClient:
        Path(self._db_path).parent.mkdir(parents=True, exist_ok=True)
        logger.info("连接 Milvus Lite: %s", self._db_path)
        client = MilvusClient(str(self._db_path))
        return client

    @property
    def client(self) -> MilvusClient:
        if self._client is None:
            if self._mode == "docker":
                self._client = self._connect_docker()
            else:
                self._client = self._connect_lite()
        return self._client

    def has_collection(self) -> bool:
        return self.client.has_collection(COLLECTION_NAME)

    def create_collection(self):
        if self.has_collection():
            logger.info("Collection '%s' 已存在, 跳过创建", COLLECTION_NAME)
            return

        schema = self.client.create_schema(enable_dynamic_field=False)
        schema.add_field("id", DataType.VARCHAR, is_primary=True, max_length=64, auto_id=False)
        schema.add_field("doc_id", DataType.INT64)
        schema.add_field("kb_id", DataType.INT64)
        schema.add_field("chunk_index", DataType.INT64)
        schema.add_field("content", DataType.VARCHAR, max_length=4096)
        schema.add_field("vector", DataType.FLOAT_VECTOR, dim=DIM)

        self.client.create_collection(
            collection_name=COLLECTION_NAME,
            schema=schema,
        )

        index_params = self.client.prepare_index_params()
        index_params.add_index(field_name="vector", index_type="IVF_FLAT", metric_type="COSINE", params={"nlist": 128})
        self.client.create_index(COLLECTION_NAME, index_params)

        self.client.load_collection(COLLECTION_NAME)
        logger.info("Collection '%s' 创建完成, dim=%d", COLLECTION_NAME, DIM)

    def drop_collection(self):
        if self.has_collection():
            self.client.drop_collection(COLLECTION_NAME)
            logger.info("Collection '%s' 已删除", COLLECTION_NAME)

    def insert(self, data: list[dict]) -> dict:
        try:
            self.create_collection()
            result = self.client.insert(COLLECTION_NAME, data)
            logger.info("Milvus insert 完成, count=%d", len(data))
            return result
        except Exception as e:
            raise MilvusException(f"Milvus insert 失败: {e}") from e

    def delete_by_doc_id(self, doc_id: int, kb_id: int):
        try:
            expr = f'doc_id == {doc_id} and kb_id == {kb_id}'
            # 先查询所有匹配的 ID（pymilvus 表达式 delete 有 bug，只删 1 条）
            results = self.client.query(
                collection_name=COLLECTION_NAME,
                filter=expr,
                output_fields=["id"],
                limit=10000,
            )
            ids_to_delete = [r["id"] for r in results]
            if ids_to_delete:
                result = self.client.delete(COLLECTION_NAME, ids=ids_to_delete)
                delete_count = result.get("delete_count", len(ids_to_delete)) if isinstance(result, dict) else len(ids_to_delete)
            else:
                delete_count = 0
            logger.info("Milvus 删除文档, doc_id=%d, kb_id=%d, found=%d, deleted=%s",
                        doc_id, kb_id, len(ids_to_delete), delete_count)
        except Exception as e:
            raise MilvusException(f"Milvus delete 失败: {e}") from e

    def count(self) -> int:
        try:
            if not self.has_collection():
                return 0
            self.client.load_collection(COLLECTION_NAME)
            stats = self.client.get_collection_stats(COLLECTION_NAME)
            return stats.get("row_count", 0)
        except Exception as e:
            raise MilvusException(f"Milvus count 失败: {e}") from e

    def search(self, vector: list[float], top_k: int = 5, expr: str | None = None) -> list[dict]:
        try:
            self.create_collection()
            search_params = {"metric_type": "COSINE", "params": {"nprobe": 8}}
            results = self.client.search(
                collection_name=COLLECTION_NAME,
                data=[vector],
                limit=top_k,
                search_params=search_params,
                output_fields=["doc_id", "kb_id", "chunk_index", "content"],
                filter=expr,
            )
            return results[0] if results else []
        except Exception as e:
            raise MilvusException(f"Milvus search 失败: {e}") from e