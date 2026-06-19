package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.AgentConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent配置 数据层
 *
 * @author aiplatform
 */
@Mapper
public interface AgentConfigMapper
{
    AgentConfig selectAgent(AgentConfig agent);

    AgentConfig selectAgentById(Long agentId);

    AgentConfig selectAgentByName(String agentName);

    List<AgentConfig> selectAgentList(AgentConfig agent);

    int insertAgent(AgentConfig agent);

    int updateAgent(AgentConfig agent);

    int deleteAgentById(Long agentId);

    int deleteAgentByIds(Long[] agentIds);
}