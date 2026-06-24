"""DOCX 加载器 — 提取段落、标题、表格"""

import logging

logger = logging.getLogger(__name__)


class DocxLoader:
    """读取 .docx 全部文本内容，包括段落、标题和表格"""

    @staticmethod
    def supports(extension: str) -> bool:
        return extension.lower() in ("docx",)

    def load(self, file_path: str) -> str:
        from docx import Document
        from docx.enum.text import WD_PARAGRAPH_ALIGNMENT

        logger.info("[DocxLoader] 开始解析: %s", file_path)
        doc = Document(file_path)
        parts = []

        for para in doc.paragraphs:
            text = para.text.strip()
            if not text:
                parts.append("")
                continue

            style_name = para.style.name if para.style else ""
            if style_name.startswith("Heading"):
                prefix = "#" * min(int(style_name.replace("Heading ", "")) if " " in style_name else 1, 3)
                parts.append(f"{prefix} {text}")
            else:
                parts.append(text)

        for table in doc.tables:
            parts.append("")
            for row in table.rows:
                cells = [cell.text.strip() for cell in row.cells]
                parts.append(" | ".join(cells))
            parts.append("")

        text = "\n".join(parts)
        logger.info("[DocxLoader] 解析完成, 段落数=%d, 表格数=%d, 长度=%d",
                     len(doc.paragraphs), len(doc.tables), len(text))
        return text