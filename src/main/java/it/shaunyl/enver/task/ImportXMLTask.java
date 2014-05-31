package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.util.GeneralUtil;
import it.shaunyl.enver.io.FileExtensionFilter;
import it.shaunyl.enver.exception.TaskException;
import java.io.File;
import java.sql.*;
import java.util.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class ImportXMLTask extends EncodingEvaluation.Task {

    private double clobId = -1;

    private String owner, table;

    private List<String> listOfFiles = new ArrayList<String>();

    private final FileExtensionFilter fef = FileExtensionFilter.withExtensions("xml");

    public ImportXMLTask(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() throws SQLException, TaskException {
        super.taskSetup();

        String path = options.getDirectory();

        File folder = new File(path);
        File[] ds = folder.listFiles();

        for (final File file : ds) {
            if (fef.accept(file)) {
                listOfFiles.add(file.getAbsolutePath());
                status.printReportLine(System.out, String.format("XML source '%s' added to the queue.", file.getName()));
            }
        }
        this.setCycle(listOfFiles.size());
        connection.setAutoCommit(false);
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {
        status.printReportLine(System.out, String.format("Importing data into the table %s.%s...", owner, table));
        loadClobToTable(i);
        populateTableFromXml(i);
        Report.appendlnformat("XML source imported successfully into '%s.%s'.", owner, table);
    }

    void loadClobToTable(final int i) throws SQLException {
        callable = connection.prepareCall("BEGIN INSERT INTO tmp_xml_clob(theclob) "
                + "values (empty_clob()) return theclob, id into ?, ?; end;");
        callable.registerOutParameter(1, Types.CLOB);
        callable.registerOutParameter(2, Types.NUMERIC);
        callable.executeUpdate();
        clobId = callable.getDouble(2);

        java.sql.Clob clob = callable.getClob(1);
        GeneralUtil.fileToClobField(listOfFiles.get(i), clob);

        callable.close();
    }

    void populateTableFromXml(final int i) throws SQLException {
        callable = connection.prepareCall("BEGIN ? := encoding_verifier.LOADXML_TABLE(?, ?, ?, ?); END;");

        final String[] names = new File(listOfFiles.get(i)).getName().split("[\\s\\.?]");
        owner = names[0];
        table = names[1];

        callable.registerOutParameter(1, Types.NUMERIC);
        callable.setString(2, owner);
        callable.setString(3, table);
        callable.setString(4, options.getFormat()); //FIXME
        callable.setDouble(5, clobId);

        callable.execute();

        int rows = callable.getInt(1);
        Report.appendlnformat("%d records was loaded into '%s.%s' successfully", rows, owner, table);
//        log.info(String.format("%d records was loaded into '%s.%s' successfully.", rows, owner, table));
    }
}