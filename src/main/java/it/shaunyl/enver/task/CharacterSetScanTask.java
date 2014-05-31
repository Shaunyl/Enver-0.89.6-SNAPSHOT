package it.shaunyl.enver.task;

import it.shaunyl.enver.database.ConnectionFactory;
import it.shaunyl.enver.database.OracleSchemas;
import it.shaunyl.enver.util.GeneralUtil;
import it.shaunyl.enver.io.Reporter;
import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.EncodingEvaluation.*;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.persistence.Column;
import it.shaunyl.enver.persistence.Message;
import it.shaunyl.enver.persistence.Record;
import it.shaunyl.enver.persistence.Schema;
import it.shaunyl.enver.persistence.Table;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import oracle.sql.ARRAY;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class CharacterSetScanTask extends EncodingEvaluation.Task {

    private final String typeObjSchemas = "T_TAB_SCHEMAS";

    private List<Schema> schemas = new ArrayList<Schema>();

    private List<String> all_schemas;

    private Schema schema;

    public CharacterSetScanTask(TaskOptions options, Status status) {
        super(options, status);
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {
        final String schema_name = all_schemas.get(i);
        setParameters(schema_name);
        Report.appendlnformat("Analyzing schema '%s'...", schema_name);
        callable.execute();

        Object[] data = (Object[]) ((Array) callable.getObject(2)).getArray();
        schema = new Schema(schema_name);
        this.buildTree(data);
        schemas.add(schema);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {
        super.taskSetup();

        if (options.getFull()) {
            this.countSchemas();
        }

        all_schemas = options.getSchemas();

        List<String> excludes = options.getExclude();
        if (!options.getCatalog()) {
            excludes.addAll(OracleSchemas.Default.list);
        }
        if (!options.getSamples()) {
            excludes.addAll(OracleSchemas.Samples.list);
        }

        if (excludes.size() > 0) {
            all_schemas.removeAll(excludes);
        }
        if (all_schemas.isEmpty()) {
            warnings += 1;
            status.printReportLine(System.out, " - WARN: There are no schemas to be scanned. Probably schemas and {catalog|samples} parameters are in conflict.");
            throw new UnexpectedEnverException("There are no schemas to be scanned. Probably schemas and {catalog|samples} parameters are in conflict.");
        }

        int size = all_schemas.size();
        status.printReportLine(System.out, " - This task is about to scan " + size + " schemas.");

        super.setCycle(size);

        callable = connection.prepareCall("{ call encoding_verifier.character_set_scan(?, ?) }");
    }

    @Override
    public void taskTakedown() throws SQLException { //FIXME: cartella da impostare..
        super.taskTakedown();
        int warns = Reporter.printCsscan(options.getDirectory() + "/csscan.log", schemas, !options.getFrep());
        warnings += warns;
        if (warns > 0) {
            status.printReportLine(System.out, "Seems like some schema was bad. See '~/csscan.log' log file for more details.");
        } else {
            status.printReportLine(System.out, "No loss of information seems to be detected. Anyway a file log '~/csscan.log' has been generated.");
        }
    }

    private void countSchemas() {
        Statement stmt = null;
        ResultSet rs = null;
        options.getSchemas().clear();
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("SELECT username FROM all_users WHERE username <> 'ENVER'");
            while (rs.next()) {
                options.getSchemas().add(rs.getString(1));
            }
        } catch (SQLException e) {
        } finally {
            ConnectionFactory.close(stmt);
            ConnectionFactory.close(rs);
        }
    }

    private void handleRecords(ARRAY array, Column column) throws SQLException {
        Object[] arrayOfRecords = (Object[]) array.getArray();
        int maxLength = column.getMaxLength();
        for (Object records : arrayOfRecords) {
            final String record = records.toString();
            Record r = this.createRecord(record, maxLength);
            column.addRecord(r);
        }
    }

    private Column handleColumns(Object[] objs, Table table) throws SQLException {
        Column column = null;
        for (Object o : objs) {
            Struct struct = (Struct) o;
            for (Object attribute : struct.getAttributes()) {
                if (attribute == null) {
                    continue;
                }
                if (attribute instanceof String) {
                    column = new Column(attribute.toString());
                }
                if (attribute instanceof BigDecimal) {
                    column.setMaxLength(Integer.parseInt(attribute.toString()));
                }
                if (attribute instanceof ARRAY) {
                    handleRecords((ARRAY) attribute, column);
                    table.addColumn(column);
                }
            }
        }
        return column;
    }

    private Table handleTables(Object[] obj, Table table) throws SQLException {
        for (Object o : obj) {
            Struct struct = (Struct) o;
            for (Object attribute : struct.getAttributes()) { //-------------
                if (attribute == null) {
                    continue;
                }
                if (attribute instanceof String) {
                    table = new Table(attribute.toString());
                }
                if (attribute instanceof ARRAY) {
                    ARRAY arrayAttribute = ((ARRAY) attribute);
                    String sqlTypeName = arrayAttribute.getSQLTypeName();
                    Object[] resultSet = (Object[]) arrayAttribute.getArray();
                    if (sqlTypeName.equals("ENVER.T_COLUMNS")) {
                        this.handleColumns(resultSet, table);
                    }
                }
            }
        }
        return table;
    }

    private void decomposeObject(Object tmp) throws SQLException {
        Table table = null;

        Struct row = (Struct) tmp;

        for (Object attribute : row.getAttributes()) {
            if (attribute == null) {
                continue;
            }

            if (attribute instanceof ARRAY) {
                ARRAY array = ((ARRAY) attribute);
                String sqlTypeName = array.getSQLTypeName();
                Object[] resultSet = (Object[]) array.getArray();

                if (sqlTypeName.equals("ENVER.T_TABLES")) {
                    table = this.handleTables(resultSet, table);
                }
            }
        }
        if (table != null) {
            schema.addTable(table);
        }
    }

    private void buildTree(Object[] data) throws SQLException {
        for (Object tmp : data) {
            decomposeObject(tmp);
        }
    }

    private Record createRecord(final String value, final int len) {
        Record record = new Record(value, value.length());
        final int newLength = GeneralUtil.utf8StringLength(value);
        if (newLength > len) {
            Message message = new Message()
                    .withText(String.format("Warning: value too large for column string (actual: %d, expanded: %d, maximum: %d)", value.length(), newLength, len))
                    .withType(Message.MessageType.WARN);
            record.setMessage(message);
//            log.debug(String.format("Record: \"%s\":\n\t\t> %s", record.getName(), record.getMessage().getText()));

        } else {
            Message message = new Message()
                    .withText("Info: no data loss found")
                    .withType(Message.MessageType.INFO);
            record.setMessage(message);
//            log.debug(String.format("Record: %s, %s", record.getName(), record.getMessage().getText()));
        }
        return record;
    }

    private void setParameters(String schema) throws SQLException {
        callable.registerOutParameter(2, Types.ARRAY, typeObjSchemas);
        callable.setString(1, schema.toUpperCase());
    }
}