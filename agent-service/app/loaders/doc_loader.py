"""DOC 加载器 — 兼容旧版 Word (.doc) 文件"""

import logging
import os
import subprocess
import tempfile
from pathlib import Path

logger = logging.getLogger(__name__)


class DocLoader:
    """读取 .doc 文件 — 多策略尝试: textract → python-docx → antiword → LibreOffice"""

    @staticmethod
    def supports(extension: str) -> bool:
        return extension.lower() in ("doc",)

    def load(self, file_path: str) -> str:
        logger.info("[DocLoader] 开始解析: %s", file_path)

        try:
            return self._try_textract(file_path)
        except Exception as e:
            logger.warning("[DocLoader] textract 失败: %s，尝试备选", e)

        try:
            return self._try_docx_fallback(file_path)
        except Exception as e:
            logger.warning("[DocLoader] docx fallback 失败: %s", e)

        try:
            return self._try_antiword(file_path)
        except Exception as e:
            logger.warning("[DocLoader] antiword 失败: %s", e)

        try:
            return self._try_libreoffice(file_path)
        except Exception as e:
            logger.error("[DocLoader] 所有方案失败: %s", e)
            raise ValueError(f".doc 文件解析失败 - 请安装 textract (pip install textract) 或将文件转为 .docx 格式后再上传") from e

    def _try_textract(self, file_path: str) -> str:
        import textract
        text = textract.process(file_path).decode("utf-8", errors="replace")
        logger.info("[DocLoader] textract 完成, 长度=%d", len(text))
        return text

    def _try_docx_fallback(self, file_path: str) -> str:
        try:
            from docx import Document
            doc = Document(file_path)
            text = "\n".join(p.text for p in doc.paragraphs)
            if text.strip():
                logger.info("[DocLoader] python-docx 完成, 长度=%d", len(text))
                return text
        except Exception:
            pass
        raise RuntimeError("python-docx 无法解析")

    def _try_antiword(self, file_path: str) -> str:
        result = subprocess.run(
            ["antiword", file_path],
            capture_output=True, text=True, timeout=30,
        )
        if result.returncode != 0:
            raise RuntimeError(f"antiword 失败: {result.stderr}")
        logger.info("[DocLoader] antiword 完成, 长度=%d", len(result.stdout))
        return result.stdout

    def _try_libreoffice(self, file_path: str) -> str:
        logger.info("[DocLoader] 尝试 LibreOffice 转换...")
        with tempfile.TemporaryDirectory() as tmpdir:
            subprocess.run([
                "libreoffice", "--headless", "--convert-to", "txt",
                "--outdir", tmpdir, file_path,
            ], check=True, timeout=60, capture_output=True)
            txt_files = list(Path(tmpdir).glob("*.txt"))
            if not txt_files:
                raise RuntimeError("LibreOffice 未生成 txt 文件")
            text = txt_files[0].read_text(encoding="utf-8", errors="replace")
            logger.info("[DocLoader] LibreOffice 完成, 长度=%d", len(text))
            return text