package it.shaunyl.enver.task.reader;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 * @version 1.2
 */
public interface IEnverReader {

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     *
     * @return a List of String[], with each String[] representing a line of the
     * file.
     *
     * @throws IOException if bad things happen during the read.
     */
    public List readAll() throws IOException;

    /**
     * Gets a list of supported extensions.
     *
     */
    public String[] getValidFileExtensions();
}
