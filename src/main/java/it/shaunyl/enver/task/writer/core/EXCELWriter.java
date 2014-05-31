package it.shaunyl.enver.task.writer.core;

import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.task.writer.IEnverWriter;
import it.shaunyl.enver.util.DatabaseUtil;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import lombok.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 * @version 1.2
 */
public class EXCELWriter implements IEnverWriter {

    private FileOutputStream fileOutputStream;

    private Font dataFont, headFont;

    @Setter @Getter
    private Workbook workbook;

    private Sheet sheet;

    private String format;

    private String parameterFile = "config/excelFormatting.properties";

    private int rowlimit = 0, start, end;

    /**
     * The default EXCEL format.
     */
    public static final String DEFAULT_FORMAT = "xls";

    /**
     * The row limit for EXCEL 2003 xls files.
     */
    public static final int EXCEL_XLS_ROW_LIMIT = 65536;

    /**
     * The row limit for EXCEL 2007+ xlsx files.
     */
    public static final int EXCEL_XLSX_ROW_LIMIT = 1048575;

    /**
     * Constructs EXCELWriter using XLS format.
     *
     * @param writer the writer to an underlying EXCEL source.
     */
    public EXCELWriter(File file, Sheet sheet) throws IOException {
        this(file, sheet, DEFAULT_FORMAT);
    }

    /**
     * Constructs EXCELWriter with supplied format.
     *
     * @param writer the writer to an underlying EXCEL source.
     */
    public EXCELWriter(File file, Sheet sheet, String format) throws IOException {
        this(file, sheet, format, -1, -1);
    }

    /**
     * Constructs EXCELWriter with supplied EXCEL format and sheet, and range of
     * lines to be grabbed.
     *
     * @param file the abstract representation of the EXCEL file location.
     * @param sheet the sheet to an underlying EXCEL workbook.
     * @param format the format of excel file.
     * @param start the line number to skip for start exporting.
     * @param end the last line number to retrieve for end exporting.
     */
    public EXCELWriter(File file, Sheet sheet, String format, int start, int end) throws IOException {
        this.fileOutputStream = new FileOutputStream(file);
        this.sheet = sheet;
        this.workbook = sheet.getWorkbook();
        this.format = format;
        this.rowlimit = format.equals("xls") ? EXCEL_XLS_ROW_LIMIT : EXCEL_XLSX_ROW_LIMIT;
        this.start = start;
        this.end = end;
    }

    /**
     * Writes the entire ResultSet to an EXCEL file.
     *
     * The caller is responsible for closing the ResultSet.
     *
     * @param rs the recordset to write
     * @param includeColumnNames true if you want column names in the output,
     * false otherwise
     *
     */
    @Override
    public void writeAll(@NonNull final ResultSet rs, boolean includeColumnNames)
            throws SQLException, IOException, TaskException {

        ResultSetMetaData metadata = rs.getMetaData();

        if (includeColumnNames) {
            writeColumnNames(metadata);
        }

        int columnCount = metadata.getColumnCount();

        int k = includeColumnNames ? 1 : 0;
        this.sheet = workbook.getSheet(sheet.getSheetName());

        if (!rs.isBeforeFirst()) {
            throw new TaskException("Warnings: The table is empty. The EXCEL file was not created.\n");
        } else {
            rs.beforeFirst();
        }

        int r = k;
        while (rs.next()) {
            if (k + 1 > this.rowlimit) {
                throw new TaskException(String.format("EXCEL: Outside allowable range (0, %d). Data was truncated\n", rowlimit));
            }

            if (r < this.start && this.start != -1) {
                r++;
                k++;
                continue;
            }

            if (k > this.end && this.end != -1) {
                throw new TaskException(String.format("ENVER: Reached the last line request: %d.\n", this.end));
            }

            String[] nextLine = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                nextLine[i] = DatabaseUtil.getColumnValue(rs, metadata.getColumnType(i + 1), i + 1);
            }

            Row row = sheet.createRow((k++) - r + 1);
            writeNext(nextLine, row);

//            // test
//            int mb = 1024 * 1024;
//            Runtime runtime = Runtime.getRuntime();
//            //Print used memory
//            System.out.println("--\n" + k + ") Memory Usage (MB): "
//                    + (runtime.totalMemory() - runtime.freeMemory()) / mb + "/" + runtime.totalMemory() / mb);
        }
    }

    /**
     * Writes the entire list to an EXCEL file. The list is assumed to be a
     * String[].
     *
     * @param lines a List of String[], with each String[] representing a line
     * of the file.
     */
    public void writeAll(List allLines) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine a string array with each comma-separated element as a
     * separate entry.
     */
    public void writeNext(String[] nextLine, Row row) {

        if (nextLine == null) {
            return;
        }

        for (int i = 0; i < nextLine.length; i++) {

            String nextElement = nextLine[i];

            Cell cell = row.createCell(i);
            cell.setCellValue(nextElement == null ? "" : nextElement);
            if (format.equals("xlsx")) {
                cell.setCellStyle(sheet.getColumnStyle(i));
            }
        }
    }

    protected void writeColumnNames(@NonNull final ResultSetMetaData metadata)
            throws SQLException {

        Row row = sheet.createRow(0);
        this.setFonts();

        int columnCount = metadata.getColumnCount();

        CellStyle style = workbook.createCellStyle();
        style.setFont(dataFont);
        for (int i = 0; i < columnCount; i++) {
            sheet.setDefaultColumnStyle(i, style);
        }
        style = workbook.createCellStyle();
        style.setFont(headFont);
        String[] nextLine = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            nextLine[i] = metadata.getColumnName(i + 1);
            Cell cell = row.createCell(i);
            cell.setCellValue(nextLine[i] == null ? "" : nextLine[i]);
            cell.setCellStyle(style);
            sheet.autoSizeColumn(i);
        }
    }

    private void setFonts() {
        headFont = workbook.createFont();
        headFont.setFontHeightInPoints((short) 18);
        headFont.setFontName("Calibri");
        headFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

        dataFont = workbook.createFont();
        dataFont.setFontHeightInPoints((short) 13);
        dataFont.setFontName("Calibri");

        File propertiesFile = new File(this.parameterFile);
        if (propertiesFile.exists()) {
            try {
                this.parseExcelParameterFile(new FileInputStream(propertiesFile));

            } catch (FileNotFoundException e) {
                throw new UnexpectedEnverException(e.getMessage());
            }
        }
    }

    private void parseExcelParameterFile(InputStream propertiesInputStream) {
        Properties props = new Properties();
        try {
            props.load(propertiesInputStream);
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage());
        }

        Pattern regexHead = Pattern.compile("font.(data|head).*(.*|height|bold|strikeout|underline|italic)");

        for (Map.Entry entry : props.entrySet()) {
            String value = entry.getValue().toString().trim();
            String key = (String) entry.getKey();
            Matcher matcher = regexHead.matcher(key);

            if (matcher.find()) {
                String scope = matcher.group(1);
                if (key.equals(String.format("font.%s.height", scope))) {
                    if (scope.equalsIgnoreCase("head")) {
                        headFont.setFontHeightInPoints(Short.parseShort(value));
                    } else {
                        dataFont.setFontHeightInPoints(Short.parseShort(value));
                    }
                } else if (key.equals(String.format("font.%s.bold", scope))) {
                    Boolean bold = Boolean.parseBoolean(value);
                    if (bold) {
                        if (scope.equalsIgnoreCase("head")) {
                            headFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
                        } else {
                            dataFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
                        }
                    } else {
                        if (scope.equalsIgnoreCase("head")) {
                            headFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
                        } else {
                            dataFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
                        }
                    }
                } else if (key.equals(String.format("font.%s", scope))) {
                    if (scope.equalsIgnoreCase("head")) {
                        headFont.setFontName(value);
                    } else {
                        dataFont.setFontName(value);
                    }
                } else if (key.equals(String.format("font.%s.strikeout", scope))) {
                    if (scope.equalsIgnoreCase("head")) {
                        headFont.setStrikeout(Boolean.parseBoolean(value));
                    } else {
                        dataFont.setStrikeout(Boolean.parseBoolean(value));
                    }
                } else if (key.equals(String.format("font.%s.italic", scope))) {
                    if (scope.equalsIgnoreCase("head")) {
                        headFont.setItalic(Boolean.parseBoolean(value));
                    } else {
                        dataFont.setItalic(Boolean.parseBoolean(value));
                    }
                } else if (key.equals(String.format("font.%s.underline", scope))) {
                    Boolean underline = Boolean.parseBoolean(value);
                    if (underline) {
                        if (scope.equalsIgnoreCase("head")) {
                            headFont.setUnderline(Font.U_SINGLE);
                        } else {
                            dataFont.setUnderline(Font.U_SINGLE);
                        }
                    } else {
                        if (scope.equalsIgnoreCase("head")) {
                            headFont.setUnderline(Font.U_NONE);
                        } else {
                            dataFont.setUnderline(Font.U_NONE);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets a list of supported extensions.
     *
     */
    @Override
    public String[] getValidFileExtensions() {
        return new String[]{ "xls", "xlsx" };
    }

    /**
     * Dispose the workbook object.
     *
     * @throws IOException if bad things happen
     */
    public void dispose() throws IOException {
        if (workbook instanceof SXSSFWorkbook) {
            ((SXSSFWorkbook) workbook).dispose();
        }
    }

    /**
     * Dispose the workbook object after flushing any buffered content.
     *
     * @throws IOException if bad things happen
     *
     */
    public void close() throws IOException {
        workbook.write(fileOutputStream);
        this.dispose();
    }
}