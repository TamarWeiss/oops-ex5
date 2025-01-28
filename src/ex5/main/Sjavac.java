package ex5.main;

import ex5.IOSjavaException;
import ex5.IllegalSjavaFileException;

/**
 * Main class for the s-Java verifier.
 * Reads and validates s-Java files according to the specified syntax rules.
 */
public class Sjavac {
    // Return codes as per specification
    private static final int SUCCESS = 0;
    private static final int FAILURE = 1;
    private static final int ERROR = 2;

    // Error messages
    private static final String ERROR_WRONG_ARGUMENTS = "Error: Wrong number of arguments";

    /**
     * Processes the input file and returns appropriate status code
     *
     * @param args the program's arguments
     * @return an appropriate status code
     */
    private static int processFile(String[] args) {
        if (args.length != 1) {
            System.err.println(ERROR_WRONG_ARGUMENTS);
            return ERROR;
        }

        try {
            new FileProcessor(args[0]).processFile();
            return SUCCESS;
        } catch (IOSjavaException e) {
            System.err.println(e.getMessage());
            return ERROR;
        } catch (IllegalSjavaFileException e) {
            System.err.println(e.getMessage());
            return FAILURE;
        }
    }

    /**
     * Main method - entry point of the program.
     *
     * @param args the program's arguments
     */
    public static void main(String[] args) {
        System.out.println(processFile(args));
    }
}