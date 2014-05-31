package it.shaunyl.enver.task.reader.core;

import it.shaunyl.enver.task.reader.IEnverReader;
import java.io.*;
import java.rmi.UnexpectedException;
import java.util.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 * @version 1.2
 */
public class EXCELReader implements IEnverReader {

    private Workbook workbook;

    private FileInputStream inputStream;

    /**
     * The default EXCEL format.
     */
    public static final String DEFAULT_FORMAT = "xls";

    /**
     * Constructs EXCELReader using default EXCEL format.
     *
     * @param workbook the workbook to an underlying EXCEL source.
     * @param filename the location of the file from which to read.
     */
    public EXCELReader(String filename) throws IOException {
        this(filename, DEFAULT_FORMAT);
    }

    /**
     * Constructs EXCELReader with supplied EXCEL format.
     *
     * @param workbook the workbook to an underlying EXCEL source.
     * @param filename the location of the file from which to read.
     * @param format the format of the EXCEL file.
     */
    public EXCELReader(String filename, String format) throws IOException {
        this.inputStream = new FileInputStream(filename);
        this.workbook = format.equals("xls") ? new HSSFWorkbook(inputStream) : new XSSFWorkbook(inputStream);
    }

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     *
     * @return a List of String[], with each String[] representing a line of the
     * file.
     *
     * @throws IOException if bad things happen during the read.
     */
    @Override
    public List readAll() throws IOException {

        Sheet sheet = workbook.getSheetAt(0); //TEMPME
        Iterator<Row> rowIterator = sheet.iterator();

        List<Object> allElements = new ArrayList<Object>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Object[] nextLineAsTokens = readNext(row);
            if (nextLineAsTokens != null) {
                allElements.add(nextLineAsTokens);
            }
        }
        return allElements;

    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return a string array with each row cell as a separate entry.
     *
     * @throws IOException if bad things happen during the read
     */
    public Object[] readNext(Row row) throws IOException {

        Object[] nextLine = getNextLine(row);
        return nextLine;
    }

    /**
     * Reads the next line from the file.
     *
     * @param row The row being read.
     *
     * @return the next line from the file without trailing newline
     * @throws IOException if bad things happen during the read
     */
    private Object[] getNextLine(Row row) throws IOException {

        List<Object> lineCells = new ArrayList<Object>();

        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    lineCells.add(cell.getBooleanCellValue());
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    lineCells.add(cell.getNumericCellValue());
                    break;
                case Cell.CELL_TYPE_STRING:
                    String value = cell.getStringCellValue();
                    if ("".equals(value)) {
                        lineCells.add(null);
                    } else {
                        lineCells.add(cell.getStringCellValue());
                    }
                    break;
                case Cell.CELL_TYPE_BLANK:
                    lineCells.add(null);
                    break;
                default:
                    throw new UnexpectedException("Cell types ERROR and FORMULA are not yet supported.");
            }
        }
        return lineCells.toArray();
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
     * Closes the underlying reader.
     *
     * @throws IOException if bad things happen
     *
     */
    public void close() throws IOException {
        this.inputStream.close();
    }
}
