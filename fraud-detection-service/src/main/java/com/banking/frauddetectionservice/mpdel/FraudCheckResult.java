package com.banking.frauddetectionservice.mpdel;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class FraudCheckResult {

    private boolean fraud;
    private String reason;
}
