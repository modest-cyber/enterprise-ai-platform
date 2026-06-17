"""
LangGraph 工作流定义

定义 Agent 编排的核心工作流：
- Planner -> 子 Agent 路由
- Code -> Review 串联
- RAG 检索增强流程
"""

from typing import Annotated, TypedDict
from langgraph.graph import StateGraph, END
from agents.planner import PlannerAgent
from agents.rag_agent import RAGAgent
from agents.code_agent import CodeAgent
from agents.review_agent import ReviewAgent


class AgentState(TypedDict):
    user_input: str
    session_id: str
    agent: str
    output: str
    next_agent: str


def build_planner_graph() -> StateGraph:
    """构建 Planner 路由工作流"""

    async def planner_node(state: AgentState) -> AgentState:
        planner = PlannerAgent()
        result = await planner.route(state["user_input"], state["session_id"])
        state["output"] = result["content"]
        state["agent"] = result["agent"]
        state["next_agent"] = END
        return state

    graph = StateGraph(AgentState)
    graph.add_node("planner", planner_node)  # type: ignore[arg-type]
    graph.set_entry_point("planner")
    graph.add_edge("planner", END)

    return graph.compile()


def build_code_review_graph() -> StateGraph:
    """构建 Code -> Review 串联工作流：
    代码生成后用 Review Agent 自动审查
    """

    async def code_node(state: AgentState) -> AgentState:
        agent = CodeAgent()
        state["output"] = await agent.generate(state["user_input"])
        state["next_agent"] = "review"
        return state

    async def review_node(state: AgentState) -> AgentState:
        agent = ReviewAgent()
        review_result = await agent.review(state["output"])
        state["output"] = (
            f"## 生成代码\n\n{state['output']}\n\n---\n\n"
            f"{review_result}"
        )
        state["agent"] = "code+review"
        state["next_agent"] = END
        return state

    graph = StateGraph(AgentState)
    graph.add_node("code", code_node)  # type: ignore[arg-type]
    graph.add_node("review", review_node)  # type: ignore[arg-type]
    graph.set_entry_point("code")
    graph.add_edge("code", "review")
    graph.add_edge("review", END)

    return graph.compile()


def build_rag_graph() -> StateGraph:
    """构建 RAG 增强检索工作流：
    检索 -> 重排序 -> 生成回答
    """

    async def retrieve_node(state: AgentState) -> AgentState:
        from rag.retriever import Retriever
        retriever = Retriever()
        docs = await retriever.search(state["user_input"], top_k=10)
        state["output"] = "\n".join(docs)
        state["next_agent"] = "rerank"
        return state

    async def rerank_node(state: AgentState) -> AgentState:
        from rag.reranker import Reranker
        reranker = Reranker()
        best_docs = await reranker.rerank(
            state["user_input"], state["output"].split("\n"), top_k=5
        )
        state["output"] = "\n\n".join(best_docs)
        state["next_agent"] = "generate"
        return state

    async def generate_node(state: AgentState) -> AgentState:
        agent = RAGAgent()
        state["output"] = await agent.query(state["user_input"])
        state["agent"] = "rag"
        state["next_agent"] = END
        return state

    graph = StateGraph(AgentState)
    graph.add_node("retrieve", retrieve_node)  # type: ignore[arg-type]
    graph.add_node("rerank", rerank_node)  # type: ignore[arg-type]
    graph.add_node("generate", generate_node)  # type: ignore[arg-type]
    graph.set_entry_point("retrieve")
    graph.add_edge("retrieve", "rerank")
    graph.add_edge("rerank", "generate")
    graph.add_edge("generate", END)

    return graph.compile()
