-- ============================================
-- 企业AI知识中台 - AI 模块初始化 SQL
-- ============================================

-- ----------------------------
-- 1、AI 会话表
-- ----------------------------
drop table if exists ai_conversation;
create table ai_conversation (
  conversation_id   bigint(20)      not null auto_increment    comment '会话主键',
  user_id           bigint(20)      not null                   comment '用户ID（关联 sys_user.user_id）',
  title             varchar(200)    default ''                 comment '会话标题（自动截取首条消息）',
  agent_type        varchar(50)     default 'planner'          comment 'Agent类型（planner/rag/code/review）',
  status            tinyint(1)      default 1                  comment '状态：1-进行中，0-已归档',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (conversation_id),
  index idx_ai_conv_user_id (user_id),
  index idx_ai_conv_create_time (create_time)
) engine=innodb auto_increment=100 comment = 'AI会话表';


-- ----------------------------
-- 2、AI 消息表
-- ----------------------------
drop table if exists ai_message;
create table ai_message (
  message_id        bigint(20)      not null auto_increment    comment '消息主键',
  conversation_id   bigint(20)      not null                   comment '会话ID（关联 ai_conversation.conversation_id）',
  role              varchar(20)     not null                   comment '角色：user/assistant/system/tool',
  content           longtext        not null                   comment '消息内容（支持Markdown和代码块）',
  token_count       int(11)         default 0                  comment 'Token消耗数量',
  model_name        varchar(100)    default ''                 comment '使用的模型名称',
  source_type       varchar(30)     default 'llm'              comment '消息来源：llm/rag/tool/agent',
  metadata_json     json            default null               comment '元数据（引用的文档ID、工具调用结果等）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (message_id),
  index idx_ai_msg_conv_id (conversation_id),
  index idx_ai_msg_create_time (create_time)
) engine=innodb auto_increment=100 comment = 'AI消息表';


-- ----------------------------
-- 3、知识库条目表
-- ----------------------------
drop table if exists kb_knowledge;
create table kb_knowledge (
  kb_id             bigint(20)      not null auto_increment    comment '知识库主键',
  name              varchar(200)    not null                   comment '知识库名称',
  description       varchar(500)    default ''                 comment '知识库描述',
  kb_type           varchar(50)     default 'general'          comment '知识库类型：general/code/doc/faq',
  embedding_model   varchar(100)    default 'text-embedding-3-small' comment 'Embedding模型',
  chunk_size        int(11)         default 512                comment '文档切分块大小',
  chunk_overlap     int(11)         default 50                 comment '文档切分重叠大小',
  status            tinyint(1)      default 1                  comment '状态：1-启用，0-停用',
  doc_count         int(11)         default 0                  comment '文档数量',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (kb_id),
  index idx_kb_type (kb_type),
  index idx_kb_status (status)
) engine=innodb auto_increment=100 comment = '知识库条目表';


-- ----------------------------
-- 4、知识文档表
-- ----------------------------
drop table if exists kb_document;
create table kb_document (
  doc_id            bigint(20)      not null auto_increment    comment '文档主键',
  kb_id             bigint(20)      not null                   comment '知识库ID（关联 kb_knowledge.kb_id）',
  file_name         varchar(300)    not null                   comment '文件名',
  file_type         varchar(20)     default 'txt'              comment '文件类型：txt/pdf/md/docx/xlsx/html',
  file_size         bigint(20)      default 0                  comment '文件大小（字节）',
  file_path         varchar(500)    default ''                 comment '文件存储路径',
  content_text      longtext        default null               comment '原始文本内容',
  chunk_count       int(11)         default 0                  comment '切分块数',
  vector_count      int(11)         default 0                  comment '向量数',
  vector_ids        json            default null               comment 'Milvus向量ID列表',
  status            tinyint(1)      default 0                  comment '状态：0-待处理，1-处理中，2-已完成，3-失败',
  process_status    varchar(20)     default 'PENDING'          comment '处理状态：PENDING/PROCESSING/SUCCESS/FAILED',
  process_progress  int(11)         default 0                  comment '处理进度 0-100',
  process_message   varchar(500)    default ''                 comment '当前处理步骤描述',
  processed_time    datetime        default null               comment '处理完成时间',
  error_msg         varchar(500)    default ''                 comment '处理失败原因',
  is_delete         tinyint(1)      default 0                  comment '逻辑删除：0-正常，1-已删除',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (doc_id),
  index idx_kb_doc_kb_id (kb_id),
  index idx_kb_doc_status (status),
  index idx_kb_doc_file_type (file_type)
) engine=innodb auto_increment=100 comment = '知识文档表';


-- ----------------------------
-- 5、Agent 配置表
-- ----------------------------
drop table if exists agent_config;
create table agent_config (
  agent_id          bigint(20)      not null auto_increment    comment 'Agent主键',
  agent_name        varchar(100)    not null                   comment 'Agent名称',
  agent_type        varchar(50)     not null                   comment 'Agent类型：planner/rag/code/review/tool/custom',
  description       varchar(500)    default ''                 comment 'Agent描述',
  system_prompt     text            default null               comment 'System Prompt模板',
  tools_json        json            default null               comment '绑定的MCP工具ID列表',
  workflow_json     json            default null               comment 'LangGraph工作流配置JSON',
  max_iterations    int(11)         default 5                  comment '最大迭代次数',
  temperature       decimal(3,2)    default 0.70               comment '温度参数',
  timeout_seconds   int(11)         default 300                comment '超时时间（秒）',
  status            tinyint(1)      default 1                  comment '状态：1-启用，0-停用',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (agent_id),
  unique index idx_agent_name (agent_name),
  index idx_agent_type (agent_type),
  index idx_agent_status (status)
) engine=innodb auto_increment=100 comment = 'Agent配置表';


-- ----------------------------
-- 6、AI 模型配置表
-- ----------------------------
drop table if exists ai_model;
create table ai_model (
  model_id          bigint(20)      not null auto_increment    comment '模型主键',
  model_name        varchar(100)    not null                   comment '模型名称（如deepseek-chat）',
  display_name      varchar(200)    not null                   comment '展示名称（如DeepSeek-V3）',
  provider          varchar(50)     not null                   comment 'Provider：openai/deepseek/qwen/ollama',
  api_key           varchar(500)    not null                   comment 'API Key（加密存储）',
  base_url          varchar(300)    not null                   comment 'API Base URL',
  model_type        varchar(30)     default 'chat'             comment '模型类型：chat/embedding/rerank',
  max_tokens        int(11)         default 4096               comment '最大输出Token',
  temperature       decimal(3,2)    default 0.70               comment '默认温度',
  is_default        tinyint(1)      default 0                  comment '是否默认模型：0-否，1-是',
  is_enabled        tinyint(1)      default 1                  comment '是否启用：1-是，0-否',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (model_id),
  unique index idx_ai_model_name (model_name),
  index idx_ai_model_provider (provider),
  index idx_ai_model_type (model_type),
  index idx_ai_model_enabled (is_enabled)
) engine=innodb auto_increment=100 comment = 'AI模型配置表';


-- ----------------------------
-- 7、MCP 工具注册表
-- ----------------------------
drop table if exists ai_tool;
create table ai_tool (
  tool_id           bigint(20)      not null auto_increment    comment '工具主键',
  tool_name         varchar(100)    not null                   comment '工具名称（唯一标识）',
  display_name      varchar(200)    not null                   comment '展示名称',
  tool_type         varchar(30)     not null                   comment '工具类型：mcp_server/http_api/function',
  description       varchar(500)    default ''                 comment '工具描述（用于Agent推理选择工具）',
  server_url        varchar(500)    default ''                 comment 'MCP Server URL或HTTP API URL',
  input_schema      json            not null                   comment '输入参数JSON Schema',
  output_schema     json            default null               comment '输出参数JSON Schema',
  auth_type         varchar(20)     default 'none'             comment '认证方式：none/api_key/oauth2',
  auth_config       json            default null               comment '认证配置（API Key/OAuth信息，加密）',
  timeout_ms        int(11)         default 30000              comment '超时时间（毫秒）',
  retry_count       int(11)         default 2                  comment '重试次数',
  is_enabled        tinyint(1)      default 1                  comment '是否启用：1-是，0-否',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (tool_id),
  unique index idx_ai_tool_name (tool_name),
  index idx_ai_tool_type (tool_type),
  index idx_ai_tool_enabled (is_enabled)
) engine=innodb auto_increment=100 comment = 'MCP工具注册表';


-- ----------------------------
-- 工具模板表
-- ----------------------------
drop table if exists ai_tool_template;
create table ai_tool_template (
  template_id       bigint(20)      not null auto_increment    comment '模板主键',
  template_code     varchar(50)     not null                   comment '模板标识（唯一）',
  template_name     varchar(200)    not null                   comment '模板名称',
  category          varchar(50)     not null                   comment '工具分类：file/network/database/search/code/enterprise',
  description       varchar(500)    default ''                 comment '模板描述',
  tool_type         varchar(30)     not null                   comment '工具类型',
  display_name      varchar(200)    default ''                 comment '建议显示名称',
  server_url        varchar(500)    default ''                 comment '默认服务URL',
  input_schema      json            default null               comment '输入参数JSON Schema',
  output_schema     json            default null               comment '输出参数JSON Schema',
  auth_type         varchar(20)     default 'none'             comment '认证方式',
  auth_config       json            default null               comment '默认认证配置',
  status            tinyint(1)      default 1                  comment '状态：1-启用，0-停用',
  create_time       datetime                                   comment '创建时间',
  update_time       datetime                                   comment '更新时间',
  primary key (template_id),
  unique index idx_tpl_code (template_code),
  index idx_tpl_category (category)
) engine=innodb auto_increment=100 comment = '工具模板表';

-- 工具模板种子数据
-- tool_type: mcp_server=Python Agent服务MCP协议工具, http_api=外部HTTP API
-- server_url: 指向 Python Agent 服务 (http://localhost:8000/api/v1/tools)
insert into ai_tool_template (template_code, template_name, category, description, tool_type, display_name, server_url, input_schema, output_schema, auth_type, auth_config, status, create_time) values
('file_reader', '文件读取工具', 'file',
 '读取本地文件内容，支持文本文件读写、目录列举、文件删除，内置路径穿越防护',
 'mcp_server', '文件读取',
 'http://localhost:8000/api/v1/tools',
 '{"type":"object","properties":{"action":{"type":"string","enum":["read_file","write_file","list_dir","delete_file"],"description":"操作类型"},"path":{"type":"string","description":"文件路径"},"content":{"type":"string","description":"写入内容(write_file时必填)"}},"required":["action","path"]}',
 '{"type":"object","properties":{"success":{"type":"boolean","description":"是否成功"},"data":{"type":"string","description":"文件内容/操作结果"},"error":{"type":"string","description":"错误信息"}}}',
 'none', '{}', 1, sysdate()),

('http_request', 'HTTP请求工具', 'network',
 '发送 HTTP 请求调用外部 API，支持 GET/POST/PUT/DELETE/PATCH 方法，自定义请求头、请求体和超时',
 'http_api', 'HTTP请求',
 '',
 '{"type":"object","properties":{"method":{"type":"string","enum":["GET","POST","PUT","DELETE","PATCH"],"description":"HTTP方法"},"url":{"type":"string","description":"请求URL"},"headers":{"type":"object","description":"请求头键值对"},"body":{"type":"object","description":"请求体JSON"},"timeoutMs":{"type":"integer","default":30000,"description":"超时(毫秒)"}},"required":["method","url"]}',
 '{"type":"object","properties":{"statusCode":{"type":"integer","description":"HTTP状态码"},"headers":{"type":"object","description":"响应头"},"body":{"type":"string","description":"响应体文本"},"elapsedMs":{"type":"integer","description":"耗时(毫秒)"}}}',
 'api_key', '{"headerName":"Authorization","prefix":"Bearer "}', 1, sysdate()),

('mysql_query', 'MySQL查询工具', 'database',
 '连接 MySQL 数据库，执行查询(SELECT)和更新(INSERT/UPDATE/DELETE)，支持参数化防注入、获取表列表和表结构',
 'mcp_server', 'MySQL查询',
 'http://localhost:8000/api/v1/tools',
 '{"type":"object","properties":{"action":{"type":"string","enum":["execute_query","execute_update","get_tables","describe_table"],"description":"操作类型"},"sql":{"type":"string","description":"SQL语句(execute_query/execute_update时必填)"},"params":{"type":"array","items":{},"description":"查询参数(可选)"},"table":{"type":"string","description":"表名(describe_table时必填)"}},"required":["action"]}',
 '{"type":"object","properties":{"success":{"type":"boolean"},"data":{"description":"查询结果(行列表/影响行数/表列表/表结构)"},"rowCount":{"type":"integer","description":"结果行数"},"elapsedMs":{"type":"integer","description":"执行耗时"}}}',
 'api_key', '{}', 1, sysdate()),

('github_search', 'GitHub搜索工具', 'search',
 '通过 GitHub API 搜索仓库、读取文件内容、创建 Pull Request，需要配置 GitHub Token',
 'mcp_server', 'GitHub搜索',
 'http://localhost:8000/api/v1/tools',
 '{"type":"object","properties":{"action":{"type":"string","enum":["list_repos","get_file_content","create_pr"],"description":"操作类型"},"org":{"type":"string","description":"组织名(list_repos时可选)"},"repo":{"type":"string","description":"仓库名(owner/repo格式)"},"path":{"type":"string","description":"文件路径(get_file_content时必填)"},"ref":{"type":"string","default":"main","description":"分支名"},"title":{"type":"string","description":"PR标题(create_pr时必填)"},"body":{"type":"string","description":"PR描述"},"head":{"type":"string","description":"源分支(create_pr时必填)"},"base":{"type":"string","default":"main","description":"目标分支"}},"required":["action"]}',
 '{"type":"object","properties":{"success":{"type":"boolean"},"data":{"description":"操作结果(仓库列表/文件内容/PR信息)"},"error":{"type":"string","description":"错误信息"}}}',
 'api_key', '{"headerName":"Authorization","prefix":"token "}', 1, sysdate()),

('web_scraper', '网页抓取工具', 'network',
 '抓取网页内容并提取文本/HTML/链接，支持 CSS 选择器定位目标元素',
 'mcp_server', '网页抓取',
 'http://localhost:8000/api/v1/tools',
 '{"type":"object","properties":{"url":{"type":"string","description":"目标网页URL"},"selector":{"type":"string","description":"CSS选择器(可选，提取特定元素)"},"extractType":{"type":"string","enum":["text","html","links"],"default":"text","description":"提取类型"},"timeoutMs":{"type":"integer","default":10000,"description":"超时(毫秒)"}},"required":["url"]}',
 '{"type":"object","properties":{"success":{"type":"boolean"},"title":{"type":"string","description":"页面标题"},"content":{"type":"string","description":"提取内容"},"links":{"type":"array","items":{"type":"string"},"description":"链接列表"},"elapsedMs":{"type":"integer","description":"耗时"}}}',
 'none', '{}', 1, sysdate()),

('python_executor', 'Python执行工具', 'code',
 '在安全沙箱中执行 Python 代码，返回标准输出和错误输出，支持 Python 3.10/3.11/3.12',
 'mcp_server', 'Python执行',
 'http://localhost:8000/api/v1/tools',
 '{"type":"object","properties":{"code":{"type":"string","description":"Python源代码"},"timeout":{"type":"integer","default":30000,"description":"超时(毫秒)"},"pythonVersion":{"type":"string","enum":["3.10","3.11","3.12"],"default":"3.12","description":"Python版本"}},"required":["code"]}',
 '{"type":"object","properties":{"success":{"type":"boolean"},"stdout":{"type":"string","description":"标准输出"},"stderr":{"type":"string","description":"标准错误"},"exitCode":{"type":"integer","description":"退出码"},"elapsedMs":{"type":"integer","description":"执行耗时(毫秒)"}}}',
 'none', '{}', 1, sysdate()),

('kb_search', '企业知识库检索工具', 'enterprise',
 '在企业知识库中进行语义向量检索，基于 Milvus 向量相似度搜索，返回最相关的文档片段及其来源',
 'mcp_server', '知识库检索',
 'http://localhost:8000/api/v1/tools',
 '{"type":"object","properties":{"query":{"type":"string","description":"检索查询内容"},"kbId":{"type":"integer","description":"知识库ID(可选，不填则搜索全部)"},"topK":{"type":"integer","default":5,"minimum":1,"maximum":20,"description":"返回结果条数"},"threshold":{"type":"number","default":0.7,"minimum":0,"maximum":1,"description":"相似度阈值"}},"required":["query"]}',
 '{"type":"object","properties":{"success":{"type":"boolean"},"results":{"type":"array","items":{"type":"object","properties":{"docId":{"type":"integer"},"docName":{"type":"string"},"content":{"type":"string"},"score":{"type":"number"}}},"description":"检索结果列表"},"totalHits":{"type":"integer","description":"命中总数"},"elapsedMs":{"type":"integer","description":"检索耗时(毫秒)"}}}',
 'api_key', '{}', 1, sysdate());