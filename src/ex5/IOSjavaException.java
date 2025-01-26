package ex5;

import java.io.IOException;

/** Exception for IO-related errors in s-Java processing */
public class IOSjavaException extends IOException {
    /**
     * IOSjavaException's constructor
     *
     * @param message an error message
     */
    public IOSjavaException(String message) {
        super(message);
    }
}