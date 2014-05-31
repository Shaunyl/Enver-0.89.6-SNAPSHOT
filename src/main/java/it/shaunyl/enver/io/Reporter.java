package it.shaunyl.enver.io;

import it.shaunyl.enver.persistence.Schema;
import it.shaunyl.enver.persistence.Column;
import it.shaunyl.enver.persistence.Record;
import it.shaunyl.enver.persistence.Table;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import it.shaunyl.enver.util.GeneralUtil;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@Deprecated
public class Reporter {

    private static PrintWriter writer;

    private static void printLine(final String table, final String column, final String record, final String recordMessage) {
        writer.printf("%-20s %-25s %-40s %-60s%n", table, column, record, recordMessage);
    }

    private static void printLine(final String table, final String column, final String column2) {
        writer.printf("%-40s %-40s %-20s%n", table, column, column2);
    }

    private static String shrink(String inputString, int MAX_CHAR) {
        int maxLength = (inputString.length() < MAX_CHAR) ? inputString.length() : MAX_CHAR;
        return inputString.substring(0, maxLength);
    }

    @Deprecated
    private static void tempME(final List<String> items) { // FIXME... non va bene... funziona solo in sto caso..
        if (items.contains("objects")) {
            printLine("Object Name", "Object Type", "Missing");
            printLine(GeneralUtil.repeat("-", 40), GeneralUtil.repeat("-", 40), GeneralUtil.repeat("-", 20));
            items.remove("objects");
        } else if (items.contains("columns")) {
            printLine("Table/View", "Column", "Missing");
            printLine(GeneralUtil.repeat("-", 40), GeneralUtil.repeat("-", 40), GeneralUtil.repeat("-", 20));
            items.remove("columns");
        }
    }

    public static void printCompare(final String path, final Schema schema, final List<String> items) {
        openWriter(path);
        printLine("\nTask (compare): differences between schemas\n\n" + schema.getName() + "\n", "\n", "");
        tempME(items);

        for (Table table : schema.getTables()) {
            table.getName();
            List<Column> columns = table.getColumns();
            if (columns.size() > 0) {
                for (int i = 0; i < 1; i++) { // FIXME: c'è sempre e solo una colonna.... (no ora due..)
                    printLine(shrink(table.getName(), 40), shrink(columns.get(i).getName(), 40), shrink(columns.get(i + 1).getName(), 20));
                }
            } else {
                writer.println();
                writer.println();
                tempME(items);
            }
        }

//        log.info("Compare task report has been generated.");

        closeWriter();
    }

    public static int printCsscan(final String path, final List<Schema> schemas, boolean warnOnly) {
        boolean check = false;
        int warning = 0;
        boolean newlines = false;
        int gbwarns = 0;
        
        openWriter(path);
        printLine("\nTask (csscan): perform a Character Set Scan\n\n", "\n", "");

        for (Schema schema : schemas) {
            printLine("Schema: " + schema.getName() + "\n", "", "", "");
            for (Table table : schema.getTables()) {
                for (Column column : table.getColumns()) {
                    for (Record record : (!warnOnly) ? (column.getRecords()) : column.getWarningRecords()) {
                        if (!check) {
                            check = true;
                            printLine("Table", "Column", "Record", "Message");
                            printLine(GeneralUtil.repeat("-", 20), GeneralUtil.repeat("-", 25), GeneralUtil.repeat("-", 40), GeneralUtil.repeat("-", 80));
                        }
                        printLine(shrink(table.getName(), 20), shrink(column.getName() + "(" + column.getMaxLength() + ")", 25), shrink(record.getName(), 40), record.getMessage().getText());
                        ++warning;
                        newlines = true;
                    }
                    if (newlines) {
                        writer.append("\n\n");
                        newlines = false;
                    }
                    check = false;
                }
            }

            if (warning == 0) {
                final String result = "No loss of information was detected passing from WE8MSWIN1252 to UTF-8\n\n";
                writer.append(result);
            }
            gbwarns += warning;
            warning = 0;
        }

//        log.info("Scan report has been generated.");
        closeWriter();
        
        return gbwarns;
    }

    private static void openWriter(final String path) {
        try {
            writer = new PrintWriter(new FileWriter(path));
        } catch (IOException e) {
        }
    }

    private static void closeWriter() {
        writer.close();
    }
}
