package com.nebula.common.validation;

import com.nebula.common.util.PasswordPolicy;
import jakarta.validation.*;
import java.lang.annotation.*;

/** DTO新密码约束，规则统一委托给common工具类。 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NewPassword.Validator.class)
public @interface NewPassword {
    String message() default PasswordPolicy.MESSAGE;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    class Validator implements ConstraintValidator<NewPassword, String> {
        @Override public boolean isValid(String value, ConstraintValidatorContext context) { return PasswordPolicy.isValidNewPassword(value); }
    }
}
