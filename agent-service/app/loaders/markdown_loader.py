"""Markdown 加载器 — 提取标题、表格、列表、代码块的纯文本内容"""

import logging
import re

logger = logging.getLogger(__name__)


class MarkdownLoader:
    """解析 Markdown，保留结构化信息同时提取纯文本"""

    @staticmethod
    def supports(extension: str) -> bool:
        return extension.lower() in ("md", "markdown", "mdown", "mkd", "mkdn")

    def load(self, file_path: str) -> str:
        logger.info("[MarkdownLoader] 开始解析: %s", file_path)

        with open(file_path, "r", encoding="utf-8") as f:
            raw = f.read()

        raw = self._strip_fences(raw)
        raw = self._strip_images(raw)
        raw = self._strip_links(raw)
        raw = self._strip_formatting(raw)
        raw = self._strip_html(raw)

        lines = raw.split("\n")
        cleaned = []
        for line in lines:
            stripped = line.strip()
            if not stripped:
                cleaned.append("")
            elif stripped.startswith("#"):
                cleaned.append(stripped.lstrip("#").strip())
            elif stripped.startswith("- ") or stripped.startswith("* "):
                cleaned.append(stripped[2:].strip())
            elif re.match(r"^\d+\.\s", stripped):
                cleaned.append(re.sub(r"^\d+\.\s", "", stripped).strip())
            else:
                cleaned.append(stripped)

        text = "\n".join(cleaned)
        logger.info("[MarkdownLoader] 解析完成, 长度=%d", len(text))
        return text

    def _strip_fences(self, text: str) -> str:
        return re.sub(r"```[\s\S]*?```", "", text)

    def _strip_images(self, text: str) -> str:
        return re.sub(r"!\[.*?\]\(.*?\)", "", text)

    def _strip_links(self, text: str) -> str:
        return re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", text)

    def _strip_formatting(self, text: str) -> str:
        text = re.sub(r"\*\*(.+?)\*\*", r"\1", text)
        text = re.sub(r"__(.+?)__", r"\1", text)
        text = re.sub(r"\*(.+?)\*", r"\1", text)
        text = re.sub(r"_(.+?)_", r"\1", text)
        text = re.sub(r"~~(.+?)~~", r"\1", text)
        text = re.sub(r"`([^`]+)`", r"\1", text)
        return text

    def _strip_html(self, text: str) -> str:
        return re.sub(r"<[^>]+>", "", text)