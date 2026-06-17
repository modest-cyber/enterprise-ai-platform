"""文件系统操作工具"""

import os
import aiofiles


class FileTool:
    def __init__(self, base_dir: str = "."):
        self.base_dir = os.path.abspath(base_dir)

    def _safe_path(self, path: str) -> str:
        """防止路径穿越攻击"""
        full = os.path.abspath(os.path.join(self.base_dir, path))
        if not full.startswith(self.base_dir):
            raise ValueError(f"非法路径: {path}")
        return full

    async def read_file(self, path: str) -> str:
        """读取文件内容"""
        safe = self._safe_path(path)
        async with aiofiles.open(safe, "r", encoding="utf-8") as f:
            return await f.read()

    async def write_file(self, path: str, content: str) -> None:
        """写入文件"""
        safe = self._safe_path(path)
        os.makedirs(os.path.dirname(safe), exist_ok=True)
        async with aiofiles.open(safe, "w", encoding="utf-8") as f:
            await f.write(content)

    async def list_dir(self, path: str = ".") -> list[dict]:
        """列出目录内容"""
        safe = self._safe_path(path)
        items = []
        for entry in os.scandir(safe):
            items.append({
                "name": entry.name,
                "type": "dir" if entry.is_dir() else "file",
                "size": entry.stat().st_size if entry.is_file() else 0,
            })
        return items

    async def delete_file(self, path: str) -> None:
        """删除文件"""
        safe = self._safe_path(path)
        if os.path.isfile(safe):
            os.remove(safe)
