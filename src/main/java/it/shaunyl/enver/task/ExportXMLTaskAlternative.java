package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.ITaskMode;
import it.shaunyl.enver.exception.EnverException;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.task.export.IExportTaskMode;
import it.shaunyl.enver.task.export.core.ExportQueryTaskMode;
import it.shaunyl.enver.task.export.core.ExportTableTaskMode;
import it.shaunyl.enver.task.writer.core.XMLWriter;
import it.shaunyl.enver.util.GeneralUtil;
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 *
 * @author Filippo
 */
//@Deprecated
public class ExportXMLTaskAlternative extends EncodingEvaluation.Task {

    private List<String> schemas;

    private String schema;

    private boolean isQuery;

    private int clobIndex, start, end;

    private XMLWriter writer = null;

    private ITaskMode exportMode;
    
    private IExportTaskMode exportTaskMode;

    ExportXMLTaskAlternative(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {
        super.taskSetup();

        super.factory.register(new ExportTableTaskMode());
        super.factory.register(new ExportQueryTaskMode());

        String mode = !options.getQueries().isEmpty() ? "query" : "table";

        try {
            exportMode = factory.getMode(mode);
        } catch (EnverException e) {
            throw new TaskException(e.getMessage());
        }
        this.exportMode.setup(this);

        schemas = options.getSchemas();

        if (!schemas.isEmpty()) {
            schema = schemas.get(0).toUpperCase();  // one schema only for now
        }

//        if (mode.equals("table")) { // FIXME: not very good..
//            start = options.getStart();
//            end = options.getEnd();
//            if (start != -1 || end != -1) {
//                status.printReportLine(System.out, String.format("You have been asked to retrieve a range of lines: (%d, %s)", start == -1 ? 1 : start, end == -1 ? ":end" : end + ""));
//            }
//        }

        callable = connection.prepareCall("BEGIN ? := main.expxml(?, ?, ?, ?, ?, ?); END;");
        
        this.exportTaskMode = ((IExportTaskMode) this.exportMode);
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {

        this.exportMode.run(i);

        String table = exportTaskMode.getTable(), query = exportTaskMode.getQuery();

        setParameters(table, query);
        clobIndex = 7;

        String buffer = table != null ? table : query;
        status.printReportLine(System.out, String.format("Exporting data from %s '%s%s'...", table == null ? "query" : "table", table == null ? "" : schema + ".", table));

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

            writer = new XMLWriter(new FileWriter(exportTaskMode.getFilename()));
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
            callable.setInt(5, exportTaskMode.getStart());
            callable.setInt(6, exportTaskMode.getEnd());
            callable.registerOutParameter(7, Types.CLOB);
            callable.registerOutParameter(1, Types.INTEGER);
        } catch (SQLException e) {
            throw new UnexpectedEnverException(e.getMessage());
        }
    }

    @Override
    public void taskTakedown() throws SQLException {
        super.taskTakedown();
        this.exportMode.takedown();
        super.factory.unregisterAll();
    }
}