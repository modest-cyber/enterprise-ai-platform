"""PDF 加载器 — 使用 PyMuPDF (fitz) 提取文本"""

import logging

logger = logging.getLogger(__name__)


class PdfLoader:

    @staticmethod
    def supports(extension: str) -> bool:
        return extension.lower() in ("pdf",)

    def load(self, file_path: str) -> str:
        import fitz

        logger.info("[PdfLoader] 开始解析: %s", file_path)
        doc = fitz.open(file_path)
        pages = []
        for i in range(len(doc)):
            text = doc[i].get_text("text")
            pages.append(text or "")
        doc.close()
        text = "\n\n".join(pages)
        logger.info("[PdfLoader] 解析完成, 页数=%d, 长度=%d", len(pages), len(text))
        return text