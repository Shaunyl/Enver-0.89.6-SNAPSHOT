package it.shaunyl.enver.task.compare.core;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.task.compare.ICompareTaskMode;
import java.sql.*;
import java.util.*;
import oracle.sql.ARRAY;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class CompareDataTaskMode extends ICompareTaskMode {

    private final String typeObjComparison = "T_COMPARISON", typeObjConverge = "T_CONVERGE";

    private List<String[]> differences;

    private Boolean converge;

    private String local, remote, diffs_count, sync, scanId, comparisonName, winner, columns, scanMode, dblink;

    private List<String> schemas, tables;

    public CompareDataTaskMode() {
    }

    private void setConfiguration(EncodingEvaluation.Task task) {
        this.task = task;
        this.options = task.getOptions();
        this.status = task.getStatus();
        this.connection = task.getConnection();
    }

    @Override
    public void setup(EncodingEvaluation.Task task) throws SQLException {
        this.setConfiguration(task);
        this.callable = this.connection.prepareCall("{ call comparison_dbms.set_comparison(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
        this.comparisonName = options.getComparisonName();
        this.winner = options.getWinner();
        if (winner == null) {
            winner = "LOCAL";
        }
        this.converge = options.getConverge();
        task.setCycle(converge ? 3 : 2);
        if (converge) {
            status.printReportLine(System.out, "If data within two database objects is divergent, data divergence will be resolved.");
        }

        schemas = options.getSchemas();
        if (schemas.size() == 1) {
            schemas.add(schemas.get(0));
        }
        tables = options.getTables();
        columns = options.getColumns();
        if (columns == null) {
            columns = "*";
        }
        scanMode = options.getScanMode();
        if (scanMode == null) {
            scanMode = "FULL";
        }
        dblink = options.getDblink();
    }

    @Override
    public void run(int i) throws SQLException {
        if (i == 0) {
            setSetComparisonParameters();
            status.printReportLine(System.out, "Creating comparison...");
            try {
                this.callable.execute();
            } catch (SQLException e) {
                if (e.getErrorCode() == 23627) {
                    EncodingEvaluation.Task.Report.appendln("Warning: The comparison '"
                            + options.getComparisonName().toUpperCase() + "' already exists. Its creation has been skipped.");
                    task.setWarnings(task.getWarnings() + 1);
                }
            }
        } else if (i == 1) {
            this.callable = this.connection.prepareCall("{ call comparison_dbms.do_comparison(?, ?) }");
            setDoComparisonParameters();
            status.printReportLine(System.out, "Executing comparison...");
            this.callable.execute();

            this.compare();

        } else if (converge && Integer.parseInt(diffs_count) > 0) { // CONVERGE
            this.callable = this.connection.prepareCall("{ call comparison_dbms.do_converge(?, ?, ?, ?) }");
            setDoConvergeParameters();
            status.printReportLine(System.out, "Executing converge using scanId = " + this.scanId + "...");
            this.callable.execute();

            this.converge();

            return;
        } else {
            EncodingEvaluation.Task.Report.appendln("Warning: No data divergence found. Converge task skipped.");
            task.setWarnings(task.getWarnings() + 1);
        }

        this.callable.clearParameters();
        this.callable.clearWarnings();
    }

    @Override
    public void takedown() throws SQLException {
        connection.close();
        this.callable.close();
    }

    @Override
    public boolean identify(String modality) {
        return "data".equals(modality);
    }

    private void compare() throws SQLException {
        Object[] t_comparison_fields = ((Struct) this.callable.getObject(2)).getAttributes();

        Object[] t_comparison_diffs = (Object[]) ((ARRAY) t_comparison_fields[5]).getArray();
        differences = new ArrayList<String[]>();
        for (Object o : t_comparison_diffs) {
            Object[] t_comparison_diff_field = ((Struct) o).getAttributes();
            String[] fields = new String[2];
            for (int m = 0; m < t_comparison_diff_field.length; m++) {
                fields[m] = t_comparison_diff_field[m].toString();
            }
            differences.add(fields);
        }

        sync = t_comparison_fields[0].toString();
        scanId = t_comparison_fields[1].toString();
        local = t_comparison_fields[2].toString();
        remote = t_comparison_fields[3].toString();
        diffs_count = t_comparison_fields[4].toString();

        int differences_count = Integer.parseInt(diffs_count);
        EncodingEvaluation.Task.Report.appendlnformat(
                "The objects '%s' (LOCAL) and '%s' (REMOTE) are %s. %s differences was found.",
                local, remote, sync.equals("Y") ? "synchronized" : "not synchronized",
                differences_count > 0 ? differences_count : "No");

        if (differences_count > 0) {
            EncodingEvaluation.Task.Report.appendln("\n-- List of all differences --");
            for (String[] diff : differences) {
                EncodingEvaluation.Task.Report.appendlnformat("Index value: %s, Is Local: %s",
                        diff[0], diff[1]);
                // FIXME: può essere che sia LOCAL che REMOTE siano entrambi a Y, perchè le righe sono da entrambi
                // i lati ma sono diverse..
            }
        }
    }

    private void converge() throws SQLException {
        Object[] t_converge_fields = ((Struct) this.callable.getObject(4)).getAttributes();

        String local_merged = "Local rows merged: " + t_converge_fields[0].toString();
        String remote_merged = "Remote rows merged: " + t_converge_fields[1].toString();
        String local_deleted = "Local rows deleted: " + t_converge_fields[2].toString();
        String remote_deleted = "Remote rows deleted: " + t_converge_fields[3].toString();

        EncodingEvaluation.Task.Report.appendln("\n-- List of all changes --");
        EncodingEvaluation.Task.Report.appendlnformat(
                "%s\n%s\n%s\n%s", local_merged, remote_merged, local_deleted, remote_deleted);
    }

    private void setDoComparisonParameters() throws SQLException {
        this.callable.registerOutParameter(2, Types.STRUCT, typeObjComparison);
        this.callable.setString(1, this.comparisonName);
    }

    private void setDoConvergeParameters() throws SQLException {
        this.callable.registerOutParameter(4, Types.STRUCT, typeObjConverge);
        this.callable.setString(1, comparisonName);
        this.callable.setString(2, this.scanId);
        this.callable.setString(3, this.winner);
    }

    private void setSetComparisonParameters() throws SQLException {
        this.callable.setString(1, this.comparisonName);
        this.callable.setString(2, schemas.get(0));
        this.callable.setString(3, tables.get(0));
        this.callable.setString(4, null); // TEMPME
        this.callable.setString(5, null); // TEMPME
        this.callable.setString(6, this.dblink);
        this.callable.setString(7, schemas.get(1));
        this.callable.setString(8, tables.get(1));
        this.callable.setString(9, this.columns);
        this.callable.setString(10, this.scanMode);
    }
}
