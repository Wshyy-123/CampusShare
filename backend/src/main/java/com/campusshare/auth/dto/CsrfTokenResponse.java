package com.campusshare.auth.dto;

public record CsrfTokenResponse(String headerName, String token) {
}
