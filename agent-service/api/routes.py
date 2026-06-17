from fastapi import APIRouter

from agents.planner import PlannerAgent
from agents.rag_agent import RAGAgent
from agents.code_agent import CodeAgent
from agents.review_agent import ReviewAgent
from pydantic import BaseModel, Field

router = APIRouter()


class ChatRequest(BaseModel):
    message: str = Field(..., description="用户输入消息")
    session_id: str = Field(default="default", description="会话 ID")


class ChatResponse(BaseModel):
    content: str
    agent: str
    session_id: str


@router.get("/health")
async def health_check():
    return {"status": "ok", "service": "agent-service"}


@router.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    """统一对话入口，由 Planner Agent 调度到子 Agent"""
    planner = PlannerAgent()
    result = await planner.route(req.message, req.session_id)
    return ChatResponse(
        content=result["content"],
        agent=result["agent"],
        session_id=req.session_id,
    )


@router.post("/chat/rag")
async def chat_rag(req: ChatRequest):
    agent = RAGAgent()
    return {"content": await agent.query(req.message), "agent": "rag"}


@router.post("/chat/code")
async def chat_code(req: ChatRequest):
    agent = CodeAgent()
    return {"content": await agent.generate(req.message), "agent": "code"}


@router.post("/chat/review")
async def chat_review(req: ChatRequest):
    agent = ReviewAgent()
    return {"content": await agent.review(req.message), "agent": "review"}
