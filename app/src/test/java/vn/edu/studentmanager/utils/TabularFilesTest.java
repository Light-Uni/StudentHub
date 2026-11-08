package vn.edu.studentmanager.utils;

import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class TabularFilesTest {
    private Map<String, Object> student() {
        Map<String, Object> row = new LinkedHashMap<>();
        String[] values = {"SV001", "Nguyễn \"An\", Bình", "2004-02-29", "female", "an@example.test", "+84901234567", "12 Nguyễn Trãi\nTP.HCM", "CNTT01", "Công nghệ thông tin", "2022-09-01", "studying"};
        for (int i = 0; i < values.length; i++) row.put(Validation.STUDENT_FIELDS[i], values[i]); return row;
    }
    @Test public void csvRoundTripVietnameseQuotesNewlinesAndPhone() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); TabularFiles.writeCsv(out, Validation.STUDENT_FIELDS, List.of(student()));
        TabularFiles.Preview preview = TabularFiles.preview(out.toByteArray(), false);
        assertEquals(1, preview.records.size()); assertTrue(preview.errors.isEmpty()); assertEquals(student(), preview.records.get(0));
    }
    @Test public void xlsxRoundTripWithLiteralFormulaAndVietnamese() throws Exception {
        Map<String, Object> row = student(); row.put("address", "=HYPERLINK(\"https://example.test\")");
        ByteArrayOutputStream out = new ByteArrayOutputStream(); TabularFiles.writeXlsx(out, Validation.STUDENT_FIELDS, List.of(row));
        TabularFiles.Preview preview = TabularFiles.preview(out.toByteArray(), false);
        assertEquals(row, preview.records.get(0));
    }
    @Test public void csvNeutralizesFormulaInjection() throws Exception {
        Map<String, Object> row = student(); row.put("address", "  =1+1");
        ByteArrayOutputStream out = new ByteArrayOutputStream(); TabularFiles.writeCsv(out, Validation.STUDENT_FIELDS, List.of(row));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("'  =1+1"));
    }
    @Test public void rejectsInvalidDatesAndDuplicateIds() throws Exception {
        Map<String, Object> invalid = student(); invalid.put("birthDate", "2003-02-29");
        ByteArrayOutputStream out = new ByteArrayOutputStream(); TabularFiles.writeCsv(out, Validation.STUDENT_FIELDS, List.of(student(), student(), invalid));
        TabularFiles.Preview preview = TabularFiles.preview(out.toByteArray(), false);
        assertEquals(1, preview.records.size()); assertEquals(2, preview.errors.size());
    }
    @Test public void accentInsensitiveSearch() { assertEquals("dang thi anh", Validation.normalized("Đặng Thị Ánh")); }
    @Test public void rejectsReversedCertificateDates() {
        assertNotNull(Validation.validate(Map.of("certificateId", "C1", "name", "MOS", "issuer", "Certiport", "issueDate", "2025-02-01", "expirationDate", "2025-01-01", "description", ""), true));
    }
    @Test(expected = IOException.class) public void rejectsUnclosedQuote() throws Exception { TabularFiles.readCsv("a,b\n\"unfinished"); }
    @Test(expected = IOException.class) public void rejectsOversizeFile() throws Exception { TabularFiles.readLimited(new ByteArrayInputStream(new byte[TabularFiles.MAX_BYTES + 1])); }
    @Test(expected = IOException.class) public void rejectsMissingHeaders() throws Exception { TabularFiles.preview("name,email\nAn,an@example.test".getBytes(StandardCharsets.UTF_8), false); }
    @Test(expected = IOException.class) public void rejectsTooManyRows() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); TabularFiles.writeCsv(out, Validation.STUDENT_FIELDS, Collections.nCopies(201, student())); TabularFiles.preview(out.toByteArray(), false);
    }
}
