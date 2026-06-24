"""DOCX 加载器 — 提取段落、标题、表格、页眉、页脚"""

import logging

logger = logging.getLogger(__name__)


class DocxLoader:
    """读取 .docx 全部文本内容"""

    @staticmethod
    def supports(extension: str) -> bool:
        return extension.lower() in ("docx",)

    def load(self, file_path: str) -> str:
        from docx import Document

        logger.info("[DocxLoader] 开始解析: %s", file_path)
        doc = Document(file_path)
        parts = []

        headers_text = self._extract_headers(doc)
        if headers_text.strip():
            parts.append(headers_text)
            parts.append("")

        for para in doc.paragraphs:
            text = para.text.strip()
            if not text:
                parts.append("")
                continue

            heading_level = self._get_heading_level(para)
            if heading_level > 0:
                prefix = "#" * min(heading_level, 3)
                parts.append(f"{prefix} {text}")
            else:
                parts.append(text)

        for table in doc.tables:
            parts.append("")
            for row in table.rows:
                cells = [cell.text.strip() for cell in row.cells]
                parts.append(" | ".join(cells))
            parts.append("")

        footers_text = self._extract_footers(doc)
        if footers_text.strip():
            parts.append("")
            parts.append(footers_text)

        text = "\n".join(parts)
        logger.info("[DocxLoader] 解析完成, 段落数=%d, 表格数=%d, 长度=%d",
                     len(doc.paragraphs), len(doc.tables), len(text))
        return text

    def _get_heading_level(self, para) -> int:
        try:
            style = para.style
            if style is None:
                return 0
            name = style.name
            if name is None:
                return 0
            if name.startswith("Heading") or name.startswith("heading"):
                level_str = name.replace("Heading", "").replace("heading", "").strip()
                if level_str.isdigit():
                    return int(level_str)
                return 1
            if name.startswith("Title") or name.startswith("Subtitle"):
                return 1
        except Exception:
            pass
        return 0

    def _extract_headers(self, doc) -> str:
        parts = []
        for section in doc.sections:
            try:
                header = section.header
                if header and not header.is_linked_to_previous:
                    for para in header.paragraphs:
                        text = para.text.strip()
                        if text:
                            parts.append("[页眉] " + text)
            except Exception:
                pass
        return "\n".join(parts) if parts else ""

    def _extract_footers(self, doc) -> str:
        parts = []
        for section in doc.sections:
            try:
                footer = section.footer
                if footer and not footer.is_linked_to_previous:
                    for para in footer.paragraphs:
                        text = para.text.strip()
                        if text:
                            parts.append("[页脚] " + text)
            except Exception:
                pass
        return "\n".join(parts) if parts else ""