package ex5;

/** Base exception for s-Java syntax and validation errors */
public class IllegalSjavaFileException extends Exception {
    /**
     * IOSjavaException's constructor
     *
     * @param message    an error message
     * @param lineNumber the line in which the error occurred
     */
    public IllegalSjavaFileException(String message, int lineNumber) {
        super(message);
    }
}