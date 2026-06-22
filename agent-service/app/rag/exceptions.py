"""RAG 模块异常定义"""


class KnowledgeBaseException(Exception):
    """知识库通用异常"""


class MilvusException(KnowledgeBaseException):
    """Milvus 操作异常"""


class EmbeddingException(KnowledgeBaseException):
    """Embedding 异常"""


class ChunkException(KnowledgeBaseException):
    """分块异常"""


class IndexException(KnowledgeBaseException):
    """索引异常"""