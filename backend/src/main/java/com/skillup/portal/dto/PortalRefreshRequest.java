package com.skillup.portal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Refresh token payload for the portal token-refresh endpoint. */
@Data
public class PortalRefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
