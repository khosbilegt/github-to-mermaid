package mn.khosbilegt.exception;

public class KnownException extends RuntimeException{
    public KnownException() {
        super();
    }

    public KnownException(String message) {
        super(message);
    }
}
