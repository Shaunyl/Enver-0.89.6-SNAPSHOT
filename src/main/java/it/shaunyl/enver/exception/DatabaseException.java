package it.shaunyl.enver.exception;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class DatabaseException extends EnverException {

    private static final long serialVersionUID = 1L;
    
    public DatabaseException() {
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(Throwable cause) {
        super(cause);
    }
}
