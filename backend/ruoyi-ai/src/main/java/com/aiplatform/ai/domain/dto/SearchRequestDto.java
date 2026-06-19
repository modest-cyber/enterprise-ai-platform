package com.aiplatform.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class SearchRequestDto {

    private List<Long> kbIds;

    @NotBlank(message = "检索关键词不能为空")
    private String query;

    private Integer topK;

    private Double minScore;

}