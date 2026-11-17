package org.campus.campussharebackend.support;

import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.when;

/** Run reservation scenarios at 08:00 on their fixture day, independent of wall-clock time. */
public abstract class BookingClockTestBase extends IntegrationTestBase {
    @MockitoBean(name = "lifecycleClock") protected Clock bookingClock;

    @BeforeEach void setBookingClock() {
        when(bookingClock.getZone()).thenReturn(ZONE);
        when(bookingClock.instant()).thenReturn(tomorrowAt(8, 0).atZone(ZONE).toInstant());
    }
}
