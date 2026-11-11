package org.campus.campussharebackend.auth;

public record CurrentUser(String id, String account, String displayName, String role) {

    public long userId() {
        return Long.parseLong(id);
    }
}
