package com.aiplatform.ai.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.aiplatform.ai.domain.AiTool;
import com.aiplatform.ai.mapper.AiToolMapper;
import com.aiplatform.ai.service.IToolService;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MCP 工具管理服务实现
 * <p>
 * 负责 MCP 工具的注册、配置管理和调用执行。
 * invokeTool() 当前为 Mock 实现，M7 阶段对接 McpToolExecutor 实现真实工具调用。
 * testConnection() 对有 serverUrl 的工具发送 HTTP GET 验证连通性。
 *
 * @author aiplatform
 */
@Service
public class ToolServiceImpl implements IToolService {

    private static final Logger log = LoggerFactory.getLogger(ToolServiceImpl.class);

    @Autowired
    private AiToolMapper aiToolMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 查询 ====================

    @Override
    public AiTool selectToolById(Long toolId) {
        if (toolId == null) {
            throw new ServiceException("工具ID不能为空");
        }
        return aiToolMapper.selectToolById(toolId);
    }

    @Override
    public AiTool selectToolByName(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            throw new ServiceException("工具名称不能为空");
        }
        return aiToolMapper.selectToolByName(toolName);
    }

    @Override
    public List<AiTool> selectToolList(AiTool tool) {
        return aiToolMapper.selectToolList(tool);
    }

    @Override
    public List<AiTool> selectEnabledTools() {
        return aiToolMapper.selectEnabledTools();
    }

    // ==================== CRUD ====================

    @Override
    public int insertTool(AiTool tool) {
        if (tool == null) {
            throw new ServiceException("工具配置不能为空");
        }
        if (!StringUtils.hasText(tool.getToolName())) {
            throw new ServiceException("工具名称不能为空");
        }
        // 名称唯一性校验
        AiTool exist = aiToolMapper.selectToolByName(tool.getToolName());
        if (exist != null) {
            throw new ServiceException("工具名称已存在: " + tool.getToolName());
        }
        tool.setCreateBy(SecurityUtils.getUsername());
        return aiToolMapper.insertTool(tool);
    }

    @Override
    public int updateTool(AiTool tool) {
        if (tool == null || tool.getToolId() == null) {
            throw new ServiceException("工具ID不能为空");
        }
        // 名称唯一性校验（排除自身）
        AiTool exist = aiToolMapper.selectToolByName(tool.getToolName());
        if (exist != null && !exist.getToolId().equals(tool.getToolId())) {
            throw new ServiceException("工具名称已存在: " + tool.getToolName());
        }
        tool.setUpdateBy(SecurityUtils.getUsername());
        return aiToolMapper.updateTool(tool);
    }

    @Override
    public int deleteToolByIds(Long[] toolIds) {
        if (toolIds == null || toolIds.length == 0) {
            throw new ServiceException("待删除的工具ID列表不能为空");
        }
        return aiToolMapper.deleteToolByIds(toolIds);
    }

    // ==================== 执行 ====================

    @Override
    public String invokeTool(Long toolId, String params) {
        if (toolId == null) {
            throw new ServiceException("工具ID不能为空");
        }
        AiTool tool = aiToolMapper.selectToolById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在, toolId=" + toolId);
        }
        if (tool.getIsEnabled() == null || tool.getIsEnabled() == 0) {
            throw new ServiceException("工具已停用, toolName=" + tool.getToolName());
        }

        // 校验输入参数 schema（简单校验：params 必须为合法 JSON）
        if (StringUtils.hasText(params)) {
            try {
                objectMapper.readTree(params);
            } catch (Exception e) {
                throw new ServiceException("输入参数不是合法的 JSON 格式: " + e.getMessage());
            }
        }

        log.info("Mock 工具调用, toolId={}, toolName={}, toolType={}, params={}",
                toolId, tool.getToolName(), tool.getToolType(), params);

        // 当前阶段 Mock 返回（M7 完成后改为 McpToolExecutor.invoke(toolName, params)）
        return "{\"status\":\"ok\",\"message\":\"工具调用成功(Mock)\",\"toolName\":\""
                + escapeJson(tool.getToolName()) + "\"}";
    }

    @Override
    public boolean testConnection(Long toolId) {
        if (toolId == null) {
            throw new ServiceException("工具ID不能为空");
        }
        AiTool tool = aiToolMapper.selectToolById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在, toolId=" + toolId);
        }

        // 对于无 serverUrl 的工具（如 function 类型），直接返回 true
        if (!StringUtils.hasText(tool.getServerUrl())) {
            log.info("工具无 Server URL，跳过连通性测试, toolId={}, toolName={}, toolType={}",
                    toolId, tool.getToolName(), tool.getToolType());
            return true;
        }

        log.info("测试工具连通性, toolId={}, toolName={}, serverUrl={}",
                toolId, tool.getToolName(), tool.getServerUrl());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tool.getServerUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 500;
            log.info("工具连通性测试结果, toolId={}, status={}, success={}", toolId, response.statusCode(), success);
            return success;
        } catch (IOException | InterruptedException e) {
            log.error("工具连通性测试失败, toolId={}", toolId, e);
            return false;
        }
    }

    /**
     * JSON 字符串转义
     */
    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
