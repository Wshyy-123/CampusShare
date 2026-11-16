package org.campus.campussharebackend.service;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.campus.campussharebackend.CampusShareBackendApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(classes=CampusShareBackendApplication.class,properties={"campusshare.venue-lifecycle.enabled=true","campusshare.venue-lifecycle.scan-delay-ms=50"})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class VenueSchedulingIntegrationTest extends IntegrationTestBase {
 @Test void realSpringSchedulerCompletesWithoutBrowserOrManualInvocation()throws Exception{
  var now=LocalDateTime.now(ZONE);long id=insertReservation(1,201,"venue-scheduled",now.minusHours(2),now.minusHours(1),"CHECKED_IN",null,now.minusHours(2));
  long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
  while(System.nanoTime()<until && !"COMPLETED".equals(jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id=?",String.class,id)))Thread.sleep(25);
  assertThat(jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id=?",String.class,id)).isEqualTo("COMPLETED");
  assertThat(jdbcTemplate.queryForObject("SELECT check_out_at FROM reservation WHERE id=?",LocalDateTime.class,id)).isNull();
  assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM violation_record",Integer.class)).isZero();
 }
}
