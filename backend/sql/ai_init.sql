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
  vector_ids        json            default null               comment 'Milvus向量ID列表',
  status            tinyint(1)      default 0                  comment '状态：0-待处理，1-处理中，2-已完成，3-失败',
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
  model_id          bigint(20)      default null               comment '绑定的模型ID（关联 ai_model.model_id）',
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