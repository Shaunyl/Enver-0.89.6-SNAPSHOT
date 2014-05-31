package it.shaunyl.enver.task.graph;

import lombok.NonNull;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public interface IPlotStrategy {

    /**
     * Create a chart.
     *
     * @param chart A chart object with supplied labels and other properties.
     * @return the chart
     */
    @NonNull
    public JFreeChart plot(IChart chart);
}
