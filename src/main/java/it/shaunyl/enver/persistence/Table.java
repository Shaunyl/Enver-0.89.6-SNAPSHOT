package it.shaunyl.enver.persistence;

import java.util.*;
import lombok.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@RequiredArgsConstructor
public class Table {

    @Getter
    @NonNull
    private final String name;
    @Getter
    private final List<Column> columns = new ArrayList<Column>();

    public void addColumn(Column column) {
        columns.add(column);
    }
}
