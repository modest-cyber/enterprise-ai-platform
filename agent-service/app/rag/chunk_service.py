"""Chunk Service — 文本分块，短段落合并，保持语义连续"""

import logging

logger = logging.getLogger(__name__)

DEFAULT_CHUNK_SIZE = 800
DEFAULT_CHUNK_OVERLAP = 100


def split_text(text: str, chunk_size: int = DEFAULT_CHUNK_SIZE, chunk_overlap: int = DEFAULT_CHUNK_OVERLAP) -> list[str]:
    """按段落切分后贪婪合并，短段落不再独立成块，chunkSize 真正生效"""
    if not text or not text.strip():
        return []

    text = text.strip()

    paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
    if not paragraphs:
        paragraphs = [text]

    chunks = []
    current = ""

    for para in paragraphs:
        # 单段落超过 chunkSize：先结块，再对长段落做句子级切分
        if len(para) > chunk_size:
            if current:
                chunks.append(current)
                current = ""
            sentences = _split_sentences(para)
            chunks.extend(_merge_sentences(sentences, chunk_size, chunk_overlap))
            continue

        # 尝试将 para 合并到 current
        if not current:
            current = para
        elif len(current) + 2 + len(para) <= chunk_size:
            current += "\n\n" + para
        else:
            # current 已满，结块并开启新块（带 overlap）
            chunks.append(current)
            if chunk_overlap > 0 and len(current) > chunk_overlap:
                current = current[-chunk_overlap:] + "\n\n" + para
            else:
                current = para

    if current:
        chunks.append(current)

    logger.info("文本分块完成, chunkSize=%d, chunkOverlap=%d, 原文长度=%d, 段落数=%d, 块数=%d",
                chunk_size, chunk_overlap, len(text), len(paragraphs), len(chunks))
    return chunks


def _split_sentences(text: str) -> list[str]:
    """中文/英文句子切分"""
    delimiters = "。！？!?\n"
    sentences = []
    current = ""

    for ch in text:
        current += ch
        if ch in delimiters:
            sent = current.strip()
            if sent:
                sentences.append(sent)
            current = ""

    if current.strip():
        sentences.append(current.strip())

    return sentences if sentences else [text]


def _merge_sentences(sentences: list[str], chunk_size: int, chunk_overlap: int) -> list[str]:
    """句子级贪婪合并"""
    chunks = []
    i = 0

    while i < len(sentences):
        chunk = sentences[i]
        while i + 1 < len(sentences) and len(chunk) + len(sentences[i + 1]) <= chunk_size:
            i += 1
            chunk += sentences[i]

        if len(chunk) > chunk_size * 1.5:
            chunks.extend(_force_split_long_chunk(chunk, chunk_size, chunk_overlap))
        else:
            chunks.append(chunk)

        i += 1

    return chunks


def _force_split_long_chunk(text: str, chunk_size: int, chunk_overlap: int) -> list[str]:
    """兜底：按字符位置强制切分"""
    chunks = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        chunks.append(text[start:end])
        start += chunk_size - chunk_overlap
    return chunks
