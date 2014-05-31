package it.shaunyl.enver.util;

import it.shaunyl.enver.exception.UnexpectedEnverException;
import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class EnverUtil {

    public static String getBuildVersion() {
        String buildVersion = "UNKNOWN";
        Properties buildInfo = new Properties();
        ClassLoader classLoader = EnverUtil.class.getClassLoader();

        URL buildInfoFile = classLoader.getResource("buildinfo.properties");
        InputStream in = null;
        try {
            if (buildInfoFile != null) {
                in = buildInfoFile.openStream();
                buildInfo.load(in);
                String o = (String) buildInfo.get("build.version");

                if (o != null) {
                    buildVersion = o;
                }
            }
        } catch (IOException e) { // This is not a fatal exception.
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new UnexpectedEnverException("Failed to close InputStream in EnverUtil.", e);
                }
            }
        }

        return buildVersion;
    }

    public static String getBuildTimestamp() {
        String buildVersion = "UNKNOWN";
        Properties buildInfo = new Properties();
        ClassLoader classLoader = EnverUtil.class.getClassLoader();

        URL buildInfoFile = classLoader.getResource("buildinfo.properties");
        InputStream in = null;
        try {
            if (buildInfoFile != null) {
                in = buildInfoFile.openStream();
                buildInfo.load(in);
                String o = (String) buildInfo.get("build.timestamp");

                if (o != null) {
                    buildVersion = o;
                }
            }
        } catch (IOException e) { // This is not a fatal exception.
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new UnexpectedEnverException("Failed to close InputStream in EnverUtil.", e);
                }
            }
        }

        return buildVersion;
    }
}
