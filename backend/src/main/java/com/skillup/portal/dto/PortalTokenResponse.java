package com.skillup.portal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/** OAuth2-style token response returned after a successful portal login or token refresh. */
@Data
@Builder
public class PortalTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    /** Token lifetime in seconds. */
    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";
}
