package it.shaunyl.enver.persistence;

import lombok.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@RequiredArgsConstructor
public class Record {

    @Getter @NonNull
    private final String name;
    @Getter
    private final int length;
    @Getter @Setter
    private Message message;
}
