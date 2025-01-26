package ex5;

/** Base exception for s-Java syntax and validation errors */
public class IllegalSjavaFileException extends Exception {
    /**
     * IOSjavaException's constructor
     *
     * @param message an error message
     */
    public IllegalSjavaFileException(String message) {
        super(message);
    }
}