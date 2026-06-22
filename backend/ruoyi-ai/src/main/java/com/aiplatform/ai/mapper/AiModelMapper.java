package com.aiplatform.ai.mapper;

import java.util.List;
import com.aiplatform.ai.domain.AiModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI模型配置 数据层
 *
 * @author aiplatform
 */
@Mapper
public interface AiModelMapper
{
    AiModel selectModel(AiModel model);

    AiModel selectModelById(Long modelId);

    List<AiModel> selectModelList(AiModel model);

    List<AiModel> selectEnabledModels();

    int insertModel(AiModel model);

    int updateModel(AiModel model);

    int deleteModelById(Long modelId);

    int deleteModelByIds(Long[] modelIds);
}