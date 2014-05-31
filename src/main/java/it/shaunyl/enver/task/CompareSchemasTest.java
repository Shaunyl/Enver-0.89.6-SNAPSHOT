package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.EncodingEvaluation.Status;
import it.shaunyl.enver.EncodingEvaluation.TaskOptions;
import it.shaunyl.enver.task.compare.core.CompareDataTaskMode;
import it.shaunyl.enver.task.compare.core.CompareStructureTaskMode;
import it.shaunyl.enver.ITaskMode;
import it.shaunyl.enver.exception.EnverException;
import it.shaunyl.enver.exception.TaskException;
import java.sql.SQLException;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class CompareSchemasTest extends EncodingEvaluation.Task {

    private ITaskMode compareMode;

    CompareSchemasTest(TaskOptions options, Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {
        super.taskSetup();
        super.factory.register(new CompareStructureTaskMode());
        super.factory.register(new CompareDataTaskMode());

        try {
            this.compareMode = this.factory.getMode(options.getMode());
        } catch (EnverException e) {
            throw new TaskException(e.getMessage());
        }
        this.compareMode.setup(this);
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {
        this.compareMode.run(i);
    }

    @Override
    public void taskTakedown() throws SQLException {
        super.taskTakedown();
        this.compareMode.takedown();
        super.factory.unregisterAll();
    }
}
