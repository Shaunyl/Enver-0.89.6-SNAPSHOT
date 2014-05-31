package it.shaunyl.enver.exception;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class UnknownTaskModeException extends EnverException {

    private static final long serialVersionUID = 1L;

    public UnknownTaskModeException() {
    }

    public UnknownTaskModeException(String message) {
        super(message);
    }

    public UnknownTaskModeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownTaskModeException(Throwable cause) {
        super(cause);
    }
}
