package com.qualitypilot.campaignlab.campaign;

import org.springframework.http.HttpStatus;

public class CampaignException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public CampaignException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
