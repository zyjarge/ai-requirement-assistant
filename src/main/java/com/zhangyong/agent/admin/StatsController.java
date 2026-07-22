package com.zhangyong.agent.admin;

import com.zhangyong.agent.storage.Demand;
import com.zhangyong.agent.storage.DemandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计 API
 */
@RestController
@RequestMapping("/admin/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DemandRepository demandRepository;

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        long total = demandRepository.count();
        long todayNew = countByCreatedDate(LocalDate.now());
        long inProgress = demandRepository.findByStatusOrderByCreatedAtDesc("IN_PROGRESS").size();
        long completed = demandRepository.findByStatusOrderByCreatedAtDesc("DONE").size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("todayNew", todayNew);
        result.put("inProgress", inProgress);
        result.put("completed", completed);
        return result;
    }

    @GetMapping("/by-type")
    public List<Map<String, Object>> byType() {
        return groupBy("requirementType");
    }

    @GetMapping("/by-status")
    public List<Map<String, Object>> byStatus() {
        return groupBy("status");
    }

    @GetMapping("/by-department")
    public List<Map<String, Object>> byDepartment() {
        return groupBy("department");
    }

    @GetMapping("/by-priority")
    public List<Map<String, Object>> byPriority() {
        return groupBy("priority");
    }

    @GetMapping("/trend")
    public List<Map<String, Object>> trend() {
        // 最近 30 天，每天新增数
        List<Demand> all = demandRepository.findAll();
        Map<String, Long> byDate = all.stream()
            .filter(d -> d.getCreatedAt() != null)
            .collect(Collectors.groupingBy(
                d -> d.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                Collectors.counting()
            ));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            point.put("count", byDate.getOrDefault(date, 0L));
            result.add(point);
        }
        return result;
    }

    private long countByCreatedDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return demandRepository.findAll().stream()
            .filter(d -> d.getCreatedAt() != null
                && !d.getCreatedAt().isBefore(startOfDay)
                && !d.getCreatedAt().isAfter(endOfDay))
            .count();
    }

    private List<Map<String, Object>> groupBy(String fieldName) {
        List<Demand> all = demandRepository.findAll();
        Map<String, Long> counts = new HashMap<>();
        for (Demand d : all) {
            String value = switch (fieldName) {
                case "requirementType" -> d.getRequirementType();
                case "status" -> d.getStatus();
                case "department" -> d.getDepartment();
                case "priority" -> d.getPriority();
                default -> null;
            };
            if (value == null || value.isEmpty()) continue;
            counts.merge(value, 1L, Long::sum);
        }

        return counts.entrySet().stream()
            .map(e -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("field", fieldName);
                item.put("label", translateLabel(fieldName, e.getKey()));
                item.put("count", e.getValue());
                return item;
            })
            .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
            .collect(Collectors.toList());
    }

    private String translateLabel(String field, String value) {
        return switch (field) {
            case "requirementType" -> switch (value) {
                case "DATA_REPORT" -> "数据报表";
                case "BUSINESS_FLOW" -> "业务流程";
                case "API_INTEGRATION" -> "接口对接";
                case "UI_CHANGE" -> "UI 调整";
                case "RULE_CHANGE" -> "规则变更";
                default -> "其他";
            };
            case "status" -> switch (value) {
                case "SUBMITTED" -> "已提交";
                case "QUESTIONING" -> "追问中";
                case "IN_PROGRESS" -> "进行中";
                case "ARCHIVED" -> "已归档";
                case "DONE" -> "完成";
                default -> value;
            };
            case "priority" -> value;
            default -> value;
        };
    }
}
