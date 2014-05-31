package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.task.graph.core.CartesianChart;
import it.shaunyl.enver.task.graph.IChart;
import it.shaunyl.enver.task.graph.core.PieChart;
import it.shaunyl.enver.task.reader.core.CSVReader;
import it.shaunyl.enver.task.writer.core.IMAGEWriter;
import java.io.*;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class ExportGraphTask extends EncodingEvaluation.Task {

    private String filename, format, query, file;

    private List data;

    private boolean isSep, isQuery;

    private CSVReader reader;

    private ExportGraphTask(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {
        query = options.getQuery();
        if (query != null) {
            super.taskSetup();
            isQuery = true;
        } else {
            file = options.getFile();
        }

        filename = options.getFilename();
        format = options.getFormat();
        if (format == null) {
            format = "png";
        }
        if (filename == null) {
            filename = String.format("%s/image_%d.%s", options.getDirectory(), System.currentTimeMillis(), format);
        } else {
            filename = String.format("%s/%s", options.getDirectory(), filename);
            if (!filename.endsWith("." + format)) {
                filename += "." + format;
            }
        }

        try {
            if (!isQuery) {
                char delimiter = options.getDelimiter();
                String ifile = String.format("%s/%s", options.getDirectory(), file);
                reader = (delimiter == '\u0000') ? new CSVReader(new FileReader(ifile))
                        : new CSVReader(new FileReader(ifile), delimiter);
                data = reader.readAll();
            } else {
                statement = connection.createStatement();
                resultSet = statement.executeQuery(query);
            }
        } catch (IOException e) {
            throw new UnexpectedEnverException("Seems like something went bad with the reader.", e); //TaskException..
        }
        setCycle(1);
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {
        String xlabel = options.getXlabel();
        if (!isQuery) {
            for (Iterator iter = data.iterator(); iter.hasNext();) {
                Object[] nextLine = (Object[]) iter.next();
                if (nextLine.length == 1 && nextLine[0].toString().matches("sep=\\p{P}")) {
                    data.remove(0); // Remove sep function for EXCEL compatibility
                    isSep = true;
                }
                // header:
                if (xlabel == null) {
                    if (isSep) {
                        nextLine = (Object[]) iter.next();
                    }
                    xlabel = nextLine[0].toString();
                }
                break; // SEP can be only at first line.
            }
            if (data.isEmpty()) {
                throw new TaskException("The source file contains no data.");
            }
        } else {
            ResultSetMetaData metaData = resultSet.getMetaData();
            if (xlabel == null) {
                xlabel = metaData.getColumnName(1);
            }
        }

        IChart chart = null;
        try {
            String title = options.getTitle();
            String chartitle = title == null ? "Data Graph" : title;
            String mode = options.getMode();
            String modechart = mode == null ? "cartesian" : mode;
            if (modechart.equals("cartesian")) {
                chart = new CartesianChart(chartitle, options.getLegend(), xlabel, options.getYlabel());
            } else if (modechart.equals("pie")) {
                chart = new PieChart(chartitle, options.getLegend());
            }
            IMAGEWriter writer;
            if (!isQuery) {
                writer = new IMAGEWriter(new File(filename), chart, format, modechart, data.size() - 1);
                writer.writeAll(data);
            } else {
                writer = new IMAGEWriter(new File(filename), chart, format, modechart);
                writer.writeAll(resultSet, true);
            }
            writer.close();
        } catch (IOException e) {
            throw new UnexpectedEnverException("Seems like something went bad with the IMAGE writer.", e);
        }
    }

    @Override
    public void taskTakedown() throws SQLException {
        if (query != null) {
            super.taskTakedown();
        }
    }
}
