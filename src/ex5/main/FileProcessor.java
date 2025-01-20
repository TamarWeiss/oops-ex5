package ex5.main;

import ex5.IOSjavaException;
import ex5.IllegalSjavaFileException;
import ex5.parser.LineParser;
import ex5.parser.LineParser.LineType;
import ex5.parser.MethodParser;
import ex5.parser.VariableParser;
import ex5.validators.ScopeValidator;
import ex5.validators.SyntaxValidator;
import ex5.validators.TypeValidator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/** Handles the processing and validation of s-Java files */
public class FileProcessor {
    private static final String SJAVA_EXTENSION = ".sjava";
    private final String filename;
    private int lineNumber;
    private final LineParser lineParser;
    private final VariableParser variableParser;
    private final MethodParser methodParser;
    private final ScopeValidator scopeValidator;
    private final TypeValidator typeValidator;
    private final SyntaxValidator syntaxValidator;
    private boolean inMethod;

    /**
     * Constructor for FileProcessor
     *
     * @param filename The name of the file to process
     */
    public FileProcessor(String filename) throws IOSjavaException {
        validateFileName(filename);
        this.filename = filename;
        this.lineParser = new LineParser();
        this.variableParser = new VariableParser();
        this.methodParser = new MethodParser();
        this.scopeValidator = new ScopeValidator();
        this.typeValidator = new TypeValidator();
        this.syntaxValidator = new SyntaxValidator();
        this.inMethod = false;
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

            // First pass: collect all method names and global variables
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                LineType lineType = lineParser.getLineType(line);

                if (lineType == LineType.EMPTY || lineType == LineType.COMMENT) {
                    continue;
                }

                switch (lineType) {
                    case METHOD_DECLARATION:
                        methodParser.validateMethodDeclaration(line);
                        inMethod = true;
                        break;
                    case BLOCK_END:
                        inMethod = false;
                        break;
                    case VARIABLE_DECLARATION:
                        if (!inMethod) {
                            throw new IllegalSjavaFileException(
                                    "Invalid statement in global scope at line " + lineNumber
                            );
                        }
                        processVariableDeclaration(line);
                        break;
                }
            }

            // Reset for second pass
            reader.close();
            BufferedReader secondReader = new BufferedReader(new FileReader(filename));
            lineNumber = 0;
            inMethod = false;

            // Second pass: validate method bodies and variable usage
            boolean lastLineWasReturn = false;
            while ((line = secondReader.readLine()) != null) {
                LineType lineType = lineParser.getLineType(line);

                if (lineType == LineType.BLOCK_END && inMethod && scopeValidator.isMethodEnd()) {
                    if (!lastLineWasReturn) {
                        throw new IllegalSjavaFileException(
                                "Method must end with return statement, line " + lineNumber
                        );
                    }
                }
                lastLineWasReturn = lineType == LineType.RETURN_STATEMENT;

                lineNumber++;
                processLine(line);
            }

            // Verify all methods end with a return statement
            if (inMethod) {
                throw new IllegalSjavaFileException("Unclosed method at end of file");
            }

        } catch (IOException e) {
            throw new IOSjavaException("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Processes a single line from the file
     *
     * @param line The line to process
     * @throws IllegalSjavaFileException if the line contains syntax errors
     */
    private void processLine(String line) throws IllegalSjavaFileException {
        LineType lineType = lineParser.getLineType(line);

        // Skip empty lines and comments
        if (lineType == LineType.EMPTY || lineType == LineType.COMMENT) {
            return;
        }

        // Validate general syntax
        syntaxValidator.validateLineSyntax(line);
        lineParser.validateLineEnding(line, lineType);

        // Process line based on its type
        switch (lineType) {
            case METHOD_DECLARATION:
                scopeValidator.enterScope(true);
                inMethod = true;
                break;

            case VARIABLE_DECLARATION:
                if (inMethod) {
                    processVariableDeclaration(line);
                }
                break;

            case BLOCK_START:
                processBlockStart(line);
                scopeValidator.enterScope(false);
                break;

            case BLOCK_END:
                if (inMethod && scopeValidator.isMethodEnd()) {
                    inMethod = false;
                    scopeValidator.exitScope(true);
                }
                else {
                    scopeValidator.exitScope(false);
                }
                break;

            case RETURN_STATEMENT:
                if (!inMethod) {
                    throw new IllegalSjavaFileException(
                            "Return statement outside method at line " + lineNumber);
                }
                break;

            case INVALID:
                throw new IllegalSjavaFileException("Invalid line format at line " + lineNumber);
        }
    }

    /** Processes a variable declaration line */
    private void processVariableDeclaration(String line) throws IllegalSjavaFileException {
        // Remove any trailing semicolon and split multiple declarations
        String[] declarations = line.substring(0, line.length() - 1).split(",");

        for (String declaration : declarations) {
            declaration = declaration.trim();
            String[] parts = declaration.split("=", 2);

            // The First part contains type and name
            String[] typeAndName = parts[0].trim().split("\\s+");
            boolean isFinal = typeAndName[0].equals("final");
            int typeIndex = isFinal ? 1 : 0;

            String type = typeAndName[typeIndex];
            String name = typeAndName[typeIndex + 1];
            boolean isInitialized = parts.length > 1;

            if (isInitialized) {
                String value = parts[1].trim();
                typeValidator.validateLiteralType(type, value);
            }

            scopeValidator.declareVariable(name, type, isFinal, isInitialized);
        }
    }

    /** Processes a block start (if/while) */
    private void processBlockStart(String line) throws IllegalSjavaFileException {
        if (!inMethod) {
            throw new IllegalSjavaFileException(
                    "Block statement outside method at line " + lineNumber);
        }

        // Extract condition from between parentheses
        int start = line.indexOf('(');
        int end = line.lastIndexOf(')');
        if (start == -1 || end == -1) {
            throw new IllegalSjavaFileException(
                    "Invalid block condition at line " + lineNumber);
        }

        String condition = line.substring(start + 1, end).trim();
        typeValidator.validateConditionType(scopeValidator.getVariableType(condition));
    }

    /** Validates the filename has the correct extension */
    private void validateFileName(String filename) throws IOSjavaException {
        if (filename == null || !filename.endsWith(SJAVA_EXTENSION)) {
            throw new IOSjavaException("File must end with " + SJAVA_EXTENSION);
        }
    }
}