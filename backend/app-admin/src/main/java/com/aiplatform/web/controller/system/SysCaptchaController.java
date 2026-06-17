package com.aiplatform.web.controller.system;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.code.kaptcha.Producer;
import com.aiplatform.common.core.domain.AjaxResult;
import com.aiplatform.common.utils.CacheUtils;
import com.aiplatform.common.utils.uuid.IdUtils;

/**
 * 图片验证码（支持算术形式）
 *
 * @author ruoyi
 */
@RestController
public class SysCaptchaController
{
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    @Value("${shiro.user.captchaEnabled}")
    private boolean captchaEnabled;

    @Value("${shiro.user.captchaType}")
    private String captchaType;

    /**
     * 验证码生成
     */
    @GetMapping(value = "/captchaImage")
    public AjaxResult getKaptchaImage()
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("captchaEnabled", captchaEnabled);

        if (!captchaEnabled)
        {
            return ajax;
        }

        // 生成验证码
        String capStr = null;
        String code = null;
        BufferedImage bi = null;
        if ("char".equals(captchaType))
        {
            capStr = code = captchaProducer.createText();
            bi = captchaProducer.createImage(capStr);
        }
        else
        {
            // 默认 math 类型
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            bi = captchaProducerMath.createImage(capStr);
        }

        // 生成UUID作为缓存key
        String uuid = IdUtils.simpleUUID();

        // 验证码存入缓存（2分钟过期，由ehcache配置控制）
        CacheUtils.put("captchaCache", uuid, code);

        // 图片转base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            ImageIO.write(bi, "jpg", baos);
        }
        catch (IOException e)
        {
            return AjaxResult.error(e.getMessage());
        }
        String img = Base64.getEncoder().encodeToString(baos.toByteArray());

        ajax.put("uuid", uuid);
        ajax.put("img", img);
        return ajax;
    }
}
