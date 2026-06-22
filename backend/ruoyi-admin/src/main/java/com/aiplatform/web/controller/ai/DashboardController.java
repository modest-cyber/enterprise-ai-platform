package com.aiplatform.web.controller.ai;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aiplatform.ai.service.IDashboardService;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;

/**
 * Dashboard 首页控制器
 *
 * @author aiplatform
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController extends BaseController {

    @Autowired
    private IDashboardService dashboardService;

    /** 系统概览统计 */
    @GetMapping("/overview")
    public AjaxResult overview() {
        Map<String, Object> data = dashboardService.getOverview();
        return success(data);
    }

    /** 最近活动 */
    @GetMapping("/recent")
    public AjaxResult recent() {
        Map<String, Object> data = dashboardService.getRecent();
        return success(data);
    }

    /** 近7天会话趋势 */
    @GetMapping("/trend")
    public AjaxResult trend() {
        Map<String, Object> data = dashboardService.getTrend();
        return success(data);
    }

    /** 模型服务状态 */
    @GetMapping("/modelStatus")
    public AjaxResult modelStatus() {
        Map<String, Object> data = dashboardService.getModelStatus();
        return success(data);
    }
}
