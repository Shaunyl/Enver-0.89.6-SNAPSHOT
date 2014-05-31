package it.shaunyl.enver.exception;

/**
 * Base class for all Enver exceptions.
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class EnverException extends Exception {

    private static final long serialVersionUID = 1L;

    public EnverException() {
    }

    public EnverException(String message) {
        super(message);
    }

    public EnverException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnverException(Throwable cause) {
        super(cause);
    }
}
