"""Chunk Service — 文本分块，保持语义连续"""

import logging

logger = logging.getLogger(__name__)

DEFAULT_CHUNK_SIZE = 800
DEFAULT_CHUNK_OVERLAP = 100


def split_text(text: str, chunk_size: int = DEFAULT_CHUNK_SIZE, chunk_overlap: int = DEFAULT_CHUNK_OVERLAP) -> list[str]:
    if not text or not text.strip():
        return []

    text = text.strip()

    paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
    if not paragraphs:
        paragraphs = [text]

    chunks = []
    for para in paragraphs:
        if len(para) <= chunk_size:
            chunks.append(para)
        else:
            sentences = _split_sentences(para)
            chunks.extend(_merge_sentences(sentences, chunk_size, chunk_overlap))

    logger.info("文本分块完成, 原文长度=%d, 段落数=%d, 块数=%d", len(text), len(paragraphs), len(chunks))
    return chunks


def _split_sentences(text: str) -> list[str]:
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
    chunks = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        chunks.append(text[start:end])
        start += chunk_size - chunk_overlap
    return chunks