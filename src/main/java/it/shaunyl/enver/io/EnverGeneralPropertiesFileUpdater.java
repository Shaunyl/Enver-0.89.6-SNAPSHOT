/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.shaunyl.enver.io;

import it.shaunyl.enver.exception.EnverException;
import java.io.*;
import java.util.Properties;

/**
 *
 * @author Filippo
 */
public class EnverGeneralPropertiesFileUpdater extends IEnverPropertiesFileUpdater {

    private String file = "config/enver.properties";

    private static final String DEFAULT_COMMENT = " This information property list file contains essential general information for ENVER.\n All the keys should not change because they are referenced in the source code.\n ---";

    @Override
    public void writeSubValueByKey(String key, String subprop, String value) throws IOException, EnverException {
    }

    @Override
    public void writeValueByKey(String key, String value) throws IOException, EnverException {
        FileInputStream in = new FileInputStream(file);
        Properties props = new Properties();
        props.load(in);
        in.close();

        FileOutputStream out = new FileOutputStream(file);
        props.setProperty(key, value);

        props.store(out, DEFAULT_COMMENT);
        out.close();
    }
}
