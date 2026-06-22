-- ============================================
-- 迁移: kb_document 表新增处理状态字段
-- 说明: 对已有数据库执行，新增 vector_count 以及
--       process_status/progress/message/time 列
-- ============================================

-- 对已有数据库执行
ALTER TABLE kb_document
    ADD COLUMN vector_count INT DEFAULT 0 COMMENT '向量数',
    ADD COLUMN process_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态',
    ADD COLUMN process_progress INT DEFAULT 0 COMMENT '处理进度 0-100',
    ADD COLUMN process_message VARCHAR(500) DEFAULT '' COMMENT '当前处理步骤描述',
    ADD COLUMN processed_time DATETIME DEFAULT NULL COMMENT '处理完成时间';
