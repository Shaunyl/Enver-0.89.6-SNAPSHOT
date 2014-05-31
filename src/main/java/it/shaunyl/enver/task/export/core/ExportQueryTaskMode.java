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
public class ExportQueryTaskMode extends IExportTaskMode {

    private List<String> queries, filenames;

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

        directory = options.getDirectory();
        queries = options.getQueries();
        if (queries.isEmpty()) {
            status.printReportLine(System.out, "Warning: the 'query' argument exists but it is empty.");
            task.setWarnings(task.getWarnings() + 1);
        }

        status.printReportLine(System.out, "The option SCHEMAS will be skipped.");

        filenames = options.getFilenames();
        task.setCycle(queries.size());
    }

    @Override
    public void run(int i) throws SQLException {
        query = queries.get(i);

        if (filenames.size() > i) {
            filename = String.format("%s/%s.xml", directory, filenames.get(i));
        } else {
            filename = String.format("%s/query_%d.xml", directory, System.currentTimeMillis());
        }
    }

    @Override
    public boolean identify(String modality) {
        return "query".equals(modality);
    }

    @Override
    public void takedown() throws SQLException {
    }
}
