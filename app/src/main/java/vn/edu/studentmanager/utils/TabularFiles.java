package vn.edu.studentmanager.utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public final class TabularFiles {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private TabularFiles() {}
    public static class Preview {
        public final List<Map<String, Object>> records = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
        public int total;
    }
    public static byte[] readLimited(InputStream input) throws IOException {
        if (input == null) throw new IOException("Không mở được tệp.");
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) != -1) {
            if (out.size() + count > MAX_BYTES) throw new IOException("Tệp vượt quá 5 MB.");
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }
    public static Preview preview(byte[] bytes, boolean certificates) throws Exception {
        boolean zip = bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K';
        List<List<String>> rows = zip ? readXlsx(bytes) : readCsv(new String(bytes, StandardCharsets.UTF_8));
        if (rows.isEmpty()) throw new IOException("Tệp không có dòng tiêu đề.");
        String[] columns = certificates ? Validation.CERTIFICATE_FIELDS : Validation.STUDENT_FIELDS;
        List<String> header = new ArrayList<>(); for (String h : rows.get(0)) header.add(h.trim());
        if (new HashSet<>(header).size() != header.size() || !new HashSet<>(header).equals(new HashSet<>(Arrays.asList(columns)))) {
            throw new IOException("Tiêu đề cần gồm đúng các cột: " + String.join(", ", columns));
        }
        Preview preview = new Preview(); Set<String> seen = new HashSet<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> cells = rows.get(i);
            if (cells.stream().allMatch(String::isBlank)) continue;
            preview.total++;
            if (preview.total > 200) throw new IOException("Mỗi lần chỉ nhập tối đa 200 dòng. Hãy chia nhỏ tệp.");
            if (cells.size() > header.size()) { preview.errors.add("Dòng " + (i + 1) + ": thừa cột."); continue; }
            Map<String, Object> record = new LinkedHashMap<>();
            for (int j = 0; j < header.size(); j++) {
                String value = j < cells.size() ? cells.get(j).trim() : "";
                if (!zip && value.startsWith("'") && formulaLike(value.substring(1))) value = value.substring(1);
                record.put(header.get(j), value);
            }
            String error = Validation.validate(record, certificates);
            String id = String.valueOf(record.get(columns[0]));
            if (error == null && !seen.add(id)) error = "Mã trùng trong tệp: " + id;
            if (error != null) preview.errors.add("Dòng " + (i + 1) + ": " + error);
            else preview.records.add(record);
        }
        return preview;
    }
    public static List<List<String>> readCsv(String input) throws IOException {
        if (input.startsWith("\uFEFF")) input = input.substring(1);
        if (input.indexOf('\uFFFD') >= 0) throw new IOException("Tệp cần được lưu bằng mã hóa UTF-8.");
        List<List<String>> rows = new ArrayList<>(); List<String> row = new ArrayList<>(); StringBuilder cell = new StringBuilder();
        boolean quoted = false, closed = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (quoted) {
                if (c == '"') { if (i + 1 < input.length() && input.charAt(i + 1) == '"') { cell.append('"'); i++; } else { quoted = false; closed = true; } }
                else cell.append(c);
            } else if (c == '"' && cell.length() == 0 && !closed) quoted = true;
            else if (c == ',' || c == '\n' || c == '\r') {
                row.add(cell.toString()); cell.setLength(0); closed = false;
                if (row.size() > 64) throw new IOException("Tệp có quá nhiều cột.");
                if (c != ',') {
                    rows.add(row); row = new ArrayList<>();
                    if (c == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') i++;
                    if (rows.size() > 1000) throw new IOException("Tệp có quá nhiều dòng.");
                }
            } else {
                if (closed || c == '"') throw new IOException("Dấu ngoặc kép trong CSV không hợp lệ.");
                cell.append(c);
            }
            if (cell.length() > 32767) throw new IOException("Một ô vượt quá 32.767 ký tự.");
        }
        if (quoted) throw new IOException("CSV thiếu dấu ngoặc kép đóng.");
        if (cell.length() > 0 || closed || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }
    private static boolean formulaLike(String value) {
        String stripped = value.stripLeading();
        return !value.isEmpty() && ("\t\r\n".indexOf(value.charAt(0)) >= 0 || (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0));
    }
    public static void writeCsv(OutputStream output, String[] columns, List<Map<String, Object>> records) throws IOException {
        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8); writer.write('\uFEFF');
        writeCsvRow(writer, Arrays.asList(columns));
        for (Map<String, Object> record : records) {
            List<String> cells = new ArrayList<>(); for (String key : columns) cells.add(Objects.toString(record.get(key), "")); writeCsvRow(writer, cells);
        }
        writer.flush();
    }
    private static void writeCsvRow(Writer writer, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) writer.write(','); String cell = cells.get(i); if (formulaLike(cell)) cell = "'" + cell;
            writer.write('"'); writer.write(cell.replace("\"", "\"\"")); writer.write('"');
        } writer.write("\r\n");
    }
    private static String xml(String value) {
        return value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
    private static String columnName(int index) {
        StringBuilder name = new StringBuilder(); do { name.insert(0, (char) ('A' + index % 26)); index = index / 26 - 1; } while (index >= 0); return name.toString();
    }
    private static void entry(ZipOutputStream zip, String path, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(path)); zip.write(value.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
    }
    public static void writeXlsx(OutputStream output, String[] columns, List<Map<String, Object>> records) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(output);
        entry(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
        entry(zip, "_rels/.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        entry(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Du lieu\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        entry(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
        zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        Writer writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" state=\"frozen\"/></sheetView></sheetViews><cols>");
        writer.write("<col min=\"1\" max=\"" + columns.length + "\" width=\"24\" customWidth=\"1\"/></cols><sheetData>");
        for (int r = 0; r <= records.size(); r++) {
            writer.write("<row r=\"" + (r + 1) + "\">");
            for (int c = 0; c < columns.length; c++) {
                String value = r == 0 ? columns[c] : Objects.toString(records.get(r - 1).get(columns[c]), "");
                writer.write("<c r=\"" + columnName(c) + (r + 1) + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">" + xml(value) + "</t></is></c>");
            } writer.write("</row>");
        }
        writer.write("</sheetData><autoFilter ref=\"A1:" + columnName(columns.length - 1) + (records.size() + 1) + "\"/></worksheet>"); writer.flush(); zip.closeEntry(); zip.finish(); zip.flush();
    }
    private static XmlPullParser parser(byte[] data) throws Exception {
        if (data == null) throw new IOException("Excel thiếu thành phần bắt buộc.");
        String xml = new String(data, StandardCharsets.UTF_8);
        if (xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) throw new IOException("Tệp XML có cấu trúc không được hỗ trợ.");
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(); factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser(); parser.setInput(new StringReader(xml)); return parser;
    }
    public static List<List<String>> readXlsx(byte[] bytes) throws Exception {
        Map<String, byte[]> files = new HashMap<>(); int total = 0, entries = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry; byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 500 || entry.getName().contains("..")) throw new IOException("Cấu trúc Excel không hợp lệ.");
                ByteArrayOutputStream out = new ByteArrayOutputStream(); int count;
                while ((count = zip.read(buffer)) != -1) {
                    total += count; if (total > 20 * 1024 * 1024) throw new IOException("Tệp Excel quá lớn sau khi giải nén."); out.write(buffer, 0, count);
                }
                if (files.put(entry.getName(), out.toByteArray()) != null) throw new IOException("Excel có thành phần trùng tên.");
            }
        }
        XmlPullParser workbook = parser(files.get("xl/workbook.xml")); String relation = null;
        for (int event = workbook.getEventType(); event != XmlPullParser.END_DOCUMENT; event = workbook.next()) {
            if (event == XmlPullParser.START_TAG && "sheet".equals(workbook.getName())) { relation = workbook.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id"); break; }
        }
        XmlPullParser relationships = parser(files.get("xl/_rels/workbook.xml.rels")); String target = null;
        for (int event = relationships.getEventType(); event != XmlPullParser.END_DOCUMENT; event = relationships.next()) {
            if (event == XmlPullParser.START_TAG && "Relationship".equals(relationships.getName()) && Objects.equals(relation, relationships.getAttributeValue(null, "Id"))) {
                target = relationships.getAttributeValue(null, "Target");
                if ("External".equals(relationships.getAttributeValue(null, "TargetMode"))) throw new IOException("Không hỗ trợ trang tính ngoài tệp."); break;
            }
        }
        if (target == null || target.contains("..")) throw new IOException("Không tìm thấy trang tính đầu tiên.");
        String sheetPath = target.startsWith("/") ? target.substring(1) : "xl/" + target;
        List<String> shared = new ArrayList<>();
        if (files.containsKey("xl/sharedStrings.xml")) {
            XmlPullParser p = parser(files.get("xl/sharedStrings.xml")); StringBuilder current = new StringBuilder();
            for (int event = p.getEventType(); event != XmlPullParser.END_DOCUMENT; event = p.next()) {
                if (event == XmlPullParser.START_TAG && "si".equals(p.getName())) current.setLength(0);
                if (event == XmlPullParser.START_TAG && "t".equals(p.getName())) current.append(p.nextText());
                if (event == XmlPullParser.END_TAG && "si".equals(p.getName())) { shared.add(current.toString()); if (shared.size() > 20000) throw new IOException("Quá nhiều chuỗi trong Excel."); }
            }
        }
        List<List<String>> rows = new ArrayList<>(); List<String> row = null;
        XmlPullParser p = parser(files.get(sheetPath)); int col = 0; String type = ""; StringBuilder value = new StringBuilder();
        for (int event = p.getEventType(); event != XmlPullParser.END_DOCUMENT; event = p.next()) {
            if (event == XmlPullParser.START_TAG) {
                switch (p.getName()) {
                    case "row" -> {
                        String rowIndex = p.getAttributeValue(null, "r");
                        int expected;
                        try { expected = rowIndex == null ? rows.size() + 1 : Integer.parseInt(rowIndex); }
                        catch (NumberFormatException e) { throw new IOException("Chỉ số dòng Excel không hợp lệ."); }
                        if (expected <= rows.size() || expected > 1000) throw new IOException("Chỉ số dòng Excel không hợp lệ hoặc vượt quá 1.000 dòng.");
                        while (rows.size() < expected - 1) rows.add(new ArrayList<>());
                        row = new ArrayList<>();
                    }
                    case "c" -> {
                        String ref = p.getAttributeValue(null, "r"); col = 0;
                        if (ref == null) col = row == null ? 0 : row.size();
                        else { for (int i = 0; i < ref.length() && Character.isLetter(ref.charAt(i)); i++) col = col * 26 + Character.toUpperCase(ref.charAt(i)) - 'A' + 1; col--; }
                        if (col < 0 || col > 63) throw new IOException("Excel có quá nhiều cột.");
                        type = p.getAttributeValue(null, "t"); value.setLength(0);
                    }
                    case "v", "t" -> value.append(p.nextText());
                    case "f" -> throw new IOException("Không nhập ô công thức. Hãy chuyển thành giá trị văn bản trước.");
                }
            } else if (event == XmlPullParser.END_TAG) {
                if ("c".equals(p.getName()) && row != null) {
                    String cell = value.toString();
                    if ("s".equals(type)) {
                        try { cell = shared.get(Integer.parseInt(cell)); } catch (RuntimeException e) { throw new IOException("Chỉ mục chuỗi trong Excel không hợp lệ."); }
                    }
                    while (row.size() <= col) row.add(""); row.set(col, cell);
                } else if ("row".equals(p.getName()) && row != null) { rows.add(row); row = null; }
            }
        }
        return rows;
    }
}
