package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.AiTool;

/**
 * MCP工具注册 数据层
 *
 * @author aiplatform
 */
public interface AiToolMapper
{
    AiTool selectTool(AiTool tool);

    AiTool selectToolById(Long toolId);

    AiTool selectToolByName(String toolName);

    List<AiTool> selectToolList(AiTool tool);

    List<AiTool> selectEnabledTools();

    int insertTool(AiTool tool);

    int updateTool(AiTool tool);

    int deleteToolById(Long toolId);

    int deleteToolByIds(Long[] toolIds);
}