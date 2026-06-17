from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app import settings


REVIEW_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是代码审查 Agent。对代码进行全面审查，输出结构化审查报告。

审查维度：
1. 安全性 - 注入攻击、敏感信息泄露、权限控制
2. 性能 - 算法效率、数据库查询优化、并发问题
3. 可维护性 - 命名规范、代码结构、注释完整性
4. 最佳实践 - 框架使用是否正确、设计模式是否恰当

输出格式：
### 审查报告
**严重程度：** 🔴高危 / 🟡中危 / 🟢低危

**问题列表：**
1. [严重程度] 问题描述 — 建议修复方案
2. ...

**总体评分：** X/10"""),
    ("human", "请审查以下代码：\n\n{code}"),
])


class ReviewAgent:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.openai_model,
            api_key=settings.openai_api_key,  # type: ignore[arg-type]
            temperature=0.1,
        )

    async def review(self, code: str) -> str:
        chain = REVIEW_PROMPT | self.llm
        response = await chain.ainvoke({"code": code})
        return response.content  # type: ignore[return-value]
