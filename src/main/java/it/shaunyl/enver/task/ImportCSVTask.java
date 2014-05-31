package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.database.ConnectionFactory;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.task.reader.core.CSVReader;
import java.io.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 *
 * @author Filippo
 */
public class ImportCSVTask extends EncodingEvaluation.Task {

    private List data;

    private List<String> columns;

    private String[] tableTypes;

    private int columnsLen;

    private boolean isSep;

    private String schema, table;

    private CSVReader reader;

//    private String columns_metadata_table, column_id;
    public ImportCSVTask(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
//        if (options.getDatabase().equals("mysql")) {
//            String sql = String.format("SELECT data_type, column_name"
//                    + " FROM information_schema.columns"
//                    + " WHERE owner = '%s'"
//                    + " AND table_name = '%s'"
//                    + " ORDER BY ordinal_position", schema, table);
//        } else {
//            String sql = String.format("SELECT data_type, column_name"
//                    + " FROM dba_tab_columns"
//                    + " WHERE owner = '%s'"
//                    + " AND table_name = '%s'"
//                    + " ORDER BY column_id", schema, table);
//        }
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {
        super.taskSetup();
        String filename = options.getFilename();
        String[] split = new File(filename).getName().split("[\\s\\.?]");
        schema = split[0].toUpperCase();
        table = split[1].toUpperCase();

        try {
            char delimiter = options.getDelimiter();
            reader = (delimiter == '\u0000') ? new CSVReader(new FileReader(filename))
                    : new CSVReader(new FileReader(filename), delimiter);
            data = reader.readAll();
        } catch (IOException e) {
            throw new UnexpectedEnverException("Seems like something went bad with the reader.", e); //TaskException..
        }

        String sql = this.prepareQuery();
        status.printReportLine(System.out, String.format("Query: '%s';", sql));
        columnsLen = columns.size();
        status.printReportLine(System.out, String.format("Number of columns: '%d'.", columnsLen));

        prepared = connection.prepareStatement(sql);

        this.getColumnTypes(columnsLen);

        if (isSep) {
            data.remove(0);  // INFOME: remove sep=? EXCEL function..
            data.remove(0);  // INFOME: remove header line..
        } else {
            data.remove(0);
        }
        int records = data.size();
        status.printReportLine(System.out, String.format("Number of record to be inserted: '%d'.", records));
        setCycle(records);

        if (options.getTruncate()) {
            deleteFrom(String.format("%s.%s", schema, table));
            status.printReportLine(System.out, String.format("\nTable %s.%s has been truncated.", schema, table));
        }
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {
        setParameters(i);
        status.printReportLine(System.out, String.format("Importing data into the table %s.%s...", schema, table));
        prepared.execute();
        Report.appendlnformat("Tuple inserted successfully into '%s.%s'.", schema, table);
    }

    private String prepareQuery() {
        Object[] nextLine = null;
        columns = new ArrayList<String>();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("INSERT INTO %s.%s (", schema, table));

        Iterator iter = data.iterator();
        if (iter.hasNext()) {
            nextLine = (Object[]) iter.next();
            if (nextLine.length == 1 && nextLine[0].toString().matches("sep=\\p{P}")) {
                isSep = true;
                nextLine = (Object[]) iter.next();
            }
            if (nextLine == null) {
                throw new UnexpectedEnverException("Seems like something went bad with data.");
            }

            String firstColumn = nextLine[0].toString();
            columns.add(firstColumn);
            sb.append(firstColumn);
            for (int i = 1; i < nextLine.length; i++) {
                String col = nextLine[i].toString();
                columns.add(col);
                sb.append(',').append(col);
            }
            sb.append(") VALUES (?");
            for (int i = 1; i < nextLine.length; i++) {
                sb.append(", ?");
            }
            sb.append(')');
        }
        return sb.toString();
    }

    private void getColumnTypes(int columnsCount) throws SQLException {
        tableTypes = new String[columnsCount];
        statement = connection.createStatement();
        String sql = String.format("SELECT data_type, column_name FROM dba_tab_columns WHERE owner = '%s' AND table_name = '%s' ORDER BY column_id", schema, table);
        ResultSet localRs = statement.executeQuery(sql);
        int k = 0;
        boolean isColumn = true;
        while (localRs.next()) {
            String column_name = localRs.getString("COLUMN_NAME");
            for (int j = 0; j < columnsCount; j++) {
                if (columns.get(j).equalsIgnoreCase(column_name)) {
                    isColumn = false;
                }
            }
            if (isColumn) {
                isColumn = true;
                continue;
            }
            String data_type = localRs.getString("DATA_TYPE");
            tableTypes[k] = data_type; //MAYBE: a map should be better
            k++;
            if (columnsCount == k) {
                break;
            }
        }

        ConnectionFactory.close(statement);
        ConnectionFactory.close(localRs);
    }

    private void setParameters(int i) throws SQLException {
        Object[] nextLine = (Object[]) data.get(i);
        for (int j = 0; j < nextLine.length; j++) {
            if (tableTypes[j].matches("CHAR|VARCHAR|VARCHAR2")) {
                prepared.setString(j + 1, nextLine[j].toString());
            } else if ("NUMBER".equals(tableTypes[j])) {
                if (nextLine[j].toString().isEmpty()) {
                    prepared.setNull(j + 1, Types.NUMERIC);
                } else {
                    prepared.setBigDecimal(j + 1, BigDecimal.valueOf(Double.parseDouble(nextLine[j].toString())));
                }
            } else if (tableTypes[j].matches("TIMESTAMP\\([0-9]+\\)|DATE")) {
                if (nextLine[j].toString().isEmpty()) {
                    prepared.setNull(j + 1, Types.TIMESTAMP);
                } else {
                    java.util.Date date = new java.util.Date(nextLine[j].toString());
                    prepared.setTimestamp(j + 1, new Timestamp(date.getTime()));
                }
            } else if (tableTypes[j].matches("CLOB")) {
                if (nextLine[j].toString().isEmpty()) {
                    prepared.setNull(j + 1, Types.CLOB);
                } else {
                    Clob clob = connection.createClob();
                    clob.setString(1, nextLine[j].toString());
                    prepared.setClob(j + 1, clob);
                }
            }
        }
    }

    private void deleteFrom(String ext_table) throws SQLException {
        statement = connection.createStatement();
        String sql = "DELETE " + ext_table;
        statement.execute(sql);
        statement.close();
    }
}
