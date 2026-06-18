package com.aiplatform.ai.service.impl;

import java.util.List;
import java.util.UUID;

import com.aiplatform.ai.agent.AgentClient;
import com.aiplatform.ai.domain.AgentConfig;
import com.aiplatform.ai.mapper.AgentConfigMapper;
import com.aiplatform.ai.service.IAgentService;
import com.aiplatform.common.exception.ServiceException;
import com.aiplatform.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Agent 管理服务实现
 * <p>
 * 负责 Agent 配置的 CRUD 管理，以及通过 AgentClient 调用 Python Agent 执行任务。
 * 当前阶段异步任务的管理（submitTask/getTaskStatus/cancelTask）为简化实现，
 * M7 阶段对接真实的任务队列后完善。
 *
 * @author aiplatform
 */
@Service
public class AgentServiceImpl implements IAgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

    @Autowired
    private AgentConfigMapper agentConfigMapper;

    @Autowired
    private AgentClient agentClient;

    // ==================== 查询 ====================

    @Override
    public AgentConfig selectAgentById(Long agentId) {
        if (agentId == null) {
            throw new ServiceException("Agent ID不能为空");
        }
        return agentConfigMapper.selectAgentById(agentId);
    }

    @Override
    public AgentConfig selectAgentByName(String agentName) {
        if (!StringUtils.hasText(agentName)) {
            throw new ServiceException("Agent名称不能为空");
        }
        return agentConfigMapper.selectAgentByName(agentName);
    }

    @Override
    public List<AgentConfig> selectAgentList(AgentConfig agent) {
        return agentConfigMapper.selectAgentList(agent);
    }

    // ==================== CRUD ====================

    @Override
    public int insertAgent(AgentConfig agent) {
        if (agent == null) {
            throw new ServiceException("Agent配置不能为空");
        }
        if (!StringUtils.hasText(agent.getAgentName())) {
            throw new ServiceException("Agent名称不能为空");
        }
        // 名称唯一性校验
        AgentConfig exist = agentConfigMapper.selectAgentByName(agent.getAgentName());
        if (exist != null) {
            throw new ServiceException("Agent名称已存在: " + agent.getAgentName());
        }
        agent.setCreateBy(SecurityUtils.getUsername());
        return agentConfigMapper.insertAgent(agent);
    }

    @Override
    public int updateAgent(AgentConfig agent) {
        if (agent == null || agent.getAgentId() == null) {
            throw new ServiceException("Agent ID不能为空");
        }
        // 名称唯一性校验（排除自身）
        AgentConfig exist = agentConfigMapper.selectAgentByName(agent.getAgentName());
        if (exist != null && !exist.getAgentId().equals(agent.getAgentId())) {
            throw new ServiceException("Agent名称已存在: " + agent.getAgentName());
        }
        agent.setUpdateBy(SecurityUtils.getUsername());
        return agentConfigMapper.updateAgent(agent);
    }

    @Override
    public int deleteAgentByIds(Long[] agentIds) {
        if (agentIds == null || agentIds.length == 0) {
            throw new ServiceException("待删除的Agent ID列表不能为空");
        }
        return agentConfigMapper.deleteAgentByIds(agentIds);
    }

    // ==================== 执行 ====================

    @Override
    public String executeSync(Long agentId, String task, String input) {
        if (agentId == null) {
            throw new ServiceException("Agent ID不能为空");
        }
        if (!StringUtils.hasText(task)) {
            throw new ServiceException("任务描述不能为空");
        }
        AgentConfig agent = agentConfigMapper.selectAgentById(agentId);
        if (agent == null) {
            throw new ServiceException("Agent不存在, agentId=" + agentId);
        }
        if (agent.getStatus() != null && agent.getStatus() == 0) {
            throw new ServiceException("Agent已停用, agentName=" + agent.getAgentName());
        }
        log.info("同步执行 Agent 任务, agentId={}, agentName={}, task={}", agentId, agent.getAgentName(), task);
        return agentClient.execute(task, input);
    }

    @Override
    public String submitTask(Long agentId, String task, String input) {
        if (agentId == null) {
            throw new ServiceException("Agent ID不能为空");
        }
        if (!StringUtils.hasText(task)) {
            throw new ServiceException("任务描述不能为空");
        }
        AgentConfig agent = agentConfigMapper.selectAgentById(agentId);
        if (agent == null) {
            throw new ServiceException("Agent不存在, agentId=" + agentId);
        }
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
        if (!StringUtils.hasText(taskId)) {
            throw new ServiceException("任务ID不能为空");
        }
        // 当前阶段由 AgentClient 查询 Python Agent 端任务状态
        // M7 对接后改为查询本地任务队列表
        String status = agentClient.getTaskStatus(taskId);
        if (status == null) {
            return "{\"taskId\":\"" + taskId + "\",\"status\":\"unknown\",\"message\":\"任务状态查询暂不支持(Mock)\"}";
        }
        return status;
    }

    @Override
    public boolean cancelTask(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new ServiceException("任务ID不能为空");
        }
        // 当前阶段为简化实现（M7 对接真实任务取消逻辑）
        log.info("取消任务请求, taskId={} (当前为Mock实现)", taskId);
        return true;
    }
}
