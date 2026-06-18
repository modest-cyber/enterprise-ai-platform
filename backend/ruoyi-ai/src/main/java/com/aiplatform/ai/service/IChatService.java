package com.aiplatform.ai.service;

import java.util.List;
import java.util.Map;

public interface IChatService {

    String chat(String prompt);

    String chatWithHistory(String prompt, List<Map<String, String>> history);

    String chatWithTemplate(String templateName, Map<String, Object> variables);
}