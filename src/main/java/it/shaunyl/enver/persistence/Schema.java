package it.shaunyl.enver.persistence;

import java.util.*;
import lombok.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@RequiredArgsConstructor
public class Schema implements Comparable<Schema> {

    @Getter
    @NonNull
    private final String name;
    @Getter
    private final List<Table> tables = new ArrayList<Table>();
    
    @Override
    public int compareTo(Schema other) {
        int last = this.name.compareTo(other.name);
        return last == 0 ? this.name.compareTo(other.name) : last;
    }

    public void addTable(Table table) {
        tables.add(table);
    }
}
