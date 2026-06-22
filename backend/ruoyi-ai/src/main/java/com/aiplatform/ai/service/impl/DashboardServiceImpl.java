package com.aiplatform.ai.service.impl;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.aiplatform.ai.mapper.DashboardMapper;
import com.aiplatform.ai.service.IDashboardService;

/**
 * Dashboard 服务实现
 *
 * @author aiplatform
 */
@Slf4j
@Service
public class DashboardServiceImpl implements IDashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agentCount", dashboardMapper.countAgents());
        result.put("knowledgeCount", dashboardMapper.countKnowledgeBases());
        result.put("documentCount", dashboardMapper.countDocuments());
        result.put("chatCount", dashboardMapper.countConversations());
        return result;
    }

    @Override
    public Map<String, Object> getRecent() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agents", dashboardMapper.selectRecentAgents());
        result.put("knowledge", dashboardMapper.selectRecentKnowledge());
        result.put("conversations", dashboardMapper.selectRecentConversations());
        return result;
    }

    @Override
    public Map<String, Object> getTrend() {
        // 生成近7天日期列表，补齐缺失日期
        List<Map<String, Object>> data = dashboardMapper.countConversationsByDay();
        List<Map<String, Object>> filled = fillMissingDays(data);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", filled);
        return result;
    }

    /** 补齐近7天中无数据的日期 */
    private List<Map<String, Object>> fillMissingDays(List<Map<String, Object>> data) {
        Map<String, Long> dateCountMap = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();
        // 生成最近7天日期
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String dateStr = String.format("%tF", cal);
            dateCountMap.put(dateStr, 0L);
        }
        // 填入已有数据
        for (Map<String, Object> row : data) {
            Object dateObj = row.get("date");
            Object countObj = row.get("count");
            if (dateObj != null && countObj != null) {
                String dateStr = dateObj.toString();
                if (dateCountMap.containsKey(dateStr)) {
                    dateCountMap.put(dateStr, Long.valueOf(countObj.toString()));
                }
            }
        }
        // 转为列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : dateCountMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getModelStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();

        // 检测常见模型服务端口
        services.add(checkService("Ollama", "127.0.0.1", 11434));
        services.add(checkService("Agent-Service", "127.0.0.1", 8081));
        services.add(checkService("Qwen", "127.0.0.1", 8000));
        services.add(checkService("Embedding", "127.0.0.1", 8001));

        result.put("services", services);
        return result;
    }

    /** 通用端口连通性检查 */
    private Map<String, Object> checkService(String name, String host, int port) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", name);
        info.put("host", host);
        info.put("port", port);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            info.put("status", "online");
        } catch (Exception e) {
            info.put("status", "offline");
        }
        return info;
    }
}
