package com.aiplatform.ai.service;

import java.util.Map;

/**
 * Dashboard 服务接口
 *
 * @author aiplatform
 */
public interface IDashboardService {

    /** 获取概览统计数据 */
    Map<String, Object> getOverview();

    /** 获取最近活动列表 */
    Map<String, Object> getRecent();

    /** 获取近7天会话趋势 */
    Map<String, Object> getTrend();

    /** 获取模型服务状态 */
    Map<String, Object> getModelStatus();
}
