"""MySQL 数据库操作工具"""

import os
import pymysql
from contextlib import contextmanager


class MySQLTool:
    def __init__(self):
        self.config = {
            "host": os.getenv("MYSQL_HOST", "localhost"),
            "port": int(os.getenv("MYSQL_PORT", "3306")),
            "user": os.getenv("MYSQL_USER", "root"),
            "password": os.getenv("MYSQL_PASSWORD", ""),
            "database": os.getenv("MYSQL_DATABASE", "enterprise_ai"),
            "charset": "utf8mb4",
        }

    @contextmanager
    def _connection(self):
        conn = pymysql.connect(**self.config)
        try:
            yield conn
        finally:
            conn.close()

    async def execute_query(self, sql: str, params: tuple | None = None) -> list[dict]:
        """执行查询并返回字典列表"""
        with self._connection() as conn:
            with conn.cursor(pymysql.cursors.DictCursor) as cursor:
                cursor.execute(sql, params)
                return cursor.fetchall()

    async def execute_update(self, sql: str, params: tuple | None = None) -> int:
        """执行增删改，返回影响行数"""
        with self._connection() as conn:
            with conn.cursor() as cursor:
                affected = cursor.execute(sql, params)
                conn.commit()
                return affected

    async def get_tables(self) -> list[str]:
        """获取数据库中所有表名"""
        rows = await self.execute_query("SHOW TABLES")
        return [list(row.values())[0] for row in rows]

    async def describe_table(self, table: str) -> list[dict]:
        """获取表结构"""
        return await self.execute_query(f"DESCRIBE `{table}`")
