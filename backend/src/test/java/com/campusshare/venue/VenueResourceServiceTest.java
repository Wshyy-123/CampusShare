package com.campusshare.venue;

import com.campusshare.common.error.BusinessException;
import com.campusshare.common.error.ErrorCode;
import com.campusshare.venue.dto.ResourceRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VenueResourceServiceTest {

    private final VenueResourceRepository repository = mock(VenueResourceRepository.class);
    private final VenueResourceService service = new VenueResourceService(repository);

    @Test
    void createRejectsMismatchedBookingUnit() {
        ResourceRequest request = new ResourceRequest(
                "乒乓球桌A", "北体育馆", ResourceType.PING_PONG_TABLE, BookingUnit.ROOM);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_BOOKING_UNIT);
    }

    @Test
    void createTrimsTextAndReturnsDisplayLabels() {
        when(repository.save(any(VenueResource.class))).thenAnswer(invocation -> {
            VenueResource resource = invocation.getArgument(0);
            resource.setId(1L);
            return resource;
        });

        ResourceRequest request = new ResourceRequest(
                " 乒乓球桌A ", " 北体育馆 ", ResourceType.PING_PONG_TABLE, BookingUnit.TABLE);

        var response = service.create(request);

        ArgumentCaptor<VenueResource> captor = ArgumentCaptor.forClass(VenueResource.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("乒乓球桌A");
        assertThat(captor.getValue().getLocation()).isEqualTo("北体育馆");
        assertThat(response.typeLabel()).isEqualTo("乒乓球桌");
        assertThat(response.bookingUnitLabel()).isEqualTo("桌");
    }
}
