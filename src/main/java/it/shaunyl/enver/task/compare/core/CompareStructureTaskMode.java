package it.shaunyl.enver.task.compare.core;

import it.shaunyl.enver.io.Reporter;
import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.persistence.Column;
import it.shaunyl.enver.persistence.Schema;
import it.shaunyl.enver.persistence.Table;
import it.shaunyl.enver.task.compare.ICompareTaskMode;
import java.sql.*;
import java.util.List;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class CompareStructureTaskMode extends ICompareTaskMode {

    private final String typeObjCompares = "T_COMPARE_TABLE";

    private String local, remote_schema, remote_password, item_list, filename;

    private Schema schema;

    private List<String> items;

    public CompareStructureTaskMode() {
    }

    public boolean identify(String modality) {
        return "structure".equals(modality);
    }

    private void setConfiguration(EncodingEvaluation.Task task) {
        this.task = task;
        this.options = task.getOptions();
        this.status = task.getStatus();
        this.connection = task.getConnection();
    }

    @Override
    public void setup(EncodingEvaluation.Task task) throws SQLException {
        setConfiguration(task);
        this.callable = this.connection.prepareCall("{ call comparison_manual.compare_db_objects(?, ?, ?, ?, ?, ?) }");
        local = options.getLocal();
        String remote = options.getRemote();
        String[] split = remote.split("/");
        remote_password = split[1];
        remote_schema = split[0];
        schema = new Schema("LOCAL:\n  > schema: " + local + "\n\nREMOTE:\n  > schema: " + remote_schema + "\n  > tnsname: " + options.getTnsname()); //FIXME.. non è sempre il local. Se faccio REMOTE-LOCAL sarebbe remote.
        // a meno che non la veda solo dal punto di vista LOCAL.

        items = options.getItems();
        if (items.isEmpty()) {
            items.add("objects");
            items.add("columns");
        }

        item_list = items.get(0);
        status.printReportLine(System.out, String.format("%d) Difference being retrieved: '%s'.", 1, item_list));
        for (int i = 1; i < items.size(); i++) {
            String item = items.get(i);
            item_list += ":" + item;
            status.printReportLine(System.out, String.format("%d) Difference being retrieved: '%s'.", i + 1, item));
        }

        filename = options.getDirectory() + "/compare.log";
    }

    @Override
    public void run(final int i) throws SQLException {
        setParameters();
        EncodingEvaluation.Task.Report.appendln("Executing SQL statement...");
        this.callable.execute();

        Object[] data = (Object[]) ((Array) this.callable.getObject(5)).getArray();

        EncodingEvaluation.Task.Report.appendln("Recovering data...");
        this.buildTree(data);
        EncodingEvaluation.Task.Report.appendlnformat("Operation completed successfully. See log file '%s' for more details.", filename);

    }

    @Override
    public void takedown() throws SQLException {
        Reporter.printCompare(options.getDirectory() + "/compare.log", schema, items);
    }

    private void fetch(Object tmp) throws SQLException {

        Struct row = (Struct) tmp;

        Object[] attributes = row.getAttributes();

        Table table = new Table(attributes[0].toString());
        Column column = new Column(attributes[1].toString());
        table.addColumn(column);
        Column column2 = new Column(attributes[2].toString());
        table.addColumn(column2);
        schema.addTable(table);
    }

    private void buildTree(Object[] data) throws SQLException {

        for (int i = 0; i < data.length; i++) {
            Object[] diff_x = (Object[]) ((Array) data[i]).getArray();
            for (Object d : diff_x) {
                fetch(d);
            }
            if (i != data.length - 1) {
                schema.addTable(new Table("@"));
            }
        }
    }

    private void setParameters() throws SQLException {
        this.callable.registerOutParameter(5, Types.ARRAY, typeObjCompares);
        this.callable.setString(1, local);
        this.callable.setString(2, remote_schema);
        this.callable.setString(3, remote_password);
        this.callable.setString(4, options.getTnsname());
        this.callable.setString(6, item_list);
    }
}
