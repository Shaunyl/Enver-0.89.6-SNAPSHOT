package it.shaunyl.enver.task.graph.core;

import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.task.graph.IChart;
import it.shaunyl.enver.task.graph.IPlotStrategy;
import java.awt.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.DefaultXYDataset;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class CartesianPlotStrategy implements IPlotStrategy {

    private Dataset dataset;
    private String parameterFile = "config/chartCartesianFormatting.properties";
    private Font titleFont, labelFont;

    public CartesianPlotStrategy(Dataset dataset) {
        this.dataset = dataset;
    }

    public JFreeChart plot(IChart chart) {
        CartesianChart xychart = (CartesianChart)chart;
        JFreeChart jfreechart = ChartFactory.createXYLineChart(
                chart.getTitle().toUpperCase(),
                xychart.getXlabel(),
                xychart.getYlabel(),
                (DefaultXYDataset) this.dataset, // initial series
                PlotOrientation.VERTICAL, // orientation
                chart.isLegend(),
                false, // tooltips?
                false // URLs?
                );

        // set chart background
        jfreechart.setBackgroundPaint(Color.WHITE);

        this.setFonts();

        jfreechart.getTitle().setFont(titleFont);

        // set a few custom plot features
        XYPlot plot = (XYPlot) jfreechart.getPlot();
        plot.setBackgroundPaint(new Color(0xf5f5f5));
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);

        // set the plot's axes to display integers
        TickUnitSource ticks = NumberAxis.createIntegerTickUnits();
        NumberAxis domain = (NumberAxis) plot.getDomainAxis();
        domain.setLabelFont(labelFont);
        domain.setStandardTickUnits(ticks);
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setStandardTickUnits(ticks);
        range.setLabelFont(labelFont);

        // render shapes and lines
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        plot.setRenderer(renderer);
        renderer.setBaseShapesVisible(true);
        renderer.setBaseShapesFilled(true);
        renderer.setSeriesPaint(0, Color.BLUE);

        // set the renderer's stroke
        Stroke stroke = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
        renderer.setBaseOutlineStroke(stroke);

        // label the points
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(3);
        XYItemLabelGenerator generator = new StandardXYItemLabelGenerator(
                StandardXYItemLabelGenerator.DEFAULT_ITEM_LABEL_FORMAT,
                numberFormat, numberFormat);
        renderer.setBaseItemLabelGenerator(generator);
        renderer.setBaseItemLabelsVisible(true);

        return jfreechart;
    }

    private void setFonts() {
        titleFont = new Font("Calibri", Font.BOLD, 32);
        labelFont = new Font("Calibri", Font.BOLD, 22);

        File propertiesFile = new File(this.parameterFile);
        if (propertiesFile.exists()) {
            try {
                this.parseCartesianFile(new FileInputStream(propertiesFile));

            } catch (FileNotFoundException e) {
                throw new UnexpectedEnverException(e.getMessage());
            }
        }
    }

    private void parseCartesianFile(InputStream propertiesInputStream) {
        Properties props = new Properties();
        try {
            props.load(propertiesInputStream);
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage());
        }

        Pattern regexHead = Pattern.compile("font.(title|label).*(.*|height|bold)");

        for (Map.Entry entry : props.entrySet()) {
            String value = entry.getValue().toString().trim();
            String key = (String) entry.getKey();
            Matcher matcher = regexHead.matcher(key);

            if (matcher.find()) {
                String scope = matcher.group(1);
                if (key.equals(String.format("font.%s.height", scope))) {
                    if (scope.equalsIgnoreCase("title")) {
                        titleFont = titleFont.deriveFont(Float.parseFloat(value));
                    } else {
                        labelFont = labelFont.deriveFont(Float.parseFloat(value));
                    }
                } else if (key.equals(String.format("font.%s.bold", scope))) {
                    boolean bold = Boolean.parseBoolean(value);
                    if (scope.equalsIgnoreCase("title")) {
                        titleFont = titleFont.deriveFont(bold ? Font.BOLD : Font.PLAIN);
                    } else {
                        labelFont = labelFont.deriveFont(bold ? Font.BOLD : Font.PLAIN);
                    }
                } else if (key.equals(String.format("font.%s", scope))) {
                    if (scope.equalsIgnoreCase("title")) {
                        titleFont = new Font(value, titleFont.getStyle(), titleFont.getSize());
                    } else {
                        labelFont = new Font(value, labelFont.getStyle(), labelFont.getSize());
                    }
                }
            }
        }
    }
}
