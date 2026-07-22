package com.zhangyong.agent.admin;

import com.zhangyong.agent.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminApiControllerTest {

    private DemandRepository demandRepo;
    private DemandCommentRepository commentRepo;
    private DemandAuditLogRepository auditRepo;
    private WeComUserDirectory directory;
    private AdminApiController controller;

    @BeforeEach
    void setup() {
        demandRepo = mock(DemandRepository.class);
        commentRepo = mock(DemandCommentRepository.class);
        auditRepo = mock(DemandAuditLogRepository.class);
        directory = mock(WeComUserDirectory.class);
        controller = new AdminApiController(demandRepo, commentRepo, auditRepo, directory);
    }

    private Demand sampleDemand() {
        Demand d = new Demand();
        d.setId(1L);
        d.setUserId("ZhangYong");
        d.setRequirementType("DATA_REPORT");
        d.setTitle("销售月报");
        d.setPriority("P1");
        d.setStatus("ARCHIVED");
        d.setAssigneeUserId(null);
        d.setNotes(null);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return d;
    }

    private HttpServletRequest authedAs(String userId, String name) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(AdminAuthFilter.ATTR_SESSION,
            new AdminSessionService.SessionInfo("tok", userId, name));
        return req;
    }

    @Test
    void listReturnsPagedResponse() {
        Page<Demand> page = new PageImpl<>(List.of(sampleDemand()));
        when(demandRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Map<String, Object> out = controller.list(0, 20, null, null, null, null, null, "created_at", "desc");

        assertEquals(1L, out.get("total"));
        assertEquals(20, out.get("size"));
        List<?> items = (List<?>) out.get("items");
        assertEquals(1, items.size());
        Map<?, ?> row = (Map<?, ?>) items.get(0);
        assertEquals(1L, row.get("id"));
        assertEquals("销售月报", row.get("title"));
    }

    @Test
    void getReturns404WhenMissing() {
        when(demandRepo.findById(99L)).thenReturn(Optional.empty());
        var resp = controller.get(99L);
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void getReturnsDetailWithCommentsAndAudit() {
        Demand d = sampleDemand();
        when(demandRepo.findById(1L)).thenReturn(Optional.of(d));
        when(commentRepo.findByDemandIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(auditRepo.findByDemandIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        var resp = controller.get(1L);
        assertEquals(200, resp.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertNotNull(body);
        assertEquals("销售月报", body.get("title"));
        assertNotNull(body.get("comments"));
        assertNotNull(body.get("audit"));
    }

    @Test
    void patchUpdatesFieldsAndWritesAudit() {
        Demand d = sampleDemand();
        when(demandRepo.findById(1L)).thenReturn(Optional.of(d));
        when(demandRepo.save(any(Demand.class))).thenReturn(d);

        var resp = controller.patch(1L,
            Map.of("status", "IN_PROGRESS", "priority", "P0",
                   "assigneeUserId", "ZhangYong", "notes", "go"),
            authedAs("admin", "管理员"));

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("IN_PROGRESS", d.getStatus());
        assertEquals("P0", d.getPriority());
        assertEquals("ZhangYong", d.getAssigneeUserId());
        assertEquals("go", d.getNotes());

        ArgumentCaptor<DemandAuditLog> cap = ArgumentCaptor.forClass(DemandAuditLog.class);
        verify(auditRepo, times(4)).save(cap.capture());
        List<DemandAuditLog> all = cap.getAllValues();
        // 4 个字段都改了
        assertTrue(all.stream().anyMatch(a -> "status".equals(a.getFieldName())));
        assertTrue(all.stream().anyMatch(a -> "priority".equals(a.getFieldName())));
        assertTrue(all.stream().anyMatch(a -> "assigneeUserId".equals(a.getFieldName())));
        assertTrue(all.stream().anyMatch(a -> "notes".equals(a.getFieldName())));
    }

    @Test
    void patchIgnoresUnchangedFields() {
        Demand d = sampleDemand();
        when(demandRepo.findById(1L)).thenReturn(Optional.of(d));
        when(demandRepo.save(any(Demand.class))).thenReturn(d);

        // status 已经是 ARCHIVED，再传 ARCHIVED 不应该写 audit
        controller.patch(1L, Map.of("status", "ARCHIVED"), authedAs("u", null));
        verify(auditRepo, never()).save(any());
    }

    @Test
    void commentRequiresNonEmptyContent() {
        when(demandRepo.existsById(1L)).thenReturn(true);
        var resp = controller.comment(1L, Map.of("content", "  "), authedAs("u", null));
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void commentPersistsAndReturnsCreated() {
        when(demandRepo.existsById(1L)).thenReturn(true);
        DemandComment saved = new DemandComment();
        saved.setId(99L);
        saved.setDemandId(1L);
        saved.setAuthorUserId("u");
        saved.setAuthorName("管理员");
        saved.setContent("hi");
        when(commentRepo.save(any(DemandComment.class))).thenReturn(saved);

        var resp = controller.comment(1L, Map.of("content", "hi"), authedAs("u", "管理员"));
        assertEquals(200, resp.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertEquals(99L, body.get("id"));
        assertEquals("管理员", body.get("authorName"));
        assertEquals("hi", body.get("content"));
    }

    @Test
    void assignableUsersReturnsListFromDirectory() {
        when(directory.listAll()).thenReturn(List.of(
            new WeComUserDirectory.User("zhangsan", "张三"),
            new WeComUserDirectory.User("lisi", "李四")
        ));
        var out = controller.assignableUsers();
        assertEquals(2, out.size());
        assertEquals("zhangsan", out.get(0).get("userid"));
        assertEquals("张三", out.get(0).get("name"));
    }
}
