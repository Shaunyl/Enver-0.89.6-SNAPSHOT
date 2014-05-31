package it.shaunyl.enver.task.writer.core;

import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.task.graph.core.CartesianPlotStrategy;
import it.shaunyl.enver.task.graph.IChart;
import it.shaunyl.enver.task.graph.IPlotStrategy;
import it.shaunyl.enver.task.graph.core.PiePlotStrategy;
import it.shaunyl.enver.task.writer.IEnverWriter;
import it.shaunyl.enver.util.DatabaseUtil;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.NonNull;
import org.jfree.chart.*;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class IMAGEWriter implements IEnverWriter {

    private ByteArrayOutputStream byteArrayOutputStream;

    private String format, graph;

    private File outputFile;

    private int entries, width, height;

    private IChart chart;

    /**
     * The default IMAGE format.
     */
    public static final String DEFAULT_FORMAT = "png";

    /**
     * The default type of diagram to generate.
     */
    private static final String DEFAULT_GRAPH = "cartesian";

    /**
     * The default IMAGE width.
     */
    private static final int DEFAULT_IMAGE_WIDTH = 800;

    /**
     * The default IMAGE height.
     */
    private static final int DEFAULT_IMAGE_HEIGHT = 600;

    /**
     * The max number of elements for each series.
     */
    private static final int MAX_ENTRIES = 50;

    /**
     * Constructs IMAGEWriter using PNG format, max entries equal to 50 and
     * resolution 800x600.
     *
     * @param file the abstract representation of the IMAGE file location.
     * @param chart a chart object supplied with label texts and legend setting.
     */
    public IMAGEWriter(File file, IChart chart) throws IOException {
        this(file, chart, DEFAULT_FORMAT);
    }

    public IMAGEWriter(File file, IChart chart, String format) throws IOException {
        this(file, chart, format, DEFAULT_GRAPH);
    }

    /**
     * Constructs IMAGEWriter with supplied the format.
     *
     * @param file the output filename.
     * @param chart a chart object supplied with label texts and legend setting.
     * @param format the format of the image to be saved.
     */
    public IMAGEWriter(File file, IChart chart, String format, String graph) throws IOException {
        this(file, chart, format, graph, MAX_ENTRIES);
    }

    /**
     * Constructs IMAGEWriter with supplied the format and the max number of
     * entries.
     *
     * @param file the output filename.
     * @param chart a chart object supplied with label texts and legend setting.
     * @param format the format of the image to be saved.
     * @param entries the maximum number of entries for each series.
     */
    public IMAGEWriter(File file, IChart chart, String format, String graph, int entries) throws IOException {
        this(file, chart, format, graph, entries, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
    }

    /**
     * Constructs IMAGEWriter with supplied the format and the resolution of the
     * image.
     *
     * @param file the output filename.
     * @param chart a chart object supplied with label texts and legend setting.
     * @param format the format of the image to be saved.
     * @param entries the maximum number of entries for each series.
     * @param width the width of the image.
     * @param height the height of the image.
     */
    public IMAGEWriter(File file, IChart chart, String format, String graph, int entries, int width, int height) throws IOException {
        this.outputFile = file;
        this.chart = chart;
        this.format = format;
        this.graph = graph;
        this.entries = entries;
        this.width = width;
        this.height = height;
    }

    @Override
    public String[] getValidFileExtensions() {
        return new String[]{ "png" };
    }

    /**
     * Export the data into as an IMAGE. The list is assumed to be a Double[] or
     * a {Date|Timestamp}[]. [Note: only double for now]
     *
     * @param lines a List of Double[] or {Date|Timestamp}[], with each array
     * representing a line of the file. [Note: only double for now]
     *
     */
    @Override
    public void writeAll(@NonNull final java.util.List lines) throws IOException {

        int k = -1;
        int columns = ((String[]) lines.get(0)).length;
        String[] labels = new String[columns];
        double[][] series = new double[columns][entries];
        for (Iterator iter = lines.iterator(); iter.hasNext();) {

            if (k == -1) { // skip header
                System.arraycopy((String[]) iter.next(), ++k, labels, 0, columns);
                continue;
            }

            String[] nextLine = (String[]) iter.next();
            writeNext(k++, nextLine, series);
        }
        final Dataset dataset;
        final IPlotStrategy plotStrategy;
        if (graph.equals("cartesian")) {
            dataset = new DefaultXYDataset();
            for (int i = 1; i < columns; i++) {
                ((DefaultXYDataset) dataset).addSeries(labels[i], new double[][]{ series[0], series[i] }); //TEMPE, legenda name
            }
            plotStrategy = new CartesianPlotStrategy(dataset);
        } else if (graph.equals("pie")) {
            dataset = new DefaultPieDataset();
            DefaultPieDataset pie = (DefaultPieDataset) dataset;
            for (int i = 0; i < columns; i++) {
                pie.setValue(labels[i], series[i][0]);
            }
            plotStrategy = new PiePlotStrategy(dataset);
        } else {
            throw new UnexpectedEnverException("The chart mode '" + graph + "' is not supported.");
        }

        final JFreeChart jfreechart = plotStrategy.plot(chart);
        this.exportImage(jfreechart);
    }

    private void exportImage(JFreeChart jfreechart) throws IOException {
        ChartPanel chartPanel = new ChartPanel(jfreechart, false);
        chartPanel.setPreferredSize(new Dimension(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT));

        BufferedImage bufferedImage = jfreechart.createBufferedImage(width, height);
        byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, this.format, this.byteArrayOutputStream);

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        InputStream in = new ByteArrayInputStream(byteArray);
        BufferedImage image = ImageIO.read(in);
        ImageIO.write(image, this.format, this.outputFile);
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine a string array with each comma-separated element as a
     * separate entry.
     */
    public void writeNext(int id, String[] nextLine, double[][] series) {

        if (nextLine == null) {
            return;
        }

        for (int i = 0; i < nextLine.length; i++) {
            double nextElement = Double.parseDouble(nextLine[i]);
            series[i][id] = nextElement;
        }
    }

    protected String[] writeColumnNames(@NonNull final ResultSetMetaData metadata)
            throws SQLException {

        int columnCount = metadata.getColumnCount();

        String[] nextLine = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            nextLine[i] = metadata.getColumnName(i + 1);
        }

        return nextLine;
    }

    @Override
    public void writeAll(ResultSet rs, boolean includeColumnNames) throws SQLException, IOException, TaskException {
        ResultSetMetaData metadata = rs.getMetaData();

        String[] columns = null;
        if (includeColumnNames) {
            columns = writeColumnNames(metadata);
        }

        int columnCount = columns.length;
        int tuples = 0;
        List<String[]> lines = new ArrayList<String[]>();
        lines.add(columns);
        while (rs.next()) {

            String[] nextLine = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                nextLine[i] = DatabaseUtil.getColumnValue(rs, metadata.getColumnType(i + 1), i + 1);
            }
            lines.add(nextLine);
            tuples++;
        }
        this.entries = tuples;
        this.writeAll(lines);
    }

    /**
     * Dispose all the disposable objects.
     *
     * @throws IOException if bad things happen.
     */
    public void dispose() throws IOException {
    }

    /**
     * Dispose all the disposable objects after flushing any buffered content.
     *
     * @throws IOException if bad things happen.
     *
     */
    public void close() throws IOException {
        byteArrayOutputStream.close();
        this.dispose();
    }
}
