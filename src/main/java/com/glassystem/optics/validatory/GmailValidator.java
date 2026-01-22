package com.glassystem.optics.validatory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GmailValidator implements ConstraintValidator<Gmail, String> {

    private static final String REGEX =  "^[a-zA-Z0-9._%+-]+@gmail\\.com$";

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if(email == null||email.isBlank()) return true;
        return email.matches(REGEX);
    }
}
