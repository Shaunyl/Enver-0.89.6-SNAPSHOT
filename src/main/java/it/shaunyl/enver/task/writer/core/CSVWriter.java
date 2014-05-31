package it.shaunyl.enver.task.writer.core;

import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.task.writer.IEnverWriter;
import it.shaunyl.enver.util.DatabaseUtil;
import java.io.*;
import java.sql.*;
import java.util.*;
import lombok.NonNull;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class CSVWriter implements IEnverWriter {

    private Writer rawWriter;

    private PrintWriter printer;

    private char delimiter, escapechar, quotechar;

    private String endline;

    private int start, end;

    /**
     * The default separator to use if none is supplied to the constructor.
     */
    public static final char DEFAULT_DELIMITER = ',';

    /**
     * The character used for escaping quotes.
     */
    public static final char DEFAULT_ESCAPE_CHARACTER = '"';

    /**
     * Default line terminator uses platform encoding.
     */
    public static final String DEFAULT_LINE_END = "\n";

    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /**
     * The quote constant to use when you wish to suppress all quoting.
     */
    public static final char NO_QUOTE_CHARACTER = '\u0000';

    /**
     * The escape constant to use when you wish to suppress all escaping.
     */
    public static final char NO_ESCAPE_CHARACTER = '\u0000';

    /**
     * Constructs CSVWriter using a comma for the delimiter.
     *
     * @param writer the writer to an underlying CSV source.
     */
    public CSVWriter(Writer writer) {
        this(writer, DEFAULT_DELIMITER);
    }

    /**
     * Constructs CSVWriter with supplied delimiter.
     *
     * @param writer the writer to an underlying CSV source.
     * @param delimiter the delimiter to use for separating entries.
     */
    public CSVWriter(Writer writer, char delimiter) {
        this(writer, delimiter, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied range of lines to be grabbed.
     *
     * @param writer the writer to an underlying CSV source.
     * @param start the line number to skip for start exporting.
     * @param end the last line number to retrieve for end exporting.
     */
    public CSVWriter(Writer writer, int start, int end) {
        this(writer, DEFAULT_DELIMITER, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END, start, end);
    }

    /**
     * Constructs CSVWriter with supplied delimiter and range of lines to be
     * grabbed.
     *
     * @param writer the writer to an underlying CSV source.
     * @param delimiter the delimiter to use for separating entries.
     * @param start the line number to skip for start exporting.
     * @param end the last line number to retrieve for end exporting.
     */
    public CSVWriter(Writer writer, char delimiter, int start, int end) {
        this(writer, delimiter, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END, start, end);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer the writer to an underlying CSV source.
     * @param delimiter the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     */
    public CSVWriter(Writer writer, char delimiter, char quotechar) {
        this(writer, delimiter, quotechar, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer the writer to an underlying CSV source.
     * @param delimiter the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     * @param escapechar the character to use for escaping quotechars or
     * escapechars
     */
    public CSVWriter(Writer writer, char delimiter, char quotechar, char escapechar) {
        this(writer, delimiter, quotechar, escapechar, DEFAULT_LINE_END, -1, -1);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer the writer to an underlying CSV source.
     * @param delimiter the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     * @param endline the line feed terminator to use
     */
    public CSVWriter(Writer writer, char delimiter, char quotechar, String endline) {
        this(writer, delimiter, quotechar, DEFAULT_ESCAPE_CHARACTER, endline, -1, -1);
    }

    /**
     * Constructs CSVWriter with supplied separator, quote char, escape char and
     * line ending.
     *
     * @param writer the writer to an underlying CSV source.
     * @param delimiter the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     * @param escapechar the character to use for escaping quotechars or
     * escapechars
     * @param lineEnd the line feed terminator to use
     */
    public CSVWriter(Writer writer, char delimiter, char quotechar, char escapechar, String endline, int start, int end) {
        this.rawWriter = writer;
        this.printer = new PrintWriter(writer);
        this.delimiter = delimiter;
        this.quotechar = quotechar;
        this.escapechar = escapechar;
        this.endline = endline;
        this.start = start;
        this.end = end;
    }

    protected void setDelimiterForMSExcel() {
        String[] nextLine = new String[]{ "sep=" + delimiter };
        writeNext(nextLine);
    }

    protected void writeColumnNames(@NonNull final ResultSetMetaData metadata)
            throws SQLException {

        int columnCount = metadata.getColumnCount();

        String[] nextLine = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            nextLine[i] = metadata.getColumnName(i + 1);
        }
        writeNext(nextLine);
    }

    /**
     * Writes the entire ResultSet to a CSV file.
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

        this.setDelimiterForMSExcel();

        if (includeColumnNames) {
            writeColumnNames(metadata);
        }

        int columnCount = metadata.getColumnCount();

        int r = 1;
        while (rs.next()) {
            if (r < start && start != -1) {
                r++;
                continue;
            }
            String[] nextLine = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                nextLine[i] = DatabaseUtil.getColumnValue(rs, metadata.getColumnType(i + 1), i + 1);
            }

            writeNext(nextLine);
            r++;
            if (r > end && end != -1) {
                break;
            }
        }
    }

    /**
     * Gets a list of supported extensions.
     *
     */
    @Override
    public String[] getValidFileExtensions() {
        return new String[]{ "csv" };
    }

    /**
     * Writes the entire list to a CSV file. The list is assumed to be a
     * String[].
     *
     * @param lines a List of String[], with each String[] representing a line
     * of the file.
     */
    @Override
    public void writeAll(@NonNull final List lines) {

        this.setDelimiterForMSExcel();

        for (Iterator iter = lines.iterator(); iter.hasNext();) {
            String[] nextLine = (String[]) iter.next();
            writeNext(nextLine);
        }
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine a string array with each comma-separated element as a
     * separate entry.
     */
    public void writeNext(String[] nextLine) {

        if (nextLine == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nextLine.length; i++) {

            if (i != 0) {
                sb.append(delimiter);
            }

            String nextElement = nextLine[i];
            if (nextElement == null) {
                continue;
            }
            if (quotechar != NO_QUOTE_CHARACTER) {
                sb.append(quotechar);
            }
            for (int j = 0; j < nextElement.length(); j++) {
                char nextChar = nextElement.charAt(j);
                if (escapechar != NO_ESCAPE_CHARACTER && nextChar == quotechar) {
                    sb.append(escapechar).append(nextChar);
                } else if (escapechar != NO_ESCAPE_CHARACTER && nextChar == escapechar) {
                    sb.append(escapechar).append(nextChar);
                } else {
                    sb.append(nextChar);
                }
            }
            if (quotechar != NO_QUOTE_CHARACTER) {
                sb.append(quotechar);
            }
        }

        sb.append(endline);
        printer.write(sb.toString());
    }

    /**
     * Flush underlying stream to writer.
     *
     * @throws IOException if bad things happen
     */
    public void flush() throws IOException {
        printer.flush();
    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     *
     * @throws IOException if bad things happen
     *
     */
    public void close() throws IOException {
        printer.flush();
        printer.close();
        rawWriter.close();
    }
}