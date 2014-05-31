package it.shaunyl.enver.persistence;

import java.util.*;
import lombok.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@RequiredArgsConstructor @AllArgsConstructor
public class Column {

    @Getter
    @NonNull
    private final String name;
    @Getter @Setter
    private int maxLength;
    @Getter
    private final List<Record> records = new ArrayList<Record>();

    public void addRecord(Record record) {
        records.add(record);
    }

    public List<Record> getWarningRecords() {
        final List<Record> warnRecords = new ArrayList<Record>();
        for (Record record : records) {
            if (record.getMessage().getType().equals(Message.MessageType.WARN)) {
                warnRecords.add(record);
            }
        }
        return warnRecords;
    }
}
