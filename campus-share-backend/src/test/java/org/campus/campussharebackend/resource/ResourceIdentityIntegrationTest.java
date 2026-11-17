package org.campus.campussharebackend.resource;

import java.util.*;
import java.util.concurrent.*;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username="admin01", roles="RESOURCE_ADMIN")
class ResourceIdentityIntegrationTest extends IntegrationTestBase {
    @AfterEach void removeOnlyIsolatedTestResources() {
        jdbcTemplate.update("DELETE FROM resource WHERE id NOT IN (101,102,201,401)");
    }

    @Test void duplicateCreateIsConflict() throws Exception {
        var body=body("测试资源", "去重测试馆", "LIBRARY_SEAT", "A-01",true);
        create(body).andExpect(status().isOk());
        create(body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESOURCE_DUPLICATE"));
        assertThat(extraCount()).isEqualTo(1);
    }

    @Test void differentNameDoesNotPermitSamePhysicalResource() throws Exception {
        create(body("原名称","去重测试馆","LIBRARY_SEAT","A-01",true)).andExpect(status().isOk());
        create(body("另一名称","去重测试馆","LIBRARY_SEAT","A-01",true)).andExpect(status().isConflict());
        assertThat(extraCount()).isEqualTo(1);
    }

    @Test void whitespaceCaseAndFullWidthCannotBypassIdentity() throws Exception {
        create(body("原名称","Test Hall","LIBRARY_SEAT","A-01",true)).andExpect(status().isOk());
        create(body("变体"," test   hall ","LIBRARY_SEAT"," a-01 ",true)).andExpect(status().isConflict());
        create(body("全角","Ｔｅｓｔ　Ｈａｌｌ","LIBRARY_SEAT","Ａ－０１",true)).andExpect(status().isConflict());
        assertThat(extraCount()).isEqualTo(1);
    }

    @Test void disabledResourceStillOwnsIdentity() throws Exception {
        create(body("原名称","去重测试馆","LIBRARY_SEAT","A-01",false)).andExpect(status().isOk());
        create(body("原名称","去重测试馆","LIBRARY_SEAT","A-01",true)).andExpect(status().isConflict());
        assertThat(extraCount()).isEqualTo(1);
    }

    @Test void editingSelfWithoutIdentityChangeIsAllowed() throws Exception {
        long id=createId(body("原名称","去重测试馆","LIBRARY_SEAT","A-01",true));
        update(id,body("更正名称","去重测试馆","LIBRARY_SEAT","A-01",true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("更正名称"));
        assertThat(extraCount()).isEqualTo(1);
    }

    @Test void editingIntoAnotherResourceFailsWithoutChangingEitherRecord() throws Exception {
        long first=createId(body("原名称","去重测试馆","LIBRARY_SEAT","A-01",true));
        long second=createId(body("第二座位","去重测试馆","LIBRARY_SEAT","A-02",true));
        update(second,body("更改名称","去重测试馆","LIBRARY_SEAT","A-01",false))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESOURCE_DUPLICATE"));
        assertThat(jdbcTemplate.queryForObject("SELECT unit_label FROM resource WHERE id=?",String.class,second)).isEqualTo("A-02");
        assertThat(jdbcTemplate.queryForObject("SELECT enabled FROM resource WHERE id=?",Boolean.class,second)).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT name FROM resource WHERE id=?",String.class,first)).isEqualTo("原名称");
    }

    @Test void sameLabelInAnotherLocationOrTypeIsAllowed() throws Exception {
        create(body("一号座位","去重测试馆","LIBRARY_SEAT","A-01",true)).andExpect(status().isOk());
        create(body("一号座位","另一馆","LIBRARY_SEAT","A-01",true)).andExpect(status().isOk());
        create(body("一号教室","去重测试馆","CLASSROOM","A-01",true)).andExpect(status().isOk());
        assertThat(extraCount()).isEqualTo(3);
    }

    @Test void legacyDuplicatesArePreservedButCannotBeAddedAgain() throws Exception {
        create(body("历史资源","去重测试馆","LIBRARY_SEAT","A-01",true)).andExpect(status().isOk());
        jdbcTemplate.update("INSERT INTO resource(name,location,resource_type,unit_label,enabled) VALUES(?,?,?,?,TRUE)",
                "旧重复记录","去重测试馆","LIBRARY_SEAT","A-01");
        create(body("又一次","去重测试馆","LIBRARY_SEAT","A-01",true)).andExpect(status().isConflict());
        assertThat(extraCount()).isEqualTo(2);
    }

    @Test void unicodeBlankIdentityIsRejected() throws Exception {
        create(body("空位置","\u3000\u00a0","LIBRARY_SEAT","A-01",true)).andExpect(status().isBadRequest());
        assertThat(extraCount()).isZero();
    }

    @Test void existingDuplicateCanBeDisabledWithoutDeletingEitherRecord() throws Exception {
        String original=body("原记录","去重测试馆","LIBRARY_SEAT","A-01",true);
        long id=createId(original);
        jdbcTemplate.update("INSERT INTO resource(name,location,resource_type,unit_label,enabled) VALUES(?,?,?,?,TRUE)",
                "旧重复记录","去重测试馆","LIBRARY_SEAT","A-01");
        update(id,body("原记录","去重测试馆","LIBRARY_SEAT","A-01",false)).andExpect(status().isOk());
        assertThat(extraCount()).isEqualTo(2);
        create(original).andExpect(status().isConflict());
    }

    @Test void concurrentIdenticalCreatesProduceExactlyOneResource() throws Exception {
        String payload=body("并发座位","并发测试馆","LIBRARY_SEAT","C-01",true);
        List<Integer> results=concurrent(() -> create(payload).andReturn().getResponse().getStatus(),
                () -> create(payload).andReturn().getResponse().getStatus());
        assertThat(results).containsExactlyInAnyOrder(200,409);
        assertThat(extraCount()).isEqualTo(1);
    }

    @Test void concurrentEditsToSameIdentityCannotBothSucceed() throws Exception {
        long first=createId(body("座位一","去重测试馆","LIBRARY_SEAT","A-01",true));
        long second=createId(body("座位二","去重测试馆","LIBRARY_SEAT","A-02",true));
        String target=body("共同编号","去重测试馆","LIBRARY_SEAT","A-03",true);
        List<Integer> results=concurrent(() -> update(first,target).andReturn().getResponse().getStatus(),
                () -> update(second,target).andReturn().getResponse().getStatus());
        assertThat(results).containsExactlyInAnyOrder(200,409);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource WHERE unit_label='A-03'",Integer.class)).isEqualTo(1);
    }

    private List<Integer> concurrent(Callable<Integer> first,Callable<Integer> second) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Integer>>();
            for(var action:List.of(first,second))futures.add(pool.submit(() -> {
                ready.countDown();if(!start.await(5,TimeUnit.SECONDS))throw new IllegalStateException("barrier timeout");return action.call();
            }));
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();start.countDown();
            return List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS));
        } finally {start.countDown();pool.shutdownNow();}
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/resources").with(user("admin01").roles("RESOURCE_ADMIN"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private ResultActions update(long id,String body) throws Exception {
        return mockMvc.perform(put("/api/v1/admin/resources/"+id).with(user("admin01").roles("RESOURCE_ADMIN"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private long createId(String body) throws Exception {
        var result=create(body).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("resourceId").asLong();
    }
    private String body(String name,String location,String type,String unit,boolean enabled) throws Exception {
        return objectMapper.writeValueAsString(Map.of("name",name,"location",location,"resourceType",type,"unitLabel",unit,"enabled",enabled));
    }
    private long extraCount(){return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource WHERE id NOT IN (101,102,201,401)",Long.class);}
}
