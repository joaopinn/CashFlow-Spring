package com.cashflow.demo.validation;

import jakarta.validation.*;
import java.lang.annotation.*;
import java.nio.charset.StandardCharsets;

@Documented
@Constraint(validatedBy = PasswordBytes.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordBytes {
    String message() default "A senha deve ter no máximo 72 bytes em UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PasswordBytes, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }
}
