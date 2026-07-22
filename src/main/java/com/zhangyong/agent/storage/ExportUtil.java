package com.zhangyong.agent.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 简单 CSV 导出，UTF-8 + BOM（Excel 中文不乱码）。
 */
public final class ExportUtil {

    private ExportUtil() {}

    public static void writeCsv(List<String[]> rows, OutputStream out) throws IOException {
        // UTF-8 BOM，Excel 打开时识别为 UTF-8
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) w.write(',');
                    w.write(escape(row[i] == null ? "" : row[i]));
                }
                w.write("\r\n");
            }
            w.flush();
        }
    }

    private static String escape(String s) {
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!needQuote) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
