package ex5.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Handles the processing and validation of s-Java files
 */
public class FileProcessor {
    private static final String SJAVA_EXTENSION = ".sjava";
    private final String filename;
    private int lineNumber;

    /**
     * Constructor for FileProcessor
     * @param filename The name of the file to process
     */
    public FileProcessor(String filename) throws IOSjavaException {
        validateFileName(filename);
        this.filename = filename;
    }

    /**
     * Processes the s-Java file
     * @throws IOSjavaException for IO-related errors
     * @throws IllegalSjavaFileException for syntax errors
     */
    public void processFile() throws IOSjavaException, IllegalSjavaFileException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                processLine(line);
            }

        } catch (IOException e) {
            throw new IOSjavaException("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Processes a single line from the file
     * @param line The line to process
     * @throws IllegalSjavaFileException if the line contains syntax errors
     */
    private void processLine(String line) throws IllegalSjavaFileException {
        // Skip empty lines
        if (line.trim().isEmpty()) {
            return;
        }

        // Skip comment lines
        if (line.trim().startsWith("//")) {
            return;
        }

        // Other line validations will be added here
        // For now, just check basic line endings
        validateLineEnding(line);
    }

    /**
     * Validates the filename has the correct extension
     */
    private void validateFileName(String filename) throws IOSjavaException {
        if (filename == null || !filename.endsWith(SJAVA_EXTENSION)) {
            throw new IOSjavaException("File must end with " + SJAVA_EXTENSION);
        }
    }

    /**
     * Validates line endings according to s-Java specifications
     */
    private void validateLineEnding(String line) throws IllegalSjavaFileException {
        line = line.trim();
        if (line.equals("}")) {
            return; // Valid closing brace line
        }

        if (!line.endsWith(";") && !line.endsWith("{") && !line.endsWith("}")) {
            throw new IllegalSjavaFileException("Line " + lineNumber + ": Invalid line ending");
        }

        if (line.endsWith("}") && !line.equals("}")) {
            throw new IllegalSjavaFileException("Line " + lineNumber + ": Closing brace must be on its own line");
        }
    }
}