from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from app import settings


RAG_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是知识库问答 Agent。基于检索到的上下文回答用户问题。

上下文信息：
{context}

如果上下文中没有答案，请诚实地说"未找到相关信息"，不要编造。"""),
    ("human", "{question}"),
])


class RAGAgent:
    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.openai_model,
            api_key=settings.openai_api_key,  # type: ignore[arg-type]
            temperature=0.3,
        )

    async def query(self, question: str) -> str:
        from rag.retriever import Retriever

        retriever = Retriever()
        docs = await retriever.search(question, top_k=5)
        context = "\n\n".join(docs) if docs else "暂无相关上下文"

        chain = RAG_PROMPT | self.llm
        response = await chain.ainvoke({
            "context": context,
            "question": question,
        })
        return response.content  # type: ignore[return-value]
