package com.glassystem.optics.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class RefundBatchRequest {

    private List<String> orderIds;
}