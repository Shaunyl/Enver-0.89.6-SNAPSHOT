package it.shaunyl.enver.io;

import java.io.File;
import lombok.*;
import java.util.*;

import static lombok.AccessLevel.PRIVATE;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@RequiredArgsConstructor(access=PRIVATE) @ToString
public class FileExtensionFilter {
    @NonNull
    private final List<String> extensions;
    
    @NonNull
    public static FileExtensionFilter withExtensions (final @NonNull String ... args) {
        return new FileExtensionFilter(Arrays.asList(args));
    }
    
    public boolean accept (final @NonNull File path) {
        return path.isDirectory() || extensions.contains((getExtension(path)));
    }
    
    @NonNull
    private static String getExtension (final @NonNull File path) {
        final String name = path.getPath().toString();
        return name.contains(".") ? name.replaceAll("^.*\\.", "") : "";
    }
}
