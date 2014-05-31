package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.task.writer.core.CSVWriter;
import java.io.*;
import java.sql.SQLException;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class ExportCSVTask extends EncodingEvaluation.Task {

    private String query, filename;

    private char delimiter;

    private CSVWriter writer = null;

    private int start, end;

    private ExportCSVTask(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {
        super.taskSetup();

        query = options.getQuery();
        this.setCycle(1);

        filename = options.getFilename();
        if (filename == null) {
            filename = String.format("%s/query_%d.csv", options.getDirectory(), System.currentTimeMillis());
        } else {
            filename = String.format("%s/%s", options.getDirectory(), filename);
            if (!filename.endsWith(".csv")) {
                filename += ".csv";
            }
        }

        delimiter = options.getDelimiter();
        start = options.getStart();
        end = options.getEnd();
        if (start != -1 || end != -1) {
            status.printReportLine(System.out, String.format("You have been asked to retrieve a range of lines: (%d, %s)", start == -1 ? 1 : start, end == -1 ? ":end" : end + ""));
        }

        try {
            writer = (delimiter == '\u0000') ? new CSVWriter(new FileWriter(filename), start, end)
                    : new CSVWriter(new FileWriter(filename), delimiter, start, end);
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage(), e);
        }

        statement = connection.createStatement();
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {
        status.printReportLine(System.out, String.format("Executing query '%s'...", query));
        resultSet = statement.executeQuery(query);
        try {
            writer.writeAll(resultSet, true);

        } catch (SQLException e) {
            status.printReportLine(System.out, String.format("Warnings on '%s' query\n  > %s", query, e.getMessage()));
            warnings += 1;
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage(), e);
        } catch (TaskException e) {
            status.printReportLine(System.out, String.format("Warnings on '%s' query\n  > %s", query, e.getMessage()));
            warnings += 1;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                throw new UnexpectedEnverException(e.getMessage(), e);
            }
        }
    }
}
