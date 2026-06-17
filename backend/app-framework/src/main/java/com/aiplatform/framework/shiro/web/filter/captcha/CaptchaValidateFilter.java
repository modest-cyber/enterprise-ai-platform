package com.aiplatform.framework.shiro.web.filter.captcha;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.web.filter.AccessControlFilter;
import com.aiplatform.common.constant.ShiroConstants;
import com.aiplatform.common.utils.CacheUtils;
import com.aiplatform.common.utils.StringUtils;

/**
 * 验证码过滤器
 *
 * @author ruoyi
 */
public class CaptchaValidateFilter extends AccessControlFilter
{
    /**
     * 是否开启验证码
     */
    private boolean captchaEnabled = true;

    /**
     * 验证码类型
     */
    private String captchaType = "math";

    public void setCaptchaEnabled(boolean captchaEnabled)
    {
        this.captchaEnabled = captchaEnabled;
    }

    public void setCaptchaType(String captchaType)
    {
        this.captchaType = captchaType;
    }

    @Override
    public boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception
    {
        request.setAttribute(ShiroConstants.CURRENT_ENABLED, captchaEnabled);
        request.setAttribute(ShiroConstants.CURRENT_TYPE, captchaType);
        return super.onPreHandle(request, response, mappedValue);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
            throws Exception
    {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        // 验证码禁用 或不是表单提交 允许访问
        if (captchaEnabled == false || !"post".equals(httpServletRequest.getMethod().toLowerCase()))
        {
            return true;
        }
        // 前端提交: code=用户输入验证码, uuid=缓存key
        String validateCode = httpServletRequest.getParameter("code");
        String uuid = httpServletRequest.getParameter("uuid");
        return validateResponse(uuid, validateCode);
    }

    public boolean validateResponse(String uuid, String validateCode)
    {
        if (StringUtils.isEmpty(uuid))
        {
            return false;
        }
        Object obj = CacheUtils.get("captchaCache", uuid);
        String code = String.valueOf(obj != null ? obj : "");
        // 验证码清除，防止多次使用
        CacheUtils.remove("captchaCache", uuid);
        if (StringUtils.isEmpty(validateCode) || !validateCode.equalsIgnoreCase(code))
        {
            return false;
        }
        return true;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception
    {
        request.setAttribute(ShiroConstants.CURRENT_CAPTCHA, ShiroConstants.CAPTCHA_ERROR);
        return true;
    }
}
