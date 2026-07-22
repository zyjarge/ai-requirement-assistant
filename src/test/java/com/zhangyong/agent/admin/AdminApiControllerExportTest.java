package com.zhangyong.agent.admin;

import com.zhangyong.agent.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminApiControllerExportTest {

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

    @Test
    void exportWritesUtf8BomAndCsv() {
        Demand d = new Demand();
        d.setId(7L);
        d.setUserId("u");
        d.setTitle("标题");
        d.setStatus("ARCHIVED");
        d.setCreatedAt(LocalDateTime.of(2026, 7, 20, 12, 0));
        when(demandRepo.findById(7L)).thenReturn(Optional.of(d));
        when(commentRepo.findByDemandIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.export(7L);
        assertEquals(200, resp.getStatusCode().value());
        byte[] body = (byte[]) resp.getBody();
        assertNotNull(body);
        // BOM
        assertEquals((byte) 0xEF, body[0]);
        assertEquals((byte) 0xBB, body[1]);
        assertEquals((byte) 0xBF, body[2]);
        String text = new String(body, 3, body.length - 3, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("id,user_id"));
        assertTrue(text.contains("7,u"));
        assertTrue(text.contains("标题"));
        // Content-Disposition
        String disposition = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("demand-7.csv"));
    }

    @Test
    void exportReturns404WhenMissing() {
        when(demandRepo.findById(99L)).thenReturn(Optional.empty());
        ResponseEntity<?> resp = controller.export(99L);
        assertEquals(404, resp.getStatusCode().value());
    }
}
