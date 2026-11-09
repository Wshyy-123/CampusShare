package com.campusshare.user;

public enum UserRole {

    STUDENT("学生"),
    TEACHER("教师"),
    RESOURCE_ADMIN("资源管理员");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
