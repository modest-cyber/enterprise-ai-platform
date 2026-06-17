package com.aiplatform.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import com.aiplatform.common.core.controller.BaseController;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.core.domain.entity.SysUser;
import com.aiplatform.common.core.text.Convert;
import com.aiplatform.common.utils.CacheUtils;
import com.aiplatform.common.utils.StringUtils;
import com.aiplatform.framework.shiro.service.SysRegisterService;
import com.aiplatform.system.service.ISysConfigService;

/**
 * 注册验证
 *
 * @author ruoyi
 */
@Controller
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @Value("${shiro.user.captchaEnabled}")
    private boolean captchaEnabled;

    @GetMapping("/register")
    public String register()
    {
        return "register";
    }

    @PostMapping("/register")
    @ResponseBody
    public AjaxResult ajaxRegister(@RequestBody Map<String, Object> params)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }

        // 验证码校验
        if (captchaEnabled)
        {
            String code = Convert.toStr(params.get("code"), "");
            String uuid = Convert.toStr(params.get("uuid"), "");
            if (StringUtils.isEmpty(uuid))
            {
                return error("验证码已失效");
            }
            String captcha = Convert.toStr(CacheUtils.get("captchaCache", uuid), "");
            CacheUtils.remove("captchaCache", uuid);
            if (StringUtils.isEmpty(code) || !code.equalsIgnoreCase(captcha))
            {
                return error("验证码错误");
            }
        }

        SysUser user = new SysUser();
        user.setLoginName(Convert.toStr(params.get("username"), ""));
        user.setPassword(Convert.toStr(params.get("password"), ""));
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
