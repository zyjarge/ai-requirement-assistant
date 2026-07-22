package com.zhangyong.agent.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DemandTest {

    @Test
    void setByStringRoutesToCorrectSetter() {
        Demand d = new Demand();
        d.setByString("status", "IN_PROGRESS");
        d.setByString("priority", "P0");
        d.setByString("assigneeUserId", "ZhangYong");
        d.setByString("notes", "test note");
        assertEquals("IN_PROGRESS", d.getStatus());
        assertEquals("P0", d.getPriority());
        assertEquals("ZhangYong", d.getAssigneeUserId());
        assertEquals("test note", d.getNotes());
    }

    @Test
    void setByStringRejectsUnknownField() {
        Demand d = new Demand();
        assertThrows(IllegalArgumentException.class, () -> d.setByString("userId", "evil"));
    }
}
