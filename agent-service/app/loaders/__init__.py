"""统一文档加载器 — 工厂模式，根据文件扩展名自动选择解析器"""

import logging
from abc import ABC, abstractmethod
from pathlib import Path

logger = logging.getLogger(__name__)


class BaseLoader(ABC):
    """文档加载器基类 — 所有格式最终输出纯文本 str"""

    @abstractmethod
    def load(self, file_path: str) -> str:
        ...

    @staticmethod
    def supports(extension: str) -> bool:
        return False


class DocumentLoaderFactory:
    """工厂：根据后缀返回对应 Loader 实例"""

    _loaders: dict[str, BaseLoader] = {}
    _initialized = False

    @classmethod
    def _ensure_initialized(cls):
        if cls._initialized:
            return
        from app.loaders.pdf_loader import PdfLoader
        from app.loaders.docx_loader import DocxLoader
        from app.loaders.doc_loader import DocLoader
        from app.loaders.txt_loader import TxtLoader
        from app.loaders.markdown_loader import MarkdownLoader
        from app.loaders.excel_loader import ExcelLoader
        from app.loaders.ppt_loader import PowerPointLoader

        cls._loaders = {
            "pdf": PdfLoader(),
            "docx": DocxLoader(),
            "doc": DocLoader(),
            "txt": TxtLoader(),
            "md": MarkdownLoader(),
            "xlsx": ExcelLoader(),
            "pptx": PowerPointLoader(),
        }
        cls._initialized = True
        logger.info("DocumentLoaderFactory 初始化完成, 注册 %d 个 Loader", len(cls._loaders))

    @classmethod
    def get_supported_extensions(cls) -> list[str]:
        cls._ensure_initialized()
        return sorted(cls._loaders.keys())

    @classmethod
    def is_supported(cls, extension: str) -> bool:
        cls._ensure_initialized()
        return extension.lower() in cls._loaders

    @classmethod
    def get_loader(cls, file_path: str) -> BaseLoader:
        cls._ensure_initialized()
        suffix = Path(file_path).suffix.lower().lstrip(".")
        if suffix not in cls._loaders:
            supported = ", ".join(cls._loaders.keys())
            raise ValueError(f"不支持的文件格式: .{suffix}，支持的格式: {supported}")
        return cls._loaders[suffix]

    @classmethod
    def load(cls, file_path: str) -> str:
        loader = cls.get_loader(file_path)
        logger.info("使用 %s 加载: %s", loader.__class__.__name__, file_path)
        return loader.load(file_path)