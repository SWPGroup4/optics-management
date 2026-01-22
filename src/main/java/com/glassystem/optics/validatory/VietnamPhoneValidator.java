package com.glassystem.optics.validatory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VietnamPhoneValidator implements ConstraintValidator<VietNamPhone, String> {

    private static final String REGEX =  "^(03|05|07|08|09)[0-9]{8}$";

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if(phone == null || phone.isBlank()) return true;
        return phone.matches(REGEX);
    }
}
