package it.shaunyl.enver.task.graph;

import lombok.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public abstract class IChart {

    @Getter @Setter
    protected String title;
    @Getter @Setter
    protected boolean legend;

    public IChart(String title) {
        this(title, false);
    }

    public IChart(String title, boolean legend) {
        this.title = title;
        this.legend = legend;
    }
}
