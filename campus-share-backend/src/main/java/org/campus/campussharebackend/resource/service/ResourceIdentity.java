package org.campus.campussharebackend.resource.service;

import java.text.Normalizer;
import java.util.Locale;
import org.campus.campussharebackend.resource.entity.ResourceRecord;

/** A physical booking unit is identified by location, type and label, including disabled units. */
record ResourceIdentity(String location, String type, String unit) {
    static ResourceIdentity of(ResourceRecord value) {
        return new ResourceIdentity(normalize(value.getLocation()), normalize(value.getResourceType()), normalize(value.getUnitLabel()));
    }
    private static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Z}]+", " ").strip().toLowerCase(Locale.ROOT);
    }
    boolean valid() { return !location.isEmpty() && !type.isEmpty() && !unit.isEmpty(); }
}
