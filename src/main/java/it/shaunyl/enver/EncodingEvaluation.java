package it.shaunyl.enver;

import it.shaunyl.enver.database.ConnectionFactory;
import it.shaunyl.enver.exception.CommandLineParsingException;
import it.shaunyl.enver.exception.TaskException;
import static it.shaunyl.enver.util.CommandLineUtil.printBanner;
import it.shaunyl.enver.util.DatabaseUtil;
import it.shaunyl.enver.util.EnverUtil;
import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import lombok.*;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class EncodingEvaluation {

    public static interface Status {

        void setStatus(PrintStream stream, final String msg) throws IOException;

        void printReport(PrintStream stream, final String msg);

        void printReportLine(PrintStream stream, final String msg);
    }

    @ToString(exclude = { "feedback" })
    public static class TaskOptions {

        @Getter
        private final char delimiter;

        @Getter
        private final String dblink, winner, scanMode, columns, comparisonName, title, xlabel, ylabel, file, changelog, database, datafiledir, param, directory, name, filename, format, local, remote, tnsname, query, mode;

        @Getter
        private final List<String> filenames, exclude, schemas, tables, items, queries, sheets;

        @Getter
        private final Boolean converge, legend, truncate, catalog, threads, full, frep, samples;

        @Getter
        private final Integer flush, feedback, start, end;

        @Getter
        private final Boolean dbAutocommit;

        @Getter
        private final int dbTimeout;

        public static class TaskBuilder {

            @Setter
            private char delimiter;

            @Setter
            private String dblink, winner, scanMode, columns, comparisonName, title, xlabel, ylabel, file, changelog, database, datafiledir, param, mode, directory = ".", filename, format, local, remote, tnsname, query;

            @Getter
            @Setter
            private String name;

            @Setter
            private Boolean converge = false, truncate = false, catalog = false, threads = false, full = false, frep = false, legend = false, samples = false;

            @Setter
            private Integer flush = 10, feedback = 1, start = -1, end = -1;

            @Setter
            private List<String> filenames = new ArrayList<String>(), exclude = new ArrayList<String>(), schemas = new ArrayList<String>(), tables = new ArrayList<String>(), items = new ArrayList<String>(), queries = new ArrayList<String>(), sheets = new ArrayList<String>();
            //Enver properties

            @Setter
            private Boolean dbAutocommit = false;

            @Setter
            private int dbTimeout = 15;

            public TaskBuilder(final @NonNull String name) {
                this.name = name;
            }

            public TaskOptions build() throws CommandLineParsingException {
                if (this.validate()) {
                    return new TaskOptions(this);
                }

                return null;
            }

            private boolean validate() throws CommandLineParsingException {
                if (start > end && end != -1) {
                    throw new CommandLineParsingException("If you set a range of lines to be retrieved, the value of END must be greater or equals to the value of START.");
                }

                if (name.equals("expexcel")) {
                    return ((full | !tables.isEmpty()) & !schemas.isEmpty()) | !queries.isEmpty();
                } else if (name.equals("expcsv")) {
                    return this.isNotNull(query);
                } else if (name.equals("expxml")) {
                    if (!tables.isEmpty() && !queries.isEmpty()) {
                        throw new CommandLineParsingException("Multiple job modes requested, tables and queries.");
                    }
                    if ((start != -1 || end != -1) && !queries.isEmpty()) {
                        throw new CommandLineParsingException("Options 'start' and 'end' are supported only with job mode 'tables'.");
                    }

                    return ((full | !tables.isEmpty()) & !schemas.isEmpty()) | !queries.isEmpty();
                } else if (name.equals("expgraph")) {
                    if (mode != null && !mode.equals("cartesian")) {
                        if (xlabel != null || ylabel != null) {
                            throw new CommandLineParsingException("The command EXPGRAPH with supplied the mode='" + mode + "' does not support xlabel and ylabel options.");
                        }
                    }
                }

                if (name.equals("impxml")) {
                    return this.isNotNull(format);
                } else if (name.equals("csscan")) {
                    boolean valid = !schemas.isEmpty();
                    return valid | full;
                } else if (name.equals("install")) {
                    return this.isNotNull(datafiledir);
                } else if (name.matches("impcsv|impexcel")) {
                    return this.isNotNull(filename);
                } else if (name.equals("liquibase")) {
                    if (mode.equalsIgnoreCase("migrate")) {
                        return this.areNotNull(mode, changelog);
                    }
                    return this.isNotNull(mode, local, remote); // FIXME
                } else if (name.equals("juniversal")) {
                    return this.isNotNull(filename, directory);
                } else if (name.equals("compare")) {
                    if (mode.equals("structure") || mode == null) {
                        return this.isNotNull(local, remote, tnsname);
                    }
                    if (mode.equals("data")) {
                        if (tables.size() != 2) {
                            throw new CommandLineParsingException("Was specified only a table.");
                        }
                    }
                }

                return true;
            }

            private boolean isNotNull(String... strArr) {
                for (String st : strArr) {
                    if (st != null) {
                        return true;
                    }
                }
                return false;
            }

            private boolean areNotNull(String... strArr) {
                for (String st : strArr) {
                    if (st == null) {
                        return false;
                    }
                }
                return true;
            }
        }

        @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
        public TaskOptions(final @NonNull TaskBuilder builder) {
            this.full = builder.full;
            this.schemas = builder.schemas;
            this.directory = builder.directory;
            this.exclude = builder.exclude;
            this.catalog = builder.catalog;
            this.feedback = builder.feedback;
            this.threads = builder.threads;
            this.name = builder.name;
            this.filename = builder.filename;
            this.tables = builder.tables;
            this.format = builder.format;
            this.frep = builder.frep;
            this.local = builder.local;
            this.remote = builder.remote;
            this.query = builder.query;
            this.tnsname = builder.tnsname;
            this.mode = builder.mode;
            this.items = builder.items;
            this.delimiter = builder.delimiter;
            this.flush = builder.flush;
            this.queries = builder.queries;
            this.sheets = builder.sheets;
            this.truncate = builder.truncate;
            this.param = builder.param;
            this.datafiledir = builder.datafiledir;
            this.database = builder.database;
            this.changelog = builder.changelog;
            this.start = builder.start;
            this.end = builder.end;
            this.file = builder.file;
            this.title = builder.title;
            this.xlabel = builder.xlabel;
            this.ylabel = builder.ylabel;
            this.legend = builder.legend;
            this.samples = builder.samples;
            this.comparisonName = builder.comparisonName;
            this.columns = builder.columns;
            this.scanMode = builder.scanMode;
            this.filenames = builder.filenames;
            this.winner = builder.winner;
            this.converge = builder.converge;
            this.dblink = builder.dblink;
            
            this.dbAutocommit = builder.dbAutocommit;
            this.dbTimeout = builder.dbTimeout;

        }
    }

    static abstract class ITask {

        @Getter
        protected Status status;

        @Getter
        protected TaskOptions options;

        void taskSetup() throws Exception {
        }

        abstract long task() throws Exception;

        void taskTakedown() throws Exception {
        }
    }

    public static class Task extends ITask {

//        protected static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(Task.class.getName());

//        private static final Random randomSeed = new Random(System.currentTimeMillis());
//
//        private static long nextRandomSeed() {
//            return randomSeed.nextLong();
//        }
        public static class Report {

            protected static StringBuilder report = new StringBuilder();

            private static StringBuilder preappend() {
                return report.append("");
            }

            public static String print() {
                return report.toString();
            }

            public static void append(String status) {
                preappend().append(status);
            }

            public static void appendln(String status) {
                preappend().append(status).append(System.getProperty("line.separator"));
            }

            public static void clean() {
                report.setLength(0);
            }

            public static void appendlnformat(String status, Object... args) {
                preappend().append(String.format(status, args)).append(System.getProperty("line.separator"));
            }
        }
//        protected final Random rand = new Random(nextRandomSeed());
        @Getter
        protected int cycle = 1;

        @Getter
        protected Connection connection;

        @Getter
        protected CallableStatement callable;

        @Getter
        protected PreparedStatement prepared;

        @Getter
        protected Statement statement;

        @Getter
        protected ResultSet resultSet;

        @Getter @Setter
        protected int errors, warnings;

        @Getter
        protected String taskname;

        @Getter
        protected long totalelapsedtime;

        protected TaskModeFactory factory;
        @Getter
        protected boolean isTaskCancelled;

        public Task(final TaskOptions options, final Status status) {
            super();
            this.status = status;
            this.options = options;
            this.taskname = options.name;
            this.factory = TaskModeFactory.getInstance();
        }

        private String generateStatus(final double i) {
            return round(i * 100) + "%";
        }

        private double round(double value) {
            return new BigDecimal(value).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
        }

        public void setCycle(int value) {
            this.cycle = value;
        }

        @Override
        public void taskSetup() throws SQLException, TaskException { //FIXME: oracle db...
            connection = DatabaseUtil.buildConnection("ORACLE", "enver.url", options.getDbAutocommit(), options.getDbTimeout()); //FIXME
            this.printHeadLogFile();
        }

        private void printHeadLogFile() {
            System.out.println("");
            System.out.println(String.format("Enver: Release %s - Production on %s", EnverUtil.getBuildVersion(), EnverUtil.getBuildTimestamp()));
            printBanner(System.out);
            System.out.println("");
            System.out.println(String.format("Starting task '%s'", options.name.toUpperCase()) + "...");
            System.out.println("------------------------------------------");
        }

        @Override
        public void taskTakedown() throws SQLException {
            ConnectionFactory.commit(connection);
            ConnectionFactory.close(prepared, callable, statement);
            ConnectionFactory.close(resultSet);
            ConnectionFactory.close(connection);
        }

        @Override
        public long task() throws Exception {

            taskSetup();

            Signal.handle(new Signal("INT"), new SignalHandler() {
                public void handle(Signal sig) {
                    ConnectionFactory.cancel(callable, statement, prepared);
                    isTaskCancelled = true;
                    status.printReportLine(System.out, "User requested cancel of current operation.");
                    ConnectionFactory.close(callable, statement, prepared);
                    ConnectionFactory.close(resultSet);
                    ConnectionFactory.rollback(connection);
                    ConnectionFactory.close(connection);
                    setCycle(0);
                }
            });

            long elapsedTimeDownload = -1, startTime = System.currentTimeMillis();

            taskTimed();
            elapsedTimeDownload = System.currentTimeMillis() - startTime;
            this.totalelapsedtime = elapsedTimeDownload;
            taskTakedown();

            return elapsedTimeDownload;
        }

        public void taskTimed() throws IOException, SQLException, TaskException {
            for (int i = 0; i < cycle; i++) {
                if (status != null && (i % options.feedback) == 0) {
                    status.setStatus(System.out, generateStatus((double) i / cycle));
                    taskAtomic(i);
                    status.printReport(System.out, Report.print());
                    Report.clean();
                }
            }

            status.setStatus(System.out, generateStatus(1));
        }

        public void taskAtomic(final int i) throws SQLException, TaskException {
        }

        protected void printReport(PrintStream stream, String message) {
            stream.println(message);
        }
    }
}
