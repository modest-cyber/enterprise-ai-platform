"""TXT 加载器 — 自动检测编码 (UTF-8 / GBK / GB2312)"""

import logging

logger = logging.getLogger(__name__)


class TxtLoader:
    """读取纯文本文件，自动检测编码"""

    @staticmethod
    def supports(extension: str) -> bool:
        return extension.lower() in ("txt", "log", "csv", "text")

    def load(self, file_path: str) -> str:
        logger.info("[TxtLoader] 开始解析: %s", file_path)

        encodings_to_try = ["utf-8", "gbk", "gb2312", "gb18030", "latin-1"]

        for encoding in encodings_to_try:
            try:
                with open(file_path, "r", encoding=encoding) as f:
                    text = f.read()
                logger.info("[TxtLoader] 检测编码=%s, 长度=%d", encoding, len(text))
                return text
            except UnicodeDecodeError:
                continue

        with open(file_path, "rb") as f:
            raw = f.read()
        try:
            import chardet
            result = chardet.detect(raw)
            detected = result.get("encoding") or "utf-8"
            logger.info("[TxtLoader] chardet 检测=%s, 置信度=%.2f", detected, result.get("confidence", 0))
            return raw.decode(detected, errors="replace")
        except ImportError:
            logger.warning("[TxtLoader] chardet 未安装, 使用 latin-1 兜底")
            return raw.decode("latin-1", errors="replace")