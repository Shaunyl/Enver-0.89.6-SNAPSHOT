package it.shaunyl.enver;

import java.sql.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public abstract class ITaskMode {

    protected EncodingEvaluation.Task task;

    protected EncodingEvaluation.TaskOptions options;

    protected EncodingEvaluation.Status status;

    protected Connection connection;

    public abstract void setup(EncodingEvaluation.Task task) throws SQLException;

    public abstract void run(final int i) throws SQLException;

    public abstract void takedown() throws SQLException;

    public abstract boolean identify(String modality);
}
