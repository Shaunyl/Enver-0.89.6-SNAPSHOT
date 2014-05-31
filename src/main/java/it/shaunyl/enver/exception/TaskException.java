package it.shaunyl.enver.exception;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class TaskException extends Exception {

    private static final long serialVersionUID = 1L;

    public TaskException() {
    }

    public TaskException(String message) {
        super(message);
    }

    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskException(Throwable cause) {
        super(cause);
    }
}
