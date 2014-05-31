package it.shaunyl.enver.task.graph.core;

import it.shaunyl.enver.task.graph.IChart;
import it.shaunyl.enver.task.graph.IPlotStrategy;
import org.jfree.chart.*;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.*;
import org.jfree.util.Rotation;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class PiePlotStrategy implements IPlotStrategy {

    private Dataset dataset;
    private String parameterFile = "config/chartPieFormatting.properties";

    public PiePlotStrategy(Dataset dataset) {
        this.dataset = dataset;
    }

    public JFreeChart plot(IChart chart) {
        JFreeChart jfreechart = ChartFactory.createPieChart3D(chart.getTitle(),
                (DefaultPieDataset) this.dataset, // data
                chart.isLegend(), // include legend
                false,
                false);

        PiePlot3D plot = (PiePlot3D) jfreechart.getPlot();
        plot.setStartAngle(10);
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setForegroundAlpha(0.5f);
        plot.setInteriorGap(0.04);
//        plot.setSectionPaint(1, new Color(0x318ce7));
//        plot.setSectionPaint(2, new Color(0x2a52be));
//        plot.setSectionPaint(3, Color.BLUE);
        return jfreechart;

    }
}
