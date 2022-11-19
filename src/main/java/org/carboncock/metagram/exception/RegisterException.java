package org.carboncock.metagram.exception;

/**
 * @author Pavel Sharaev (mail@pechhenka.ru)
 */
public class RegisterException extends Exception {
    public RegisterException(final String message) {
        super(message);
    }

    public RegisterException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
