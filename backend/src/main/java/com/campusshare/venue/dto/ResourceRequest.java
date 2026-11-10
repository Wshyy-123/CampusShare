package com.campusshare.venue.dto;

import com.campusshare.venue.BookingUnit;
import com.campusshare.venue.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
        @NotBlank(message = "资源名称不能为空")
        @Size(max = 100, message = "资源名称长度不能超过100个字符")
        String name,

        @NotBlank(message = "资源位置不能为空")
        @Size(max = 200, message = "资源位置长度不能超过200个字符")
        String location,

        @NotNull(message = "资源类型不能为空")
        ResourceType type,

        @NotNull(message = "预约单位不能为空")
        BookingUnit bookingUnit
) {
}
