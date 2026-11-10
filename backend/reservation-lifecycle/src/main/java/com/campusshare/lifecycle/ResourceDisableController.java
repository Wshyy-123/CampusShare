package com.campusshare.lifecycle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/resources")
public class ResourceDisableController {
    public record Request(@NotNull OffsetDateTime startTime, @NotNull OffsetDateTime endTime,
                          @NotBlank @Size(max = 500) String reason) {}
    public record Response(String code, String message, Object data, String requestId) {}
    private final ResourceDisableService service;

    public ResourceDisableController(ResourceDisableService service) { this.service = service; }

    @PostMapping("/{resourceId}/disable-periods")
    public Response disable(@PathVariable long resourceId, @Valid @RequestBody Request request) {
        var requestId = UUID.randomUUID().toString();
        var result = service.disable(resourceId, request.startTime(), request.endTime(), request.reason(), requestId);
        return new Response("OK", "停用成功", result, requestId);
    }
}
