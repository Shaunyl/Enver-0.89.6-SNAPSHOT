/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.shaunyl.enver.io;

import it.shaunyl.enver.exception.EnverException;
import java.io.*;
import java.util.Properties;
import java.util.regex.*;
import lombok.NonNull;

/**
 *
 * @author Filippo
 */
public class EnverConnectionStringsPropertiesFileUpdater extends IEnverPropertiesFileUpdater {

    private String file = "config/connectionStrings.properties";

    private static final String DEFAULT_COMMENT = " This information property list file contains connection strings information for ENVER.\n All the keys should not change because they are referenced in the source code.\n ---";

    private static final String REGEX = "jdbc:oracle:thin:(.*?)/(.*?)@(.*?):(\\d+):(.*?)$";

    @Override
    public void writeSubValueByKey(@NonNull final String key, @NonNull final String subprop, @NonNull final String value) throws IOException, EnverException {
        FileInputStream in = new FileInputStream(file);
        Properties props = new Properties();
        props.load(in);
        String prop = props.getProperty(key);
        in.close();
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(prop);
        boolean valid = m.find();
        if (!valid) {
            throw new EnverException("Some stuff in this property list file may be corrupted. No changes were commited.");
        }

        String user = "user".equals(subprop) ? value : m.group(1);
        String password = "password".equals(subprop) ? value : m.group(2);
        String host = "host".equals(subprop) ? value : m.group(3);
        String port = "port".equals(subprop) ? value : m.group(4);
        String schema = "schema".equals(subprop) ? value : m.group(5);
        FileOutputStream out = new FileOutputStream(file);
        props.setProperty(key, String.format("jdbc:oracle:thin:%s/%s@%s:%d:%s", user, password, host, Integer.parseInt(port), schema));

        props.store(out, DEFAULT_COMMENT);
        out.close();
    }

    @Override
    public void writeValueByKey(String key, String value) throws IOException, EnverException {
    }
}
