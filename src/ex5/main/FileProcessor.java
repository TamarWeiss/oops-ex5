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
    private static final String ERROR_INVALID_FILENAME = "File must end with " + SJAVA_EXTENSION;
    private static final String ERROR_READING_FILE = "Failed to read file: ";
    private static final String ERROR_UNCLOSED_METHOD_BLOCK = "Unclosed method block at end of file";
    private final String filename;
    private final ValidationManager validationManager = new ValidationManager();

    /**
     * Constructor for FileProcessor
     *
     * @param filename The name of the file to process
     * @throws IOSjavaException if the file's name is invalid
     */
    public FileProcessor(String filename) throws IOSjavaException {
        validateFileName(filename);
        this.filename = filename;
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
            int lineNumber = 0;
            validationManager.reset();  // Reset validation state

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                validationManager.validateLine(line, lineNumber);
            }

            // Check the final state
            if (validationManager.isInMethod()) {
                throw new IllegalSjavaFileException(ERROR_UNCLOSED_METHOD_BLOCK);
            }

        } catch (IOException e) {
            throw new IOSjavaException(ERROR_READING_FILE + e.getMessage());
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
            throw new IOSjavaException(ERROR_INVALID_FILENAME);
        }
    }
}