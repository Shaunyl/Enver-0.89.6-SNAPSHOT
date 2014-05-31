package it.shaunyl.enver.util;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.commandline.Command;
import it.shaunyl.enver.commandline.CommandLine;
import it.shaunyl.enver.commandline.Parameter;
import it.shaunyl.enver.exception.CommandLineParsingException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class CommandLineUtil {

    private static Collection<Command> getCommands(CommandLine shell/*, boolean expanded*/, String... args) throws CommandLineParsingException {
        Collection<Command> commands = new ArrayList<Command>();
        if (args.length == 1) { // ALL COMPACT
            return shell.getCommands().values();
        }

//        int start = expanded ? 2 : 1;

        for (Iterator<Command> it = shell.getCommands().values().iterator(); it.hasNext();) {
            Command command = it.next();
            for (int i = 1; i < args.length; i++) {
                boolean valid = validateCommand(shell, args[i].replace("-", ""));
                if (!valid) {
                    throw new CommandLineParsingException(String.format("The command '%s' is not supported.", args[1]));
                }
                if (command.getName().equalsIgnoreCase(args[i].replace("-", ""))) {
                    commands.add(command);
                }
            }
        }

        return commands;
    }

    private static void printHelp(CommandLine shell, PrintStream stream, String... args) throws CommandLineParsingException {
        stream.println("Standard Commands:\n");
        Map<String, String> global = new HashMap<String, String>();
        boolean expanded = false;
//        if (args.length == 2 && "-a".equals(args[1])) {
//            expanded = true;
//        }
        Collection<Command> commands = getCommands(shell/*, expanded*/, args);

        for (Iterator<Command> it = commands.iterator(); it.hasNext();) {
            Command command = it.next();
            stream.println(String.format(" %-15s %s", command.getName().toUpperCase(), command.getDescription() + ":"));
//            if (expanded) {
                List<Parameter> parameters = command.getParameters();
                for (Parameter commandParameter : parameters) {
                    String name = commandParameter.getName();
                    String description = commandParameter.getDescription();
                    if (!name.matches("feedback|directory|param")) {
                        stream.println(String.format("    > %-35s %s", name, description));
                    } else {
                        global.put(name, description);
                    }
                }
//            }
        }
//        if (expanded) {
            stream.println("");
            stream.println("Global Parameters:");
            Iterator<Map.Entry<String, String>> it = global.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> next = it.next();
                stream.println(String.format("    > %-35s %s", next.getKey(), next.getValue()));
            }
//        }
        stream.println("");
        stream.println("Configuration files (.properties):");
        stream.println(String.format("    > %-35s %s", "connectionStrings", "Lists all the string connections."));
        stream.println(String.format("    > %-35s %s", "enver", "Lists global ENVER properties."));
        stream.println(String.format("    > %-35s %s", "excelFormatting", "Lists all supported properties for EXCEL tasks."));
        stream.println("");
    }

    /**
     * Validates a command, i.e. Checks if a command is supported by the ENVER
     * shell.
     *
     * @param shell An objects that represents the ENVER shell.
     * @param arg The ENVER shell command to validate.
     * @return 'true' if the command is supported. Otherwise it returns 'false'.
     */
    public static boolean validateCommand(CommandLine shell, String arg) {
        Collection<Command> values = shell.getCommands().values();
        for (Iterator<Command> it = values.iterator(); it.hasNext();) {
            Command command = it.next();
            if (command.getName().equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prints the current release version of ENVER.
     *
     * @param stream The output stream.
     */
    public static void printEnverVersion(PrintStream stream) {
        stream.println("");
        printBanner(stream);
        stream.println("");
        stream.println("Printing Enver version...");
        stream.println("------------------------------------------");
        stream.println(String.format("Beta %s - Production on %s", EnverUtil.getBuildVersion(), EnverUtil.getBuildTimestamp()));
        stream.println("------------------------------------------");
    }

    /**
     * Prints the ENVER shell header.
     *
     * @param stream The output stream.
     */
    public static void printShellHeader(PrintStream stream) {
        stream.println("Enver Shell; enter 'help<RETURN>' for list of supported commands.");
        stream.println(String.format("Version: 0.89.6-beta, %s - %s", EnverUtil.getBuildVersion(), EnverUtil.getBuildTimestamp())); //FIXME
    }

    /**
     * Prints the ENVER banner.
     *
     * @param stream The output stream.
     */
    public static void printBanner(PrintStream stream) {
        stream.println("Copyright (c) 2014 Ansaldo STS and/or its affiliates. All right reserved.");
    }

    /**
     * Prints the results of a runned task.
     *
     * @param t The task executed.
     * @param stream The output stream.
     */
    public static void printTaskResults(EncodingEvaluation.Task t, PrintStream stream) {
        int warnings = t.getWarnings();
        int errors = t.getErrors();
        String task = t.getTaskname().toUpperCase();
        String currentDate = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String result;
        if (warnings + errors > 0) {
            result = String.format("Task '%s' completed at %s with errors (%d) and warnings (%d)", task, currentDate, errors, warnings);
        } else {
            result = String.format("Task '%s' successfully completed at %s", task, currentDate);
        }
        stream.println("\n------------------------------------------");
        stream.println(result);
        stream.println("Elapsed time: " + t.getTotalelapsedtime() + " ms");
    }

    /**
     * Prints ENVER help on shell.
     *
     * @param shell ENVER shell.
     * @param stream The output stream.
     */
    public static void printEnverHelp(CommandLine shell, PrintStream stream, String... args) throws CommandLineParsingException {
        if (args.length <= 1) {
            stream.println("Usage: java -jar <ENVER-VERSION.jar> <COMMAND> [-arg1=value1] [-arg2=value2]  ...  [-argN=\"value(N -i) .. value(N)\"]");
            stream.println("");
            stream.println("    > HELP [-COMMAND1] ... [-COMMANDn]    Prints this message. If supplied with <COMMAND> parameters, it prints only the help about those commands.");
            stream.println("    > SET  key=value [prop=value]         Set ENVER property list file settings. The 'prop' options is used in order to update subkey values.");
            stream.println("    > SHELL                               Open ENVER shell.");
            stream.println("    > VERSION                             Prints ENVER version.");
            stream.println("");
        }
        CommandLineUtil.printHelp(shell, stream, args);
    }

    /**
     * Prints entire help supplied with error messages.
     *
     * @param errorMessages The error messages being printed.
     * @param stream The output stream.
     */
    public static void printHelp(CommandLine shell, List<String> errorMessages, PrintStream stream) throws CommandLineParsingException {
        stream.println("Errors:");
        for (String message : errorMessages) {
            stream.println("  " + message);
        }
        stream.println();
        printEnverHelp(shell, stream);
    }
}
