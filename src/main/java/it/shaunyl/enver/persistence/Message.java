package it.shaunyl.enver.persistence;

import lombok.*;
import lombok.experimental.Wither;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@RequiredArgsConstructor
@Wither
public class Message {
    @Getter
    private final String text;
    @Getter
    private final MessageType type;

    public Message() {
        this(null, null);
    }
    public Message(final @NonNull String text) {
        this(text, MessageType.INFO);
    }

    public static enum MessageType {

        INFO,
        WARN
    }
}
