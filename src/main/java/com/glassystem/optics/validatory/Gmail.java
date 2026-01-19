package com.glassystem.optics.validatory;

import com.nimbusds.jose.Payload;
import jakarta.validation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {GmailValidator.class})
public @interface Gmail {
    String message() default "Invalid email address!";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
