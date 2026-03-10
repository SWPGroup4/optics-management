package com.glassystem.optics.dto.request;

import lombok.Data;

@Data
public class BankInfoRequest {

    private String bankName;

    private String bankAccountNumber;

    private String accountHolderName;
}