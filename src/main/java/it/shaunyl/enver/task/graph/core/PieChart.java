package it.shaunyl.enver.task.graph.core;

import it.shaunyl.enver.task.graph.IChart;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class PieChart extends IChart {
    
    public PieChart(String title) {
        super(title, false);
    }

    public PieChart(String title, boolean legend) {
        super(title, legend);
    }
}
