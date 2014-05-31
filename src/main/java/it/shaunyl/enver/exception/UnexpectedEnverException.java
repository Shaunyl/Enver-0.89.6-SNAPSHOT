package it.shaunyl.enver.exception;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public class UnexpectedEnverException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public UnexpectedEnverException(String message) {
        super(message);
    }

    public UnexpectedEnverException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedEnverException(Throwable cause) {
        super(cause);
    }
}
