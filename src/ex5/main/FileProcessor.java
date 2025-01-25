

package ex5.main;

import ex5.IOSjavaException;
import ex5.IllegalSjavaFileException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Handles the processing of s-Java files.
 * This class is responsible for file operations and coordinating the validation process.
 */
public class FileProcessor {
    private static final String SJAVA_EXTENSION = ".sjava";
    private final String filename;
    private final ValidationManager validationManager;
    private int lineNumber;

    /**
     * Constructor for FileProcessor
     *
     * @param filename The name of the file to process
     * @throws IOSjavaException if the file's name is invalid
     */
    public FileProcessor(String filename) throws IOSjavaException {
        validateFileName(filename);
        this.filename = filename;
        this.validationManager = new ValidationManager();
        this.lineNumber = 0;
    }

    /**
     * Processes the s-Java file
     *
     * @throws IOSjavaException          for IO-related errors
     * @throws IllegalSjavaFileException for syntax errors
     */
    public void processFile() throws IOSjavaException, IllegalSjavaFileException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            lineNumber = 0;
            validationManager.reset();  // Reset validation state

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                validationManager.validateLine(line, lineNumber);
            }

            // Check the final state
            if (validationManager.isInMethod()) {
                throw new IllegalSjavaFileException("Unclosed method block at end of file", lineNumber);
            }

        } catch (IOException e) {
            throw new IOSjavaException("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Validates the filename has the correct extension
     *
     * @param filename the file's name
     * @throws IOSjavaException if the file's name is invalid
     */
    private void validateFileName(String filename) throws IOSjavaException {
        if (filename == null || !filename.endsWith(SJAVA_EXTENSION)) {
            throw new IOSjavaException("File must end with " + SJAVA_EXTENSION);
        }
    }
}