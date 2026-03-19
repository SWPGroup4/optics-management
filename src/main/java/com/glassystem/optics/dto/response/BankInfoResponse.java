package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BankInfoResponse {
    String bankName;
    String bankAccountNumber;
    String accountHolderName;
}