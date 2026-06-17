from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app import settings


CODE_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是代码生成 Agent。根据用户需求生成高质量、可维护的代码。

要求：
- 代码包含必要的注释
- 遵循业界最佳实践
- 考虑边界情况和错误处理
- 输出时使用 Markdown 代码块标注语言类型

你可以使用以下工具辅助开发：
- GitHub 仓库操作
- MySQL 数据库查询
- 文件系统操作"""),
    ("human", "{input}"),
])


class CodeAgent:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.openai_model,
            api_key=settings.openai_api_key,  # type: ignore[arg-type]
            temperature=0.1,
        )

    async def generate(self, task: str) -> str:
        chain = CODE_PROMPT | self.llm
        response = await chain.ainvoke({"input": task})
        return response.content  # type: ignore[return-value]
