package it.shaunyl.enver.commandline;

import lombok.*;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@AllArgsConstructor @RequiredArgsConstructor
public class Parameter {

    @Getter @NonNull
    private final String name;
    @Getter
    private String description;
}