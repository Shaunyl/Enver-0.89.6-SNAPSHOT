package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.task.writer.core.XMLWriter;
import it.shaunyl.enver.util.DatabaseUtil;
import it.shaunyl.enver.util.GeneralUtil;
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 *
 * @author Filippo
 */
public class ExportXMLTask extends EncodingEvaluation.Task {

    private List<String> schemas, tables, queries, filenames;

    private String schema, filename, directory;

    private boolean isQuery;

    private int clobIndex, start, end;

    private XMLWriter writer = null;

    ExportXMLTask(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {
        super.taskSetup();

        directory = options.getDirectory();

        schemas = options.getSchemas();
        boolean full = options.getFull();
        queries = options.getQueries();
        boolean isQueriesEmpty = queries.isEmpty();

        if (!schemas.isEmpty()) {
            schema = schemas.get(0).toUpperCase();  // one schema only for now
            if (!isQueriesEmpty) {
                status.printReportLine(System.out, "The option SCHEMAS will be skipped.");
            }
        }

        filenames = options.getFilenames();

        if (!isQueriesEmpty) {
            isQuery = true;
            setCycle(queries.size());
        }

        if (!isQuery) {
            tables = full ? DatabaseUtil.getAllTables(connection, schema) : options.getTables();

            if (tables.isEmpty()) {
                setCycle(0);
                return;
            }

            start = options.getStart();
            end = options.getEnd();
            if (start != -1 || end != -1) {
                status.printReportLine(System.out, String.format("You have been asked to retrieve a range of lines: (%d, %s)", start == -1 ? 1 : start, end == -1 ? ":end" : end + ""));
            }

            this.setCycle(tables.size());
        }

        callable = connection.prepareCall("BEGIN ? := main.expxml(?, ?, ?, ?, ?, ?); END;");
    }

    @Override
    public void taskAtomic(final int i) throws TaskException {
        String table = null, query = null;
        if (isQuery) {
            query = queries.get(i);

            if (filenames.size() > i) {
                filename = String.format("%s/%s.xml", directory, filenames.get(i));
            } else {
                filename = String.format("%s/query_%d.xml", directory, System.currentTimeMillis());
            }
            status.printReportLine(System.out, String.format("Executing query '%s'...", query));
        } else {
            table = tables.get(i).toUpperCase(); // one schema only for now
            if (filenames.size() > i) {
                filename = String.format("%s/%s.xml", directory, filenames.get(i));
            } else {
                filename = String.format("%s/%s.xml", directory, table);
            }
            status.printReportLine(System.out, String.format("Exporting table '%s.%s'...", schema, table));
        }

        setParameters(table, query);
        clobIndex = 7;

        String buffer = table != null ? table : query;
        try {
            callable.execute();
            java.sql.Clob clob = callable.getClob(clobIndex);
            if (clob == null) {
                status.printReport(System.out, String.format("Warnings on '%s' \n  > %s", buffer, "Table is empty or contains invalid characters\n"));
                warnings += 1;
                Report.clean();
                return;
            }
            final String xml = GeneralUtil.readClob(clob);

            writer = new XMLWriter(new FileWriter(filename));
            List<String[]> data = new ArrayList<String[]>();
            data.add(new String[]{ xml });
            writer.writeAll(data);

            if (isQuery) {
                Report.appendlnformat("Result set get from query '%s' successfully exported.", buffer);
            } else {
                Report.appendlnformat("Table '%s.%s' successfully exported.", schema, buffer);
            }
        } catch (SQLException e) {
            if (isTaskCancelled) {
                Report.clean();
            } else if (e.getErrorCode() == 6503) {
                status.printReport(System.out, String.format("Errors from '%s'\n  > Table is empty or does not exists\n", buffer));
                errors += 1;
                Report.clean();
            } else if (e.getErrorCode() == 942) {
                status.printReport(System.out, String.format("Errors from '%s'\n  > %s", buffer, e.getMessage()));
                errors += 1;
                Report.clean();
            }
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage());
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                throw new UnexpectedEnverException(e.getMessage());
            }
        }
    }

    private void setParameters(String table, String query) {
        try {
            callable.setString(2, schema); //TEMPME...      
            callable.setString(3, table);
            callable.setString(4, query);
            callable.setInt(5, start);
            callable.setInt(6, end);
            callable.registerOutParameter(7, Types.CLOB);
            callable.registerOutParameter(1, Types.INTEGER);
        } catch (SQLException e) {
            throw new UnexpectedEnverException(e.getMessage());
        }
    }
}
