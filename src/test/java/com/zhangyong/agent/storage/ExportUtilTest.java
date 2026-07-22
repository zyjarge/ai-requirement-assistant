package com.zhangyong.agent.storage;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportUtilTest {

    @Test
    void writesUtf8BomAndRows() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"id", "name"});
        rows.add(new String[]{"1", "张三"});
        ExportUtil.writeCsv(rows, baos);
        byte[] all = baos.toByteArray();
        assertEquals((byte) 0xEF, all[0]);
        assertEquals((byte) 0xBB, all[1]);
        assertEquals((byte) 0xBF, all[2]);
        String body = new String(all, 3, all.length - 3, StandardCharsets.UTF_8);
        assertTrue(body.contains("id,name"));
        assertTrue(body.contains("1,张三"));
    }

    @Test
    void escapesQuotesAndCommas() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"a,b", "he said \"hi\""});
        ExportUtil.writeCsv(rows, baos);
        String body = baos.toString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"a,b\""));
        assertTrue(body.contains("\"he said \"\"hi\"\"\""));
    }

    @Test
    void nullBecomesEmpty() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"a", null, "b"});
        ExportUtil.writeCsv(rows, baos);
        String body = baos.toString(StandardCharsets.UTF_8);
        assertTrue(body.startsWith("\uFEFF"));
        assertTrue(body.contains("a,,b"));
    }
}
