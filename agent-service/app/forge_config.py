"""从平台 Agent/Model 配置构建 forge-agent 运行参数"""

from dataclasses import dataclass, field
from typing import Any


@dataclass
class ForgeConfig:
    """forge-agent 运行所需全部参数"""
    task: str
    repo_path: str = "."
    provider: str = "deepseek"
    model_name: str = "deepseek-chat"
    api_key: str = ""
    base_url: str = "https://api.deepseek.com"
    max_tokens: int = 8192
    max_steps: int = 40
    temperature: float = 0.7
    budget_tokens: int = 80000
    log_dir: str = "./logs/forge"

    @classmethod
    def from_platform_config(
        cls,
        task: str,
        agent_config: dict[str, Any],
        model_config: dict[str, Any],
        repo_path: str = ".",
    ) -> "ForgeConfig":
        """从平台 API 传入的 Agent + Model 配置构建 ForgeConfig

        agent_config 字段（来自 Spring Boot ConversationConfigDto.AgentInfo）:
            agentId, agentName, agentType, systemPrompt,
            maxIterations, temperature, timeoutSeconds, tools

        model_config 字段（来自 Spring Boot ConversationConfigDto.ModelInfo）:
            modelId, provider, modelName, baseUrl, apiKey, maxTokens, temperature
        """
        provider = model_config.get("provider", "deepseek")
        # provider → base_url 映射（与 forge-agent llm/router.py 保持一致）
        provider_base_urls = {
            "deepseek": "https://api.deepseek.com",
            "groq": "https://api.groq.com/openai/v1",
            "ollama": "http://localhost:11434/v1",
        }

        return cls(
            task=task,
            repo_path=repo_path,
            provider=provider,
            model_name=model_config.get("modelName", "deepseek-chat"),
            api_key=model_config.get("apiKey", ""),
            base_url=model_config.get("baseUrl") or provider_base_urls.get(provider, ""),
            max_tokens=model_config.get("maxTokens", 8192),
            max_steps=agent_config.get("maxIterations", 40),
            temperature=agent_config.get("temperature", 0.7),
            budget_tokens=80000,
            log_dir="./logs/forge",
        )
