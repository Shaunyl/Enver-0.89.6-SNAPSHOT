package it.shaunyl.enver.task;

import it.shaunyl.enver.task.writer.core.EXCELWriter;
import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.util.DatabaseUtil;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class ExportEXCELTask extends EncodingEvaluation.Task {

    private String schema, format, filename, dir;

    private List<String> tables, sheets, queries;

    private boolean isQuery;

    private Integer flush;

    private int start, end;

    private Workbook workbook;

    private EXCELWriter writer = null;

    private Sheet sheet;

    private ExportEXCELTask(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {

        super.taskSetup();
        format = options.getFormat();
        if (format == null) {
            format = "xls"; //FIXME.. here and in EXCELWriter, two possible default formats..
        }

        queries = options.getQueries();
        sheets = options.getSheets();
        flush = options.getFlush();
        dir = options.getDirectory();

        start = options.getStart();
        end = options.getEnd();
        if (start != -1 || end != -1) {
            status.printReportLine(System.out, String.format("You have been asked to retrieve a range of lines: (%d, %s)", start == -1 ? 1 : start, end == -1 ? ":end" : end + ""));
        }

        if (!queries.isEmpty()) {
            this.setCycle(queries.size());
            tables = new ArrayList<String>();
            workbook = format.equals("xls") ? new HSSFWorkbook() : new SXSSFWorkbook(flush);
            for (int i = 0; i < queries.size(); i++) {
                tables.add(i + "query");
                if (i >= sheets.size()) {
                    sheets.add(i + "query");
                }
                workbook.createSheet(sheets.get(i));
            }
            isQuery = true;
            filename = String.format("%s/QUERIES-%d.%s", dir, System.currentTimeMillis(), format);
        } else {
            schema = options.getSchemas().get(0); // only one schema for now..

            tables = (options.getFull()) ? DatabaseUtil.getAllTables(connection, schema)
                    : options.getTables();

            for (int i = 0; i < tables.size(); i++) {
                queries.add(String.format("SELECT * FROM %s.%s", schema, tables.get(i)));
            }
            this.setCycle(queries.size());
        }
        statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }

    @Override
    public void taskAtomic(final int i) throws TaskException {
        String query = queries.get(i);
        try {
            status.printReportLine(System.out, String.format("Executing '%s'...", query));
            resultSet = statement.executeQuery(query);

            if (!isQuery) {
                filename = String.format("%s/%s.%s.%s", dir, schema, tables.get(i), format);
                workbook = format.equals("xls") ? new HSSFWorkbook() : new SXSSFWorkbook(flush);
            }

            sheet = (isQuery) ? workbook.getSheetAt(i) : workbook.createSheet(tables.get(i));

            writer = new EXCELWriter(new File(filename), sheet, format, start, end);
            if (isQuery) {
                if (writer.getWorkbook() != null) {
                    writer.setWorkbook(workbook);
                }
            } else {
                writer.setWorkbook(workbook);
            }
            writer.writeAll(resultSet, true);

            if (isQuery) {
                Report.appendlnformat("Sheet '%s' successfully created", sheet.getSheetName());
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 942) {
                status.printReport(System.out, String.format("Errors on query '%s'\n  > %s", query, e.getMessage()));
                errors += 1;
                Report.clean();
            } else {
                throw new UnexpectedEnverException(e.getMessage());
            }
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage());
        } catch (TaskException e) {
            status.printReport(System.out, String.format("Warnings on '%s' query\n  > %s", query, e.getMessage()));
            warnings += 1;
            Report.clean();
        }
    }

    @Override
    public void taskTakedown() throws SQLException {
        super.taskTakedown();
        try {
            if (!isQuery) {
                writer.close();
                Report.appendlnformat("Data successfully written to '%s'", filename);
                return;
            }
            if (warnings + errors < sheets.size()) {
                writer.close();
            }
            Report.appendlnformat("File successfully written to '%s'", filename);
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage());
        }
    }
}
