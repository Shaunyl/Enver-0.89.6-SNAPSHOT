package it.shaunyl.enver.commandline;

import it.shaunyl.enver.EncodingEvaluation.*;
import java.util.*;
import lombok.*;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class Command {

    @Getter
    @NonNull
    private final Class<? extends Task> cmdClass;
    @Getter
    @NonNull
    private final String name;
    @Getter
    private String description;
    @Getter
    private List<Parameter> parameters = new ArrayList<Parameter>();

    public Command(final @NonNull Class<? extends Task> cmdClass, final @NonNull String name) {
        this.name = name;
        this.cmdClass = cmdClass;
    }

    public Command withDescription(String description) {
        this.description = description;
        return this;
    }

    public Command addParameter(final @NonNull String name, final String description) {
        Parameter param = new Parameter(name, description);
        parameters.add(param);
        return this;
    }

    public Command addParameter(final @NonNull String name) {
        addParameter(name, null);
        return this;
    }

    public List<String> getParameterNames() {
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < parameters.size(); i++) {
            names.add(parameters.get(i).getName());
        }
        return names;
    }
}
