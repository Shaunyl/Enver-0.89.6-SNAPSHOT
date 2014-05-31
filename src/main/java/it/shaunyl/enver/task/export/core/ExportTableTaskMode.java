/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.shaunyl.enver.task.export.core;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.task.export.IExportTaskMode;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author Filippo
 */
public class ExportTableTaskMode extends IExportTaskMode {

    private List<String> tables, filenames;

    private String directory;

    private void setConfiguration(EncodingEvaluation.Task task) {
        this.task = task;
        this.options = task.getOptions();
        this.status = task.getStatus();
        this.connection = task.getConnection();
    }

    @Override
    public void setup(EncodingEvaluation.Task task) throws SQLException {
        this.setConfiguration(task);

        tables = options.getTables(); // It can be only "tables"...
        directory = options.getDirectory();
        if (tables.isEmpty()) {
            task.setCycle(0);
            return;
        }

        start = options.getStart();
        end = options.getEnd();
        if (start != -1 || end != -1) {
            status.printReportLine(System.out, String.format("You have been asked to retrieve a range of lines: (%d, %s)", start == -1 ? 1 : start, end == -1 ? ":end" : end + ""));
        }

        task.setCycle(tables.size());
        filenames = options.getFilenames();
    }

    @Override
    public void run(int i) throws SQLException {

        table = tables.get(i).toUpperCase(); // one schema only for now
        if (filenames.size() > i) {
            filename = String.format("%s/%s.xml", directory, filenames.get(i));
        } else {
            filename = String.format("%s/%s.xml", directory, table);
        }
    }

    @Override
    public boolean identify(String modality) {
        return "table".equals(modality);
    }

    @Override
    public void takedown() throws SQLException {
    }
}
