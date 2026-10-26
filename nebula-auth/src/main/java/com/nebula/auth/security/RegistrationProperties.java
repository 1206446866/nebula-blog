package com.nebula.auth.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

/** 注册确认页面的可信地址，不从请求头推导域名。 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "nebula.registration")
public class RegistrationProperties {
    @NotBlank private String confirmationUrl = "http://localhost:5173/register";

    /** 只接受不含凭据、查询参数及片段的HTTP(S)地址，避免拼接到错误域名或破坏邮件链接。 */
    @AssertTrue(message = "注册确认地址必须是有效的HTTP(S)页面地址，且不能包含凭据、查询参数或片段")
    public boolean isConfirmationUrlValid() {
        try {
            URI uri = URI.create(confirmationUrl);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null;
        } catch (IllegalArgumentException | NullPointerException e) { return false; }
    }

    /** 将服务端生成的确认凭证编码为查询参数；调用方负责HTML属性转义。 */
    public String confirmationLink(String token) {
        return UriComponentsBuilder.fromUriString(confirmationUrl)
                .queryParam("token", "{token}").encode().buildAndExpand(token).toUriString();
    }
}
