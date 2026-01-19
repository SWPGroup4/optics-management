package com.glassystem.optics.validatory;

import com.nimbusds.jose.Payload;
import jakarta.validation.Constraint;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {VietnamPhoneValidator.class})
public @interface VietNamPhone {
    String message() default "Invalid Vietnam phone number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
