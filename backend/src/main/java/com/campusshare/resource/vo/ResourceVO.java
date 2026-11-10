package com.campusshare.resource.vo;

import com.campusshare.resource.entity.ResourceRecord;

public record ResourceVO(String resourceId, String name, String location,
                         String resourceType, String unitLabel, boolean enabled) {

    public static ResourceVO from(ResourceRecord record) {
        return new ResourceVO(
                Long.toString(record.getId()),
                record.getName(),
                record.getLocation(),
                record.getResourceType(),
                record.getUnitLabel(),
                record.isEnabled());
    }
}
