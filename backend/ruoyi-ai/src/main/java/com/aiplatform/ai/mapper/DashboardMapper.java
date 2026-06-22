package com.aiplatform.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 数据层
 *
 * @author aiplatform
 */
@Mapper
public interface DashboardMapper {

    /** 统计Agent总数 */
    Long countAgents();

    /** 统计知识库总数 */
    Long countKnowledgeBases();

    /** 统计文档总数 (未删除) */
    Long countDocuments();

    /** 统计会话总数 */
    Long countConversations();

    /** 近7天每日会话数 */
    List<Map<String, Object>> countConversationsByDay();

    /** 最近Agent创建记录 */
    List<Map<String, Object>> selectRecentAgents();

    /** 最近知识库创建记录 */
    List<Map<String, Object>> selectRecentKnowledge();

    /** 最近会话记录 */
    List<Map<String, Object>> selectRecentConversations();
}
