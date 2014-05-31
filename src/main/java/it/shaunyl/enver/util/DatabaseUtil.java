/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.shaunyl.enver.util;

import it.shaunyl.enver.database.ConnectionFactory;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.sql.DataSource;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class DatabaseUtil {

    private static final String BLOB_FORMATTER = "<BLOB>";

    private static final SimpleDateFormat TIMESTAMP_FORMATTER =
            new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("dd-MMM-yyyy");

    public static Connection buildConnection(String database, String url, boolean autocommit, int seconds) throws SQLException {
        ConnectionFactory.DataProvider dataprovider = ConnectionFactory.DataProvider.valueOf(database); //FIXME
        DataSource datasource = ConnectionFactory.getDataSource(dataprovider, url);
        datasource.setLoginTimeout(seconds);
        Connection connection = datasource.getConnection();
        connection.setAutoCommit(autocommit);
        return connection;
    }

    public static List<String> getAllTables(Connection conn, String schema) {
        Statement stmt = null;
        ResultSet rs = null;
        List<String> tables = new ArrayList<String>();
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT table_name FROM dba_tables WHERE owner = '" + schema + "'");
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new UnexpectedEnverException(e.getMessage(), e);
        } finally {
            ConnectionFactory.close(stmt);
            ConnectionFactory.close(rs);
        }
        return tables;
    }

    @Deprecated
    public static List<String> buildQuerySelectAllTables(Connection conn, String schema) {
        Statement stmt = null;
        ResultSet rs = null;
        List<String> tables = new ArrayList<String>();
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT table_name FROM dba_tables WHERE owner = '" + schema + "'");
            while (rs.next()) {
                tables.add(String.format("SELECT * FROM %s.%s", schema, rs.getString(1)));
            }
        } catch (SQLException e) {
            throw new UnexpectedEnverException(e.getMessage(), e);
        } finally {
            ConnectionFactory.close(stmt);
            ConnectionFactory.close(rs);
        }
        return tables;
    }

    public static String getColumnValue(ResultSet rs, int colType, int colIndex)
            throws SQLException, IOException {

        String value = "";

        switch (colType) {
            case Types.BIT:
                Object bit = rs.getObject(colIndex);
                if (bit != null) {
                    value = String.valueOf(bit);
                }
                break;
            case Types.BOOLEAN:
                boolean b = rs.getBoolean(colIndex);
                if (!rs.wasNull()) {
                    value = Boolean.valueOf(b).toString();
                }
                break;
            case Types.CLOB:
                Clob c = rs.getClob(colIndex);
                if (c != null) {
                    value = GeneralUtil.readClob(c);
                }
                break;
            case Types.BIGINT:
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
            case Types.NUMERIC:
                BigDecimal bd = rs.getBigDecimal(colIndex);
                if (bd != null) {
                    value = "" + bd.doubleValue();
                }
                break;
            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                int intValue = rs.getInt(colIndex);
                if (!rs.wasNull()) {
                    value = "" + intValue;
                }
                break;
            case Types.JAVA_OBJECT:
                Object obj = rs.getObject(colIndex);
                if (obj != null) {
                    value = String.valueOf(obj);
                }
                break;
            case Types.DATE:
                java.sql.Date date = rs.getDate(colIndex);
                if (date != null) {
                    value = DATE_FORMATTER.format(date);
                }
                break;
            case Types.TIME:
                Time t = rs.getTime(colIndex);
                if (t != null) {
                    value = t.toString();
                }
                break;
            case Types.TIMESTAMP:
                Timestamp tstamp = rs.getTimestamp(colIndex);
                if (tstamp != null) {
                    value = TIMESTAMP_FORMATTER.format(tstamp);
                }
                break;
            case Types.LONGVARCHAR:
            case Types.VARCHAR:
            case Types.CHAR:
                value = rs.getString(colIndex);
                break;
            case Types.BLOB:
                value = BLOB_FORMATTER;
                break;
            default:
                value = "";
        }


        if (value == null) {
            value = "";
        }

        return value;

    }
}
