from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from app import settings


ROUTE_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是一个路由规划器。根据用户意图，将请求分发给对应的 Agent：

- **rag**: 知识库问答、文档检索、RAG 查询
- **code**: 代码生成、代码解释、技术问题解答
- **review**: 代码审查、安全审查

请从用户的输入中识别意图，并以 JSON 格式输出：
{{"agent": "<agent_name>", "reason": "<简短理由>"}}

如果无法确定意图，返回 agent 为 "unknown"。"""),
    ("human", "{input}"),
])


class PlannerAgent:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.openai_model,
            api_key=settings.openai_api_key,  # type: ignore[arg-type]
            temperature=0,
        )

    async def route(self, user_input: str, session_id: str) -> dict:
        """分析用户意图并路由到对应的子 Agent"""
        chain = ROUTE_PROMPT | self.llm
        response = await chain.ainvoke({"input": user_input})

        import json
        try:
            decision = json.loads(response.content)  # type: ignore[arg-type]
        except json.JSONDecodeError:
            decision = {"agent": "unknown", "reason": "无法解析意图"}

        agent_map = {
            "rag": self._run_rag,
            "code": self._run_code,
            "review": self._run_review,
        }

        agent_name = decision.get("agent", "unknown")
        handler = agent_map.get(agent_name)

        if handler:
            content = await handler(user_input)
            return {"content": content, "agent": agent_name}

        return {"content": f"无法识别意图: {decision.get('reason')}", "agent": "planner"}

    async def _run_rag(self, user_input: str) -> str:
        from agents.rag_agent import RAGAgent
        agent = RAGAgent()
        return await agent.query(user_input)

    async def _run_code(self, user_input: str) -> str:
        from agents.code_agent import CodeAgent
        agent = CodeAgent()
        return await agent.generate(user_input)

    async def _run_review(self, user_input: str) -> str:
        from agents.review_agent import ReviewAgent
        agent = ReviewAgent()
        return await agent.review(user_input)
