package ex5.exceptions;

/** Base exception for s-Java syntax and validation errors */
public class IllegalSjavaFileException extends Exception {
    public IllegalSjavaFileException(String message) {
        super(message);
    }
}