"""Seed ai_model and agent_config tables"""
import pymysql

conn = pymysql.connect(
    host="49.232.215.44", port=3306, user="root",
    password="MySqL@2026#Ryv!XpG9q", database="ry-vue", charset="utf8mb4"
)
cursor = conn.cursor()

# ==================== ai_model ====================
models = [
    ("deepseek-chat", "DeepSeek-V3", "deepseek",
     "", "https://api.deepseek.com/v1",
     "chat", 8192, 0.70, 1, 1, "DeepSeek V3 旗舰对话模型"),
    ("deepseek-reasoner", "DeepSeek-R1", "deepseek",
     "", "https://api.deepseek.com/v1",
     "chat", 8192, 0.60, 0, 1, "DeepSeek R1 推理模型"),
    ("qwen3", "Qwen3 (Ollama)", "ollama",
     "", "http://localhost:11434/v1",
     "chat", 4096, 0.70, 0, 1, "Ollama 本地 Qwen3 模型"),
    ("gpt-4o", "GPT-4o", "openai",
     "", "https://api.openai.com/v1",
     "chat", 16384, 0.70, 0, 1, "OpenAI GPT-4o 多模态模型"),
    ("nomic-embed-text", "Nomic Embed (Ollama)", "ollama",
     "", "http://localhost:11434/api",
     "embedding", 512, 0.0, 0, 1, "Ollama 本地 Embedding 模型"),
]

sql_m = """INSERT INTO ai_model (model_name, display_name, provider, api_key, base_url,
    model_type, max_tokens, temperature, is_default, is_enabled, create_by, create_time, remark)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 'admin', NOW(), %s)"""

for m in models:
    cursor.execute(sql_m, m)
    print(f"MODEL OK: {m[0]}")

# ==================== agent_config ====================
planner_prompt = """你是企业AI中台的总调度Agent。你的职责是：
1. 分析用户意图和任务复杂度
2. 将复杂任务拆解为子任务
3. 根据子任务类型，路由到对应的Agent (rag/code/review/tool)
4. 汇总各子Agent的结果，生成最终回复

路由规则：
- 知识库相关 -> rag agent
- 代码生成/分析 -> code agent
- 代码审查 -> review agent
- 工具调用(文件/HTTP/MySQL/GitHub/网页/Python) -> tool agent
- 简单对话 -> 直接回复"""

rag_prompt = """你是知识库检索Agent。基于用户提供的文档内容回答问题。

要求：
1. 回答必须严格基于文档内容，不得编造
2. 引用文档来源（文档名+片段）
3. 如果文档内容不足以回答，请明确告知用户
4. 使用Markdown格式组织回答"""

code_prompt = """你是一位资深软件工程师。你可以：
1. 根据需求生成代码（Java/TypeScript/Python/SQL等）
2. 分析代码逻辑和潜在问题
3. 解释代码原理
4. 提供重构建议

回复格式：
- 先简要说明思路
- 代码使用```语言标记包裹
- 补充关键注意事项"""

review_prompt = """你是代码审查专家。审查代码时需要关注：
1. 安全漏洞（SQL注入、XSS、CSRF、命令注入等）
2. 代码规范和最佳实践
3. 性能和资源消耗
4. 错误处理和边界条件
5. 可维护性和可读性

输出格式：
## 总体评价
## 严重问题（必须修复）
## 改进建议
## 亮点"""

tool_prompt = """你是工具调用专家。你可以调用以下MCP工具：
- 文件系统操作（文件读取/写入/列表/删除）
- HTTP API 请求
- MySQL 数据库查询
- GitHub 仓库操作
- 网页抓取与内容提取
- Python 代码沙箱执行
- 企业知识库语义检索

执行原则：
1. 理解用户意图，选择合适的工具
2. 将用户输入转为工具参数
3. 解释工具返回的结果"""

custom_prompt = """你是企业AI助手。你的回答应该：
1. 专业、准确、简洁
2. 使用中文回复
3. 必要时使用Markdown格式
4. 不确定时诚实告知"""

agents = [
    ("planner", "planner",
     "总调度 Agent - 分析用户意图，拆解任务并分发给子 Agent",
     None, planner_prompt, "[]", "{}", 10, 0.70, 300, 1,
     "Planner调度Agent"),

    ("rag", "rag",
     "知识库检索 Agent - 基于RAG架构的文档问答",
     None, rag_prompt, "[]", "{}", 5, 0.50, 300, 1,
     "RAG知识库检索Agent"),

    ("code", "code",
     "代码 Agent - 生成、分析、解释代码",
     None, code_prompt, "[]", "{}", 8, 0.30, 300, 1,
     "代码生成/分析Agent"),

    ("review", "review",
     "代码审查 Agent - Review代码质量和安全性",
     None, review_prompt, "[]", "{}", 5, 0.30, 300, 1,
     "代码审查Agent"),

    ("tool", "tool",
     "工具调用 Agent - 执行MCP工具（文件/HTTP/MySQL/GitHub/网页/Python/知识库）",
     None, tool_prompt, "[1,2,3,4,5,6,7]", "{}", 5, 0.40, 600, 1,
     "MCP工具调用Agent"),

    ("custom", "custom",
     "通用自定义 Agent - 灵活配置的通用对话Agent",
     None, custom_prompt, "[]", "{}", 5, 0.70, 300, 1,
     "通用自定义Agent"),
]

sql_a = """INSERT INTO agent_config (agent_name, agent_type, description, model_id,
    system_prompt, tools_json, workflow_json, max_iterations, temperature,
    timeout_seconds, status, create_by, create_time, remark)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 'admin', NOW(), %s)"""

for a in agents:
    cursor.execute(sql_a, a)
    print(f"AGENT OK: {a[0]}")

conn.commit()

# Verify
print()
print("=== ai_model ===")
cursor.execute("SELECT model_id, model_name, display_name, provider, model_type, is_default, is_enabled FROM ai_model ORDER BY model_id")
for row in cursor.fetchall():
    print(f"  id={row[0]} | {row[1]:25s} | {row[2]:25s} | {row[3]:10s} | {row[4]:12s} | default={row[5]} | enabled={row[6]}")

print()
print("=== agent_config ===")
cursor.execute("SELECT agent_id, agent_name, agent_type, max_iterations, status FROM agent_config ORDER BY agent_id")
for row in cursor.fetchall():
    print(f"  id={row[0]} | {row[1]:12s} | {row[2]:10s} | max_iter={row[3]} | status={row[4]}")

conn.close()
print()
print("SEED DONE")
