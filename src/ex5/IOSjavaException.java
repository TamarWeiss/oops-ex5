package ex5;

/** Exception for IO-related errors in s-Java processing */
public class IOSjavaException extends Exception {
    /**
     * IOSjavaException's constructor
     *
     * @param message an error message
     */
    public IOSjavaException(String message) {
        super(message);
    }
}