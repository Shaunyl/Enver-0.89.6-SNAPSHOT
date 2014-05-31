package it.shaunyl.enver;

import it.shaunyl.enver.EncodingEvaluation.*;
import it.shaunyl.enver.commandline.Command;
import it.shaunyl.enver.commandline.CommandLine;
import it.shaunyl.enver.exception.CommandLineParsingException;
import it.shaunyl.enver.exception.EnverException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.io.EnverConnectionStringsPropertiesFileUpdater;
import it.shaunyl.enver.io.EnverGeneralPropertiesFileUpdater;
import it.shaunyl.enver.io.IEnverPropertiesFileUpdater;
import it.shaunyl.enver.task.CharacterSetScanTask;
import it.shaunyl.enver.task.CompareSchemasTest;
import it.shaunyl.enver.task.ExportCSVTask;
import it.shaunyl.enver.task.ExportEXCELTask;
import it.shaunyl.enver.task.ExportGraphTask;
import it.shaunyl.enver.task.ExportXMLTaskAlternative;
import it.shaunyl.enver.task.ImportCSVTask;
import it.shaunyl.enver.task.ImportEXCELTask;
import it.shaunyl.enver.task.ImportXMLTask;
import it.shaunyl.enver.task.JuniversalchardetTask;
import it.shaunyl.enver.task.LiquibaseTask;
import it.shaunyl.enver.util.CommandLineUtil;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class Main {

    private TaskOptions options = null;

    private CommandLine shell = new CommandLine();

    private TaskOptions.TaskBuilder builder;

    private String parameterFile, enverPropertiesFile = "config/enver.properties";

    private static boolean logconsole = true;

    public Main() throws FileNotFoundException, IOException {
        this.buildCommands();
    }

    private void runTest(final Class<? extends Task> cmd) {
        EncodingEvaluation.Status status = new EncodingEvaluation.Status() {
            @Override
            public void setStatus(PrintStream stream, String msg) throws IOException {
                stream.println("\n> Status of operation: " + msg);
            }

            @Override
            public void printReport(PrintStream stream, String msg) {
                stream.print(msg);
            }

            @Override
            public void printReportLine(PrintStream stream, String msg) {
                stream.println(msg);
            }
        };

        try {
            long time = run(cmd, status);
//            log.info("Task successfully finished in " + time + " ms.");
        } catch (Exception e) {
            throw new UnexpectedEnverException("" + e.getMessage(), e);
        }
    }

    private long run(final Class<? extends Task> cmd, final EncodingEvaluation.Status status)
            throws IOException {

        long totalElapsedTime = 0;
        Task t = null;

        try {
            Constructor<? extends Task> constructor = cmd.getDeclaredConstructor(
                    TaskOptions.class, EncodingEvaluation.Status.class);

            constructor.setAccessible(true);
            t = constructor.newInstance(options, status);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Invalid command class: " + cmd.getName() + ".  It does not provide a constructor. " + "Available constructors are: " + Arrays.toString(cmd.getConstructors()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct command class. " + e.getMessage(), e);
        }
        try {
            totalElapsedTime = t.task();
        } catch (Exception e) {
            throw new UnexpectedEnverException(e.getMessage(), e);
        }
        CommandLineUtil.printTaskResults(t, System.out);

        return totalElapsedTime;
    }

    @SuppressWarnings("unchecked")
    public int parseCommandLine(final String[] args) throws IOException, CommandLineParsingException, EnverException {

        int errCode = -1;
        if (args.length < 1) {
            return errCode;
        }

        String mode = args[0].trim();

        if ("help".equals(mode)) {
//            log.info("Opening ENVER screen help...");
            CommandLineUtil.printEnverHelp(shell, System.out, args);
            if (!logconsole) {
                CommandLineUtil.printEnverHelp(shell, System.err, args);
            }
            return errCode;
        } else if ("version".equals(mode)) {
//            log.info("Printing ENVER version...");
            CommandLineUtil.printEnverVersion(System.out);
            if (!logconsole) {
                CommandLineUtil.printEnverVersion(System.err);
            }
            return errCode;
        } else if ("set".equals(mode)) {
            if (args.length == 1) {
                throw new CommandLineParsingException("Missing arguments. See help for more details about SET command.");
            }
//            log.info("Performing SET command...");
            this.setEnverSetting(args);
            return errCode;
        } else if ("shell".equals(mode)) {
//            log.info("Running ENVER shell...");
            this.runShell();
        } else if (!isMode(mode)) {
            if (!mode.isEmpty()) {
                throw new CommandLineParsingException(String.format("Command '%s' not found.", mode));
            }
        }

        Class<? extends Task> cmdClass = shell.determineCommandClass(mode);
        if (cmdClass == null) {
            return errCode;
        }

        builder = new TaskOptions.TaskBuilder(mode);
        File propertiesFile = new File(this.enverPropertiesFile);
        if (propertiesFile.exists()) {
//            log.info("Parsing 'enver.properties' file...");
            this.parseEnverPropertiesFile(new FileInputStream(propertiesFile));
        }

        for (int i = 1; i < args.length; i++) {
            String cmd = args[i];

            String[] splitArg = splitArg(cmd);

            String attribute = splitArg[0];
            String value = splitArg[1];

            if (attribute.equalsIgnoreCase("param")) {
                this.parameterFile = value;
                propertiesFile = new File(this.parameterFile);
                if (propertiesFile.exists()) {
//                    log.info(String.format("Found a parameter file, '%s'.", this.parameterFile));
                    this.parseParameterFile(new FileInputStream(propertiesFile));
                } else {
                    String message = String.format("Parameter file '%s' does not exist.", value);
                    throw new IOException(message);
                }
            }

            if (attribute.contains("-")) {
                throw new CommandLineParsingException("Unexpected value " + cmd + ": parameters must start with a '-'.");
            }

            if (!validate(attribute, mode)) {
                throw new CommandLineParsingException(String.format("Command '%s' does not support parameter '%s'. See 'help -%s' for more details.", mode, attribute, mode));
            }

            try {
                Field field = builder.getClass().getDeclaredField(attribute);
                field.setAccessible(true);
                this.reflectOptions(field, value);
            } catch (Exception e) {
                throw new CommandLineParsingException("Unknown parameter: '" + attribute + "'");
            }
        }

        options = builder.build();

        if (options == null) {
            throw new CommandLineParsingException(String.format("Missing required parameter for command '%s'. See 'help -%s' for more details.", mode, mode));
        }

//        log.info(String.format("Running task '%s'...", mode));
        runTest(cmdClass);
        errCode = 0;

        return errCode;
    }

    private void buildCommands() {
        shell.add(new Command(CharacterSetScanTask.class, "csscan")
                .withDescription("Full character set scan")
                .addParameter("catalog", "If disabled, allow to exclude default Oracle catalog schemas.")
                .addParameter("samples", "If disabled, allow to exclude samples Oracle schemas.")
                .addParameter("schemas", "List of the schemas being scanned into database.")
                .addParameter("exclude", "List of the schemas to be excluded.")
                .addParameter("full", "If enabled, allow to scan all the schemas into database.")
                .addParameter("frep", "If enabled, print a complete report; i.e. includes records that do not lose data."));

        shell.add(new Command(CompareSchemasTest.class, "compare")
                .withDescription("Compare data or structure of the objects of a local schema with remote ones in search of differences")
                .addParameter("local", "The name of the local schema.")
                .addParameter("remote", "The name and the password of remote schema.")
                .addParameter("tnsname", "Oracle Net Service Name for remote schema.")
                .addParameter("mode", "Specify the type of the comparison.")
                .addParameter("items", "Differencies between schemas being catched (objects, columns).")
                .addParameter("dblink", "Database link to the remote database.")
                .addParameter("columns", "This parameter specify the columns to include in the database objects being compared.")
                .addParameter("scanMode", "Indicates if the entire or a portion of the database object is compared, and in which way.")
                .addParameter("comparisonName", "The name of the comparison.")
                .addParameter("schemas", "The name of the schemas that contains the local and the remote database objects to compare.")
                .addParameter("tables", "The name of the local and remote database objects to compare.")
                .addParameter("winner", "Indicates if the column values at the local database replace the column values at the remote database or viceversa.")
                .addParameter("converge", "If true, will be also performed a converge whether data divergence was found."));

        shell.add(new Command(LiquibaseTask.class, "liquibase")
                .withDescription("Liquibase command line emulator")
                .addParameter("mode", "Running mode (diff, migrate).")
                .addParameter("format", "The format of the report (RAW, XML) for task DIFF.")
                .addParameter("filename", "The filename of the report in which the output will be saved.")
                .addParameter("changelog", "The database changelog file. This file is where all database changes are listed.")
                .addParameter("local", "String connection key to local database.")
                .addParameter("remote", "String connection key to remote database."));

        shell.add(new Command(JuniversalchardetTask.class, "juniversal")
                .withDescription("Try to guess the encoding of text files")
                .addParameter("filename", "The filename for which to guess the encoding."));

        shell.add(new Command(ExportXMLTaskAlternative.class, "expxml")
                .withDescription("Export XML files from corresponding tables or from full schemas")
                .addParameter("schemas", "List of the schemas being considered.")
                .addParameter("queries", "List of SQL statements to send to the database.")
                .addParameter("tables", "List of tables from which to generate the XML data.")
                .addParameter("full", "If enabled, allow to retrieve all tables owned by a database schema.")
                .addParameter("filenames", "List of filenames in which the QUERIES output will be saved.")
                .addParameter("start", "The line number to skip for start exporting.")
                .addParameter("end", "The last line number to retrieve for end exporting."));

        shell.add(new Command(ImportXMLTask.class, "impxml")
                .withDescription("Import XML files from corresponding tables")
                .addParameter("format", "Sets the date format."));

        shell.add(new Command(ExportCSVTask.class, "expcsv")
                .withDescription("Export CSV files from corresponding tables")
                .addParameter("query", "The query whose result set will be exported as a CSV file.")
                .addParameter("delimiter", "Specifies the delimiter of the CSV file.")
                .addParameter("start", "The line number to skip for start exporting.")
                .addParameter("filename", "The output filename.")
                .addParameter("end", "The last line number to retrieve for end exporting."));

        shell.add(new Command(ImportCSVTask.class, "impcsv")
                .withDescription("Download CSV files from corresponding tables")
                .addParameter("filename", "The filename for which to import its data.")
                .addParameter("truncate", "If enabled, the table is truncated before."));

        shell.add(new Command(ExportEXCELTask.class, "expexcel")
                .withDescription("Export EXCEL files from corresponding tables")
                .addParameter("full", "If enabled, allow to retrieve all tables owned by a database schema.")
                .addParameter("schemas", "List of the schemas whose tables are to be considered.")
                .addParameter("tables", "List of the tables for which to generate XLS. data.")
                .addParameter("format", "The format of the excel file (XLS, XLSX)..")
                .addParameter("queries", "List of SQL statements to send to the database.")
                .addParameter("sheets", "Maps sheet names with query results.")
                .addParameter("flush", "Exceeding rows will be flushed to disk from time to time. To turn off auto-flushing set to -1.")
                .addParameter("start", "The line number to skip for start exporting.")
                .addParameter("end", "The last line number to retrieve for end exporting."));

        shell.add(new Command(ImportEXCELTask.class, "impexcel")
                .withDescription("Import EXCEL files from corresponding tables")
                .addParameter("filename", "The filename for which to import its data.")
                .addParameter("truncate", "If enabled, the table is truncated before."));

        shell.add(new Command(ExportGraphTask.class, "expgraph")
                .withDescription("Export IMAGE file from corresponding trend of data")
                .addParameter("filename", "The output image file.")
                .addParameter("mode", "The type of diagram to generate.")
                .addParameter("file", "Will be considered a CSV file as the data source to be imported in order to generate the chart.")
                .addParameter("query", "Will be considered the result set of a query as the data source to be imported in order to generate the chart.")
                .addParameter("format", "The format of the image to export.")
                .addParameter("title", "The title of the chart.")
                .addParameter("xlabel", "The text of the X axis label.")
                .addParameter("ylabel", "The text of the Y axis label.")
                .addParameter("legend", "If enabled, includes the legend in the graph.")
                .addParameter("delimiter", "Specifies the delimiter of the CSV file."));

        for (Command c : shell.getCommands().values()) {
            c.addParameter("directory", "Specifies working directory.");
            c.addParameter("feedback", "Frequency job status is to be monitored where the default (1) will show new status when available.");
            c.addParameter("param", "Parameter file location");
        }

//        shell.add(new Command(InstallationTask.class, "install")
//                .withDescription("Install ENVER schema with its own functions/objects/packages")
//                .addParameter("directory", "Path to the folder that contains SQL installation files.")
//                .addParameter("datafiledir", "Path to the folder that will contain ENVER datafile."));
//
//        shell.add(new Command(UninstallationTask.class, "uninstall")
//                .withDescription("Uninstall everything related to ENVER from the database")
//                .addParameter("directory", "Path to the folder that contains SQL installation files."));

//        log.info("Commands loaded into the shell.");
    }

    public static void main(String[] args) throws IOException, CommandLineParsingException, EnverException {
        Main main = new Main();
        try {
            main.parseCommandLine(args);
        } catch (Exception e) {
            String message = e.getMessage();
//            log.error(message);
            if (!logconsole) {
                System.err.println(message);
            }
            System.out.println(e.getMessage());
        }
    }

    private void setEnverSetting(String[] args) throws IOException, EnverException, CommandLineParsingException {
        IEnverPropertiesFileUpdater ipf = null;
        String[] splitArg = splitSetArg(args[1]);
        String key = null;
        if (splitArg[0].equals("key")) {
            key = splitArg[1];
            if (args[2].matches("host=(.*?)|port=(\\d+)|schema=(.*?)|user=(.*?)|pass=(.*?)")) {
                ipf = new EnverConnectionStringsPropertiesFileUpdater();
            } else {
                throw new EnverException("Enver setting not supported or provided invalid data. Use syntax 'set key=<propkey> <subprop>=<value>'.");
            }
            String[] splitSetArg = splitSetArg(args[2]);
            ipf.writeSubValueByKey(key, splitSetArg[0], splitSetArg[1]);

        } else {
            if (args[1].matches("database.autocommit=(true|false)|log.file.path=(.*?)|log.console=(.*?)")) {
                ipf = new EnverGeneralPropertiesFileUpdater();
            } else {
                throw new EnverException("Enver setting not supported or provided invalid data. Use syntax 'set <key>=<value>'.");
            }
            String[] splitSetArg = splitSetArg(args[1]);
            ipf.writeValueByKey(splitSetArg[0], splitSetArg[1]);
        }

        System.out.println("Setting successfully updated.");
    }

    private void runShell() {
        // To run the shell do: $ ${ENVER_HOME}/bin/enver shell
        Scanner scanIn = new Scanner(System.in);
        CommandLineUtil.printShellHeader(System.out); // FIXME...
        while (true) {
//            System.out.print("shell:~$>  ");
            System.err.print("shell:~$>  ");
            String buffer = scanIn.nextLine();
            String[] commands = buffer.trim().split("-");
            //.split("\\s+");

            if (commands[0].equals("shell")) {
//                System.out.println("Enver Shell already running.");
                System.err.println("Enver Shell already running.");
                continue;
            }

            if (commands[0].equals("exit")) {
//                System.out.println("Enver Shell; closed.");
                System.err.println("Enver Shell; closed.");
                break;
            }

            for (int i = 1; i < commands.length; i++) {
                commands[i] = "-" + commands[i].trim();
            }

            if (commands[0].startsWith("set ")) {
                commands = buffer.trim().split(" ");
            }

            try {
                this.parseCommandLine(commands);
            } catch (Exception e) {
                if (!logconsole) {
                System.err.println(e.getMessage());}
                System.out.println(e.getMessage());
            }
        }
        scanIn.close();
    }

    private boolean isMode(String arg) {
        return CommandLineUtil.validateCommand(shell, arg);
    }

    private String[] splitArg(String arg) throws CommandLineParsingException {
        String[] splitArg = arg.split("=", 2);
        if (splitArg.length < 2) {
            throw new CommandLineParsingException("Could not parse '" + arg + "'.");
        }

        String option = splitArg[0];

        if (!option.contains("-")) {
            throw new CommandLineParsingException("Unexpected parameter '" + option + "': parameters must start with a '-'.");
        }

        splitArg[0] = option.replaceFirst("-", "");
        return splitArg;
    }

    private String[] splitSetArg(String arg) throws CommandLineParsingException {
        String[] splitArg = arg.split("=", 2);
        if (splitArg.length < 2) {
            throw new CommandLineParsingException("Could not parse '" + arg + "'.");
        }
        return splitArg;
    }

    private boolean validate(final String arg, String mode) {
        for (Command command : shell.getCommands().values()) {
            if (command.getName().equals(mode)) {
                for (String parameter : command.getParameterNames()) {
                    if (!arg.equals(parameter)) {
                        continue;
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void parseEnverPropertiesFile(InputStream propertiesInputStream) throws IOException, EnverException {
        Properties props = new Properties();
        props.load(propertiesInputStream);

        String logfile = null;
        logconsole = true;
        for (Map.Entry entry : props.entrySet()) {
            String value = entry.getValue().toString().trim();
            String key = (String) entry.getKey();
            if (key.equalsIgnoreCase("database.autocommit")) {
                builder.setDbAutocommit(Boolean.parseBoolean(value));
            } else if (key.equalsIgnoreCase("log.console")) {
                logconsole = Boolean.parseBoolean(value);
            } else if (key.equalsIgnoreCase("database.timeout")) {
                builder.setDbTimeout(Integer.parseInt(value));
            }

            if (key.equalsIgnoreCase("log.file.path")) {
                logfile = value + "/enver.log";
            }
        }

        if (!logconsole && logfile != null) {
            System.setOut(new PrintStream(new File(logfile)));
            if (!logconsole) {
            System.setErr(System.err);}
        }
    }

    private void parseParameterFile(InputStream propertiesInputStream) throws IOException, CommandLineParsingException {
        Properties props = new Properties();
        props.load(propertiesInputStream);

        for (Map.Entry entry : props.entrySet()) {
            try {
                String value = entry.getValue().toString().trim();
                String key = (String) entry.getKey();

                Field field = builder.getClass().getDeclaredField(key);
                field.setAccessible(true);

                Object currentValue = field.get(builder);
                if (field.getType().equals(java.util.List.class)) {
                    if (((ArrayList) currentValue).isEmpty()) {
                        this.reflectOptions(field, value);
                    }
                } else {
                    this.reflectOptions(field, value);
                }
            } catch (Exception e) {
                throw new CommandLineParsingException("Unknown parameter: '" + entry.getKey() + "'.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void reflectOptions(final Field field, final String value) throws Exception {

        if (field.getType().equals(Boolean.class)) {
            field.set(builder, Boolean.valueOf("y".equals(value) ? "true" : "false"));
        } else if (field.getType().equals(java.util.List.class)) {
            String pattern = "\\s*,\\s";
            if (field.getName().equalsIgnoreCase("queries")) {
                pattern = "\\s*;\\s";
            }
            List<String> list = ((List<String>) field.get(builder));
            if (!value.isEmpty()) {
                Set<String> set = new LinkedHashSet<String>(Arrays.asList(value.split(pattern)));
                list.addAll(set);
            }
        } else if (field.getType().equals(Integer.class)) {
            field.set(builder, Integer.valueOf(value));
        } else if (field.getType().equals(char.class)) {
            field.set(builder, value.charAt(0));
        } else {
            field.set(builder, value);
        }
    }
}