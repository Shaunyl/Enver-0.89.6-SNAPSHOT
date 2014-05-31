package it.shaunyl.enver.commandline;

import it.shaunyl.enver.EncodingEvaluation.*;
import java.util.*;
import lombok.Getter;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class CommandLine {

    @Getter
    private Map<String, Command> commands = new TreeMap<String, Command>();

    public void add(Command command) {
        commands.put(command.getName(), command);
    }

    public Class<? extends Task> determineCommandClass(String cmd) {
        Command descriptor = commands.get(cmd);
        return descriptor != null ? descriptor.getCmdClass() : null;
    }

    public void print() {
        for (Iterator<Command> it = this.getCommands().values().iterator(); it.hasNext();) {
            Command command = it.next();
            System.out.println(String.format(" %-15s %s", command.getName().toUpperCase(), command.getDescription() + ":"));
            List<Parameter> parameters = command.getParameters();
            for (Parameter commandParameter : parameters) {
                System.out.println(String.format("    > %-15s %s", commandParameter.getName(), commandParameter.getDescription()));
            }
        }
    }
}
