package it.shaunyl.enver.util;

import it.shaunyl.enver.exception.UnexpectedEnverException;
import java.io.*;
import java.sql.*;
import java.util.Scanner;
import lombok.Cleanup;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class GeneralUtil {

    public static String readFile(String path) {
        String content = new Scanner(
                GeneralUtil.class.getResourceAsStream(path), "UTF-8").next();
        return content;
    }

    public static int utf8StringLength(final CharSequence sequence) {
        int count = 0;
        for (int i = 0, len = sequence.length(); i < len; i++) {
            char ch = sequence.charAt(i);
            if (ch <= 0x7F) {
                count++;
            } else if (ch <= 0x7FF) {
                count += 2;
            } else if (Character.isHighSurrogate(ch)) {
                count += 4;
                ++i;
            } else {
                count += 3;
            }
        }
        return count;
    }

    public static String readClob(final Clob c) throws SQLException, IOException {
        StringBuilder sb = new StringBuilder((int) c.length());
        Reader r = c.getCharacterStream();
        char[] cbuf = new char[2048];
        int n = 0;
        while ((n = r.read(cbuf, 0, cbuf.length)) != -1) {
            if (n > 0) {
                sb.append(cbuf, 0, n);
            }
        }
        return sb.toString();
    }

    public static void fileToClobField(final String file, final java.sql.Clob clob) throws SQLException {
        try {
            BufferedReader br;
            @Cleanup
            Writer os = clob.setCharacterStream(0L);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
                line = br.readLine();
            }
            os.write(sb.toString());
            br.close();
        } catch (UnsupportedEncodingException e) {
            throw new UnexpectedEnverException(e.getMessage());
        } catch (FileNotFoundException e) {
            throw new UnexpectedEnverException(e.getMessage());
        } catch (IOException e) {
            throw new UnexpectedEnverException(e.getMessage());
        }
    }

    public static String repeat(String str, int repeat) {
        String repeated = "";
        for (int i = 0; i < repeat; i++) {
            repeated += repeated;
        }
        return repeated;
    }
}
