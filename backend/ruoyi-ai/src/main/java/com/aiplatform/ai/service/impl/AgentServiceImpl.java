package com.aiplatform.ai.service.impl;

import java.util.List;
import java.util.UUID;

import com.aiplatform.ai.agent.AgentClient;
import com.aiplatform.ai.domain.AgentConfig;
import com.aiplatform.ai.mapper.AgentConfigMapper;
import com.aiplatform.ai.service.IAgentService;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Agent 管理服务实现
 * <p>
 * 负责 Agent 配置的 CRUD 管理，以及通过 AgentClient 调用 Python Agent 执行任务。
 * 基础入参非空校验由 Controller 层 DTO + @Valid 完成，Service 只保留业务规则。
 * 当前阶段异步任务的管理为简化实现，M7 阶段对接真实的任务队列后完善。
 *
 * @author aiplatform
 */
@Slf4j
@Service
public class AgentServiceImpl implements IAgentService {

    @Autowired
    private AgentConfigMapper agentConfigMapper;

    @Autowired
    private AgentClient agentClient;

    // ==================== 查询 ====================

    @Override
    public AgentConfig selectAgentById(Long agentId) {
        return agentConfigMapper.selectAgentById(agentId);
    }

    @Override
    public AgentConfig selectAgentByName(String agentName) {
        return agentConfigMapper.selectAgentByName(agentName);
    }

    @Override
    public List<AgentConfig> selectAgentList(AgentConfig agent) {
        return agentConfigMapper.selectAgentList(agent);
    }

    @Override
    public List<AgentConfig> selectEnabledAgents() {
        return agentConfigMapper.selectEnabledAgents();
    }

    // ==================== CRUD ====================

    @Override
    public int insertAgent(AgentConfig agent) {
        // 名称唯一性校验（业务规则）
        AgentConfig exist = agentConfigMapper.selectAgentByName(agent.getAgentName());
        if (exist != null) {
            throw new ServiceException("Agent名称已存在: " + agent.getAgentName());
        }
        agent.setCreateBy(SecurityUtils.getUsername());
        return agentConfigMapper.insertAgent(agent);
    }

    @Override
    public int updateAgent(AgentConfig agent) {
        // 名称唯一性校验（业务规则，排除自身）
        AgentConfig exist = agentConfigMapper.selectAgentByName(agent.getAgentName());
        if (exist != null && !exist.getAgentId().equals(agent.getAgentId())) {
            throw new ServiceException("Agent名称已存在: " + agent.getAgentName());
        }
        agent.setUpdateBy(SecurityUtils.getUsername());
        return agentConfigMapper.updateAgent(agent);
    }

    @Override
    public int deleteAgentByIds(Long[] agentIds) {
        return agentConfigMapper.deleteAgentByIds(agentIds);
    }

    // ==================== 执行 ====================


    @Override
    public String executeSync(Long agentId, String task, String input) {
        AgentConfig agent = agentConfigMapper.selectAgentById(agentId);
        if (agent == null) {
            throw new ServiceException("Agent不存在, agentId=" + agentId);
        }
        // 业务规则：停用的 Agent 不允许执行
        if (agent.getStatus() != null && agent.getStatus() == 0) {
            throw new ServiceException("Agent已停用, agentName=" + agent.getAgentName());
        }
        log.info("同步执行 Agent 任务, agentId={}, agentName={}, task={}", agentId, agent.getAgentName(), task);
        return agentClient.execute(task, input);
    }


    @Override
    public String submitTask(Long agentId, String task, String input) {
        AgentConfig agent = agentConfigMapper.selectAgentById(agentId);
        if (agent == null) {
            throw new ServiceException("Agent不存在, agentId=" + agentId);
        }
        // 业务规则：停用的 Agent 不允许执行
        if (agent.getStatus() != null && agent.getStatus() == 0) {
            throw new ServiceException("Agent已停用, agentName=" + agent.getAgentName());
        }
        String taskId = UUID.randomUUID().toString().replace("-", "");
        log.info("异步提交 Agent 任务, agentId={}, agentName={}, taskId={}, task={}",
                agentId, agent.getAgentName(), taskId, task);

        // 异步执行（当前简化实现：启动新线程执行，M7 对接任务队列后替换）
        new Thread(() -> {
            try {
                log.info("异步任务开始执行, taskId={}", taskId);
                agentClient.execute(task, input);
                log.info("异步任务执行完成, taskId={}", taskId);
            } catch (Exception e) {
                log.error("异步任务执行失败, taskId={}", taskId, e);
            }
        }, "agent-task-" + taskId).start();

        return taskId;
    }

    @Override
    public String getTaskStatus(String taskId) {
        // 当前阶段由 AgentClient 查询 Python Agent 端任务状态
        String status = agentClient.getTaskStatus(taskId);
        if (status == null) {
            return "{\"taskId\":\"" + taskId + "\",\"status\":\"unknown\",\"message\":\"任务状态查询暂不支持(Mock)\"}";
        }
        return status;
    }

    @Override
    public boolean cancelTask(String taskId) {
        // 当前阶段为简化实现（M7 对接真实任务取消逻辑）
        log.info("取消任务请求, taskId={} (当前为Mock实现)", taskId);
        return true;
    }
}
