package com.zhangyong.agent.admin;

import com.zhangyong.agent.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理端 API
 *
 * 端点：
 *   GET    /admin/api/requirements                分页列表
 *   GET    /admin/api/requirements/{id}           详情
 *   PATCH  /admin/api/requirements/{id}           编辑字段
 *   POST   /admin/api/requirements/{id}/comments  加评论
 *   GET    /admin/api/assignable-users            企微成员
 *   GET    /admin/api/requirements/{id}/export    CSV 导出
 */
@Slf4j
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminApiController {

    private final DemandRepository demandRepository;
    private final DemandCommentRepository commentRepository;
    private final DemandAuditLogRepository auditRepository;
    private final WeComUserDirectory userDirectory;

    @GetMapping("/requirements")
    public Map<String, Object> list(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String assignee,
        @RequestParam(required = false) String source,
        @RequestParam(required = false) String department,
        @RequestParam(required = false) String sprintVersion,
        @RequestParam(defaultValue = "created_at") String sort,
        @RequestParam(defaultValue = "desc") String order
    ) {
        size = Math.min(Math.max(size, 1), 100);
        offset = Math.max(offset, 0);
        int page = offset / size;
        Sort.Direction dir = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pr = PageRequest.of(page, size, Sort.by(dir, mapSortField(sort)));

        Specification<Demand> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim() + "%";
                ps.add(cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("rawInput"), like)
                ));
            }
            if (status != null && !status.isBlank()) ps.add(cb.equal(root.get("status"), status));
            if (priority != null && !priority.isBlank()) ps.add(cb.equal(root.get("priority"), priority));
            if (type != null && !type.isBlank()) ps.add(cb.equal(root.get("requirementType"), type));
            if (assignee != null && !assignee.isBlank()) ps.add(cb.equal(root.get("assigneeUserId"), assignee));
            if (source != null && !source.isBlank()) ps.add(cb.equal(root.get("source"), source));
            if (department != null && !department.isBlank()) ps.add(cb.equal(root.get("department"), department));
            if (sprintVersion != null && !sprintVersion.isBlank()) ps.add(cb.equal(root.get("sprintVersion"), sprintVersion));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<Demand> p = demandRepository.findAll(spec, pr);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Demand d : p.getContent()) items.add(toSummary(d));
        return Map.of("total", p.getTotalElements(), "size", size, "offset", offset, "items", items);
    }

    private String mapSortField(String s) {
        return switch (s) {
            case "priority" -> "priority";
            case "status" -> "status";
            case "type" -> "requirementType";
            case "title" -> "title";
            case "department" -> "department";
            case "source" -> "source";
            case "sprintVersion" -> "sprintVersion";
            case "deadline" -> "deadline";
            case "estimatedHours" -> "estimatedHours";
            case "created_at", "createdAt" -> "createdAt";
            default -> "createdAt";
        };
    }

    @GetMapping("/requirements/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return demandRepository.findById(id)
            .<ResponseEntity<?>>map(d -> ResponseEntity.ok(toDetail(d)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found")));
    }

    @PatchMapping("/requirements/{id}")
    @Transactional
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                   HttpServletRequest req) {
        Optional<Demand> opt = demandRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        Demand d = opt.get();
        SessionCtx ctx = sessionOf(req);

        applyAndAudit(d, "status", d.getStatus(), asString(body.get("status")), ctx);
        applyAndAudit(d, "priority", d.getPriority(), asString(body.get("priority")), ctx);
        applyAndAudit(d, "assigneeUserId", d.getAssigneeUserId(), asString(body.get("assigneeUserId")), ctx);
        applyAndAudit(d, "notes", d.getNotes(), asString(body.get("notes")), ctx);
        applyAndAudit(d, "department", d.getDepartment(), asString(body.get("department")), ctx);
        applyAndAudit(d, "source", d.getSource(), asString(body.get("source")), ctx);
        applyAndAudit(d, "categoryTags", d.getCategoryTags(), asString(body.get("categoryTags")), ctx);
        applyAndAudit(d, "sprintVersion", d.getSprintVersion(), asString(body.get("sprintVersion")), ctx);
        applyAndAudit(d, "relatedDemandIds", d.getRelatedDemandIds(), asString(body.get("relatedDemandIds")), ctx);

        // 数值/日期字段单独处理
        if (body.containsKey("estimatedHours")) {
            Object v = body.get("estimatedHours");
            Double newVal = v instanceof Number ? ((Number) v).doubleValue() : null;
            if (!Objects.equals(d.getEstimatedHours(), newVal)) {
                String before = d.getEstimatedHours() == null ? null : d.getEstimatedHours().toString();
                String after = newVal == null ? null : newVal.toString();
                d.setEstimatedHours(newVal);
                saveAudit(d.getId(), "estimatedHours", before, after, ctx);
            }
        }
        if (body.containsKey("deadline")) {
            String v = asString(body.get("deadline"));
            LocalDate newVal = v == null || v.isEmpty() ? null : LocalDate.parse(v);
            if (!Objects.equals(d.getDeadline(), newVal)) {
                String before = d.getDeadline() == null ? null : d.getDeadline().toString();
                d.setDeadline(newVal);
                saveAudit(d.getId(), "deadline", before, v, ctx);
            }
        }
        if (body.containsKey("feedbackRating")) {
            Object v = body.get("feedbackRating");
            Integer newVal = v instanceof Number ? ((Number) v).intValue() : null;
            if (!Objects.equals(d.getFeedbackRating(), newVal)) {
                String before = d.getFeedbackRating() == null ? null : d.getFeedbackRating().toString();
                String after = newVal == null ? null : newVal.toString();
                d.setFeedbackRating(newVal);
                saveAudit(d.getId(), "feedbackRating", before, after, ctx);
            }
        }
        // closed_at: 当状态改为 DONE 时自动设，也可以手动传
        if (body.containsKey("closedAt")) {
            String v = asString(body.get("closedAt"));
            LocalDateTime newVal = v == null || v.isEmpty() ? null : LocalDateTime.parse(v);
            d.setClosedAt(newVal);
        }

        demandRepository.save(d);
        return ResponseEntity.ok(toSummary(d));
    }

    private void applyAndAudit(Demand d, String field, String before, String after, SessionCtx ctx) {
        if (after == null) return;
        if (Objects.equals(before, after)) return;
        d.setByString(field, after);
        saveAudit(d.getId(), field, before, after, ctx);
    }

    private void saveAudit(Long demandId, String field, String before, String after, SessionCtx ctx) {
        DemandAuditLog log = new DemandAuditLog();
        log.setDemandId(demandId);
        log.setActorUserId(ctx.userId);
        log.setActorName(ctx.displayName);
        log.setFieldName(field);
        log.setBeforeValue(before);
        log.setAfterValue(after);
        auditRepository.save(log);
    }

    @PostMapping("/requirements/{id}/comments")
    public ResponseEntity<?> comment(@PathVariable Long id, @RequestBody Map<String, String> body,
                                     HttpServletRequest req) {
        if (!demandRepository.existsById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "content_required"));
        SessionCtx ctx = sessionOf(req);
        DemandComment c = new DemandComment();
        c.setDemandId(id);
        c.setAuthorUserId(ctx.userId);
        c.setAuthorName(ctx.displayName);
        c.setContent(content);
        c = commentRepository.save(c);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", c.getId());
        out.put("authorUserId", c.getAuthorUserId());
        out.put("authorName", c.getAuthorName() == null ? "" : c.getAuthorName());
        out.put("content", c.getContent());
        out.put("createdAt", c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/assignable-users")
    public List<Map<String, String>> assignableUsers() {
        List<Map<String, String>> out = new ArrayList<>();
        for (WeComUserDirectory.User u : userDirectory.listAll()) {
            out.add(Map.of("userid", u.userid == null ? "" : u.userid,
                            "name", u.name == null ? "" : u.name));
        }
        return out;
    }

    @GetMapping(value = "/requirements/{id}/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<?> export(@PathVariable Long id) {
        Optional<Demand> opt = demandRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        Demand d = opt.get();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"id","user_id","department","source","requirement_type","title","priority","status",
            "assignee_user_id","sprint_version","estimated_hours","deadline","category_tags","related_demand_ids",
            "feedback_rating","closed_at","raw_input","business_context","user_role","acceptance_criteria",
            "notes","created_at","updated_at"});
        rows.add(new String[]{
            String.valueOf(d.getId()), n(d.getUserId()), n(d.getDepartment()), n(d.getSource()),
            n(d.getRequirementType()), n(d.getTitle()), n(d.getPriority()), n(d.getStatus()),
            n(d.getAssigneeUserId()), n(d.getSprintVersion()),
            d.getEstimatedHours() == null ? "" : d.getEstimatedHours().toString(),
            d.getDeadline() == null ? "" : d.getDeadline().toString(),
            n(d.getCategoryTags()), n(d.getRelatedDemandIds()),
            d.getFeedbackRating() == null ? "" : d.getFeedbackRating().toString(),
            d.getClosedAt() == null ? "" : d.getClosedAt().toString(),
            n(d.getRawInput()), n(d.getBusinessContext()), n(d.getUserRole()),
            n(d.getAcceptanceCriteria()), n(d.getNotes()),
            d.getCreatedAt() == null ? "" : d.getCreatedAt().toString(),
            d.getUpdatedAt() == null ? "" : d.getUpdatedAt().toString(),
        });
        for (DemandComment c : commentRepository.findByDemandIdOrderByCreatedAtDesc(d.getId())) {
            rows.add(new String[]{"comment:" + c.getId(), n(c.getAuthorUserId()), "", "",
                "", "", "", "", "", "", "", "", "", "", "", "",
                n(c.getContent()), "", "", "", "", c.getCreatedAt() == null ? "" : c.getCreatedAt().toString(), ""});
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ExportUtil.writeCsv(rows, baos);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"demand-" + d.getId() + ".csv\"; filename*=UTF-8''demand-" + d.getId() + ".csv");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("export failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "export_failed"));
        }
    }

    private Map<String, Object> toSummary(Demand d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("userId", d.getUserId());
        m.put("department", d.getDepartment());
        m.put("source", d.getSource());
        m.put("type", d.getRequirementType());
        m.put("title", d.getTitle());
        m.put("priority", d.getPriority());
        m.put("status", d.getStatus());
        m.put("assigneeUserId", d.getAssigneeUserId());
        m.put("sprintVersion", d.getSprintVersion());
        m.put("estimatedHours", d.getEstimatedHours());
        m.put("deadline", d.getDeadline() == null ? null : d.getDeadline().toString());
        m.put("createdAt", d.getCreatedAt() == null ? null : d.getCreatedAt().toString());
        m.put("updatedAt", d.getUpdatedAt() == null ? null : d.getUpdatedAt().toString());
        return m;
    }

    private Map<String, Object> toDetail(Demand d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("userId", d.getUserId());
        m.put("department", d.getDepartment());
        m.put("source", d.getSource());
        m.put("type", d.getRequirementType());
        m.put("title", d.getTitle());
        m.put("rawInput", d.getRawInput());
        m.put("businessContext", d.getBusinessContext());
        m.put("userRole", d.getUserRole());
        m.put("acceptanceCriteria", d.getAcceptanceCriteria());
        m.put("priority", d.getPriority());
        m.put("status", d.getStatus());
        m.put("structured", d.getStructured());
        m.put("qaHistory", d.getQaHistory());
        m.put("assigneeUserId", d.getAssigneeUserId());
        m.put("notes", d.getNotes());
        m.put("categoryTags", d.getCategoryTags());
        m.put("estimatedHours", d.getEstimatedHours());
        m.put("deadline", d.getDeadline() == null ? null : d.getDeadline().toString());
        m.put("sprintVersion", d.getSprintVersion());
        m.put("relatedDemandIds", d.getRelatedDemandIds());
        m.put("attachments", d.getAttachments());
        m.put("closedAt", d.getClosedAt() == null ? null : d.getClosedAt().toString());
        m.put("feedbackRating", d.getFeedbackRating());
        m.put("createdAt", d.getCreatedAt() == null ? null : d.getCreatedAt().toString());
        m.put("updatedAt", d.getUpdatedAt() == null ? null : d.getUpdatedAt().toString());

        List<Map<String, Object>> comments = new ArrayList<>();
        for (DemandComment c : commentRepository.findByDemandIdOrderByCreatedAtDesc(d.getId())) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.getId());
            cm.put("authorUserId", c.getAuthorUserId());
            cm.put("authorName", c.getAuthorName());
            cm.put("content", c.getContent());
            cm.put("createdAt", c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
            comments.add(cm);
        }
        m.put("comments", comments);

        List<Map<String, Object>> audit = new ArrayList<>();
        for (DemandAuditLog a : auditRepository.findByDemandIdOrderByCreatedAtDesc(d.getId())) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.getId());
            am.put("actorUserId", a.getActorUserId());
            am.put("actorName", a.getActorName());
            am.put("fieldName", a.getFieldName());
            am.put("beforeValue", a.getBeforeValue());
            am.put("afterValue", a.getAfterValue());
            am.put("createdAt", a.getCreatedAt() == null ? null : a.getCreatedAt().toString());
            audit.add(am);
        }
        m.put("audit", audit);
        return m;
    }

    private static String n(String s) { return s == null ? "" : s; }
    private static String asString(Object o) { return o == null ? null : o.toString(); }

    private SessionCtx sessionOf(HttpServletRequest req) {
        Object attr = req.getAttribute(AdminAuthFilter.ATTR_SESSION);
        if (attr instanceof AdminSessionService.SessionInfo s)
            return new SessionCtx(s.userId(), s.displayName());
        return new SessionCtx("anonymous", null);
    }

    private record SessionCtx(String userId, String displayName) { }
}
