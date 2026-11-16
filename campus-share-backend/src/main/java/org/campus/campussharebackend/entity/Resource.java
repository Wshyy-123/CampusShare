package org.campus.campussharebackend.entity;

import lombok.Data;

/**
 * 可预约资源。XIA-7 / XIA-8 需要 resource_type 判断是否为体育场地。
 */
@Data
public class Resource {
    private Long id;
    private String name;
    private String location;
    private String resourceType;
    private Boolean enabled;
}
