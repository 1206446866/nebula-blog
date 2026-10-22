package com.nebula.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.nebula.auth.util.RegistrationEmail;
import lombok.Data;

@Data
public class EmailCaptchaDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String email;

    public void setEmail(String email) { this.email = RegistrationEmail.normalize(email); }
}
