package it.shaunyl.enver.database;

import it.shaunyl.enver.exception.UnexpectedEnverException;
import oracle.jdbc.pool.OracleDataSource;
import javax.sql.*;
import java.sql.*;
import java.io.*;
import java.util.Properties;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class ConnectionFactory {

    public enum DataProvider {

        ORACLE,
        MYSQL

    }

    public static DataProvider[] getDataProviders() {
        return DataProvider.values();
    }

    public static DataSource getDataSource(DataProvider dp, String url) throws SQLException {
        DataSource dataSource = null;
        switch (dp) {
            case ORACLE: {
                dataSource = new OracleDataSource();
                break;
            }
        }

        loadProperties(dataSource, url);
        return dataSource;

    }

    private static DataSource loadProperties(DataSource datasource, String url) {
        Properties prop = new Properties();

        try {
            prop.load(new FileInputStream("config/connectionStrings.properties"));
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage());
        }

        if (datasource.getClass().equals(OracleDataSource.class)) {
            ((OracleDataSource) datasource).setURL(prop.getProperty(url));
        }

        return datasource;
    }

    public static boolean containsProvider(String test) {
        for (DataProvider dp : DataProvider.values()) {
            if (dp.name().equals(test)) {
                return true;
            }
        }

        return false;
    }

    public static String getStringDataProviders() {
        String providers = "";
        for (DataProvider dp : getDataProviders()) {
            providers += "  " + dp.name();
        }

        return providers;
    }

    public static void close(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
        }
    }

    public static void close(ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
        }
    }

    public static void cancel(Statement... statements) {
        try {
            for (Statement statement : statements) {
                if (statement != null) {
                    statement.cancel();
                }
            }
        } catch (SQLException e) {
        }
    }

    public static void close(Statement... statements) {
        try {
            for (Statement statement : statements) {
                if (statement != null) {
                    statement.close();
                }
            }
        } catch (SQLException e) {
        }
    }

    public static void rollback(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
        }
    }

    public static void commit(Connection connection) {
        try {
            if (connection != null) {
                connection.commit();
            }
        } catch (SQLException e) {
        }
    }

    public static void printException(Exception e) {
        System.out.println("Exception caught! Exiting ..");
        System.out.println("error message: " + e.getMessage());
    }
}
