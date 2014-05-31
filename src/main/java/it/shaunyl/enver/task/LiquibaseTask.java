package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.util.DatabaseUtil;
import java.io.*;
import java.sql.*;
import javax.xml.parsers.ParserConfigurationException;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.*;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.diff.output.report.DiffToReport;
import liquibase.exception.*;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.snapshot.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class LiquibaseTask extends EncodingEvaluation.Task {

    private DatabaseSnapshot referenceSnapshot, targetSnapshot;

    private CompareControl compareControl;

    private String mode, filename;

    private Liquibase liquibase = null;

    public LiquibaseTask(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {

        mode = options.getMode();
        if (mode.toLowerCase().equals("diff")) {
            super.taskSetup();
            this.doDiffSetup();
            super.taskTakedown();
        } else if (mode.toLowerCase().equals("migrate")) {
            this.doMigrateSetup();
        }

        this.setCycle(1);
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {
        if (mode.toLowerCase().equals("diff")) {
            this.runDiff();
            Report.appendlnformat("Liquibase 'diff' task completed successfully. A report file '%s' was generated.", filename);
        } else if (mode.toLowerCase().equals("migrate")) {
            this.runMigrate();
            Report.appendlnformat("Liquibase 'migrate' task completed successfully.", filename);
        }
    }

    private void runDiff() {
        try {
            DiffResult diffResult = DiffGeneratorFactory.getInstance()
                    .compare(referenceSnapshot, targetSnapshot, compareControl);

            String format = options.getFormat();
            if (format == null) {
                format = "raw";
            }
            filename = options.getFilename();
            if (filename == null) {
                filename = "liquibase-diff-" + System.currentTimeMillis();
            }

            if (format.equals("xml")) {
                if (!filename.endsWith(".xml")) {
                    filename += ".xml";
                }
                new DiffToChangeLog(diffResult, new DiffOutputControl()).print(filename);
            } else if (format.equals("raw")) {
                if (!filename.endsWith(".txt")) {
                    filename += ".txt";
                }
                new DiffToReport(diffResult, new PrintStream(filename)).print();
            } else {
                new DiffToReport(diffResult, System.out).print();
            }
        } catch (DatabaseException e) {
            throw new UnexpectedEnverException(e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new UnexpectedEnverException(e.getMessage(), e);
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage(), e);
        }
    }

    private void runMigrate() {
        try {
            liquibase.update(new Contexts());
        } catch (LiquibaseException e) {
            System.out.println("Liquibase error:" + e);
        }
    }

    private void doMigrateSetup() {
        Database database = null;
        try {
            File file = new File(options.getChangelog());
            if (!file.exists()) {
                throw new UnexpectedEnverException(String.format("Error: '%s' does not exist.", file.getAbsolutePath()));
            }
            Connection targetc = DatabaseUtil.buildConnection("ORACLE", "liquibase.migrate", options.getDbAutocommit(), options.getDbTimeout());

            database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(targetc));
            liquibase = new Liquibase(file.getAbsolutePath(), new FileSystemResourceAccessor(), database);

        } catch (DatabaseException e) {
            throw new UnexpectedEnverException("Liquibase error: " + e.getMessage());
        } catch (LiquibaseException e) {
            throw new UnexpectedEnverException("Liquibase error: " + e.getMessage());
        } catch (SQLException e) {
            throw new UnexpectedEnverException("SQL error: " + e.getMessage());
        }
    }

    private void doDiffSetup() throws SQLException {
        Connection targetc = DatabaseUtil.buildConnection("ORACLE", options.getRemote(), options.getDbAutocommit(), options.getDbTimeout());
        JdbcConnection jdbcctarget = new JdbcConnection(targetc);
        JdbcConnection jdbcc = new JdbcConnection(connection);

        Database database, databaseTarget;

        try {
            database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcc);
            databaseTarget = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcctarget);

            referenceSnapshot = SnapshotGeneratorFactory.getInstance()
                    .createSnapshot(database.getDefaultSchema(), database, new SnapshotControl(database));

            targetSnapshot = SnapshotGeneratorFactory.getInstance()
                    .createSnapshot(databaseTarget.getDefaultSchema(), databaseTarget, new SnapshotControl(databaseTarget));

            compareControl = new CompareControl(referenceSnapshot.getSnapshotControl().getTypesToInclude());

        } catch (DatabaseException e) {
//            log.error("" + e.getMessage(), e);
        } catch (InvalidExampleException e) {
//            log.error("" + e.getMessage(), e);
        }
    }

    @Override
    public void taskTakedown() throws SQLException {
    }
}
