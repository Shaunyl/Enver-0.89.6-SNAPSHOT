package it.shaunyl.enver.task.writer.core;

import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.task.writer.IEnverWriter;
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class XMLWriter implements IEnverWriter {

    private Writer rawWriter;

    private PrintWriter printer;

    private String endline;

    private int start, end;

    /**
     * Default line terminator uses platform encoding.
     */
    public static final String DEFAULT_LINE_END = "\n";

    /**
     * Constructs XMLWriter using the default line ending.
     *
     * @param writer the writer to an underlying XML source.
     */
    public XMLWriter(Writer writer) {
        this(writer, DEFAULT_LINE_END);
    }

    /**
     * Constructs XMLWriter with supplied line ending.
     *
     * @param writer the writer to an underlying XML source.
     * @param endline the line feed terminator to use.
     */
    public XMLWriter(Writer writer, String endline) {
        this.rawWriter = writer;
        this.printer = new PrintWriter(writer);
        this.endline = endline;
    }

    @Override
    public String[] getValidFileExtensions() {
        return new String[]{ "xml" };
    }

    /*@Override
     public void write(final @NonNull String filename) throws IOException, EnverException {
     @Cleanup
     PrintWriter out = new PrintWriter(filename, "UTF-8");
     out.write(data[0][0].toString());
     out.flush();
     }*/
    @Override
    public void writeAll(List lines) {
        for (Iterator iter = lines.iterator(); iter.hasNext();) {
            String[] nextLine = (String[]) iter.next();
            writeNext(nextLine);
        }
    }

    @Override
    public void writeAll(ResultSet rs, boolean includeColumnNames)
            throws SQLException, IOException, TaskException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void writeNext(String[] nextLine) {
        if (nextLine == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nextLine.length; i++) {
            String nextElement = nextLine[i];
            if (nextElement == null) {
                continue;
            }
            sb.append(nextElement);
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