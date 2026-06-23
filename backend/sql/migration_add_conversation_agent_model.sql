-- 会话绑定 Agent 和 Model
ALTER TABLE ai_conversation
    ADD COLUMN agent_id BIGINT DEFAULT NULL COMMENT '绑定的Agent ID',
    ADD COLUMN model_id BIGINT DEFAULT NULL COMMENT '绑定的模型 ID';