package com.aiplatform.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class KnowledgeUploadDto {

    private Long kbId;

    private MultipartFile file;

    private Integer chunkSize;

    private Integer chunkOverlap;

}