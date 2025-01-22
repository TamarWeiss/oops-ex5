package ex5.main;

import ex5.IOSjavaException;
import ex5.IllegalSjavaFileException;
import ex5.parser.LineParser;
import ex5.parser.LineParser.LineType;
import ex5.parser.MethodParser;
import ex5.parser.Types;
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
    private boolean lastLineWasReturn;

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
        this.lastLineWasReturn = false;
    }

    /**
     * Processes the s-Java file
     *
     * @throws IOSjavaException          for IO-related errors
     * @throws IllegalSjavaFileException for syntax errors
     */
    public void processFile() throws IOSjavaException, IllegalSjavaFileException {
        try {
            // First pass: collect global structure
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    LineType lineType = lineParser.getLineType(line);

                    if (lineType == LineType.EMPTY || lineType == LineType.COMMENT) {
                        continue;
                    }

                    syntaxValidator.validateLineSyntax(line);

                    switch (lineType) {
                        case METHOD_DECLARATION:
                            methodParser.validateMethodDeclaration(line);
                            scopeValidator.enterScope(true);  // Using existing method
                            inMethod = true;
                            break;

                        case BLOCK_END:
                            if (inMethod) {
                                if (scopeValidator.isMethodEnd()) {
                                    inMethod = false;
                                    scopeValidator.exitScope(true);
                                } else {
                                    scopeValidator.exitScope(false);
                                }
                            }
                            break;

                        case VARIABLE_DECLARATION:
                            if (!inMethod) {
                                variableParser.validateDeclaration(line);
                                processVariableDeclaration(line, true); // true for global
                            }
                            break;

                        case BLOCK_START:
                            if (inMethod) {
                                scopeValidator.enterScope(false);
                            }
                            break;
                    }
                }

                if (inMethod) {
                    throw new IllegalSjavaFileException(
                            "Unclosed method block at end of file", lineNumber
                    );
                }
            }

            // First pass code remains the same...

            // Second pass: validate method internals and variable usage
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                lineNumber = 0;
                inMethod = false;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    LineType lineType = lineParser.getLineType(line);

                    if (lineType == LineType.EMPTY || lineType == LineType.COMMENT) {
                        continue;
                    }

                    switch (lineType) {
                        case METHOD_DECLARATION -> {
                            inMethod = true;
                            scopeValidator.enterScope(true);
                        }
                        case VARIABLE_DECLARATION -> {
                            if (inMethod) {
                                // Validate and process local variable
                                variableParser.validateDeclaration(line);
                                processVariableDeclaration(line, false); // false for local
                            }
                        }
                        case BLOCK_START -> {
                            if (inMethod) {
                                // Check condition (if/while)
                                validateBlockCondition(line);
                                scopeValidator.enterScope(false);
                            }
                        }
                        case BLOCK_END -> {
                            if (inMethod) {
                                if (scopeValidator.isMethodEnd()) {
                                    // Check if last statement was return
                                    if (!lastLineWasReturn) {
                                        throw new IllegalSjavaFileException(
                                                "Method must end with return statement",
                                                lineNumber
                                        );
                                    }
                                    inMethod = false;
                                    scopeValidator.exitScope(true);
                                } else {
                                    scopeValidator.exitScope(false);
                                }
                            }
                        }
                        case RETURN_STATEMENT -> {
                            if (!inMethod) {
                                throw new IllegalSjavaFileException(
                                        "Return statement outside method",
                                        lineNumber
                                );
                            }
                            if (!methodParser.isValidReturnStatement(line)) {
                                throw new IllegalSjavaFileException(
                                        "Invalid return statement",
                                        lineNumber
                                );
                            }
                            lastLineWasReturn = true;
                        }
                    }
                    // If not a return statement, update lastLineWasReturn
                    if (lineType != LineType.RETURN_STATEMENT) {
                        lastLineWasReturn = false;
                    }
                }
            }

        } catch (IOException e) {
            throw new IOSjavaException("Failed to read file: " + e.getMessage());
        }
    }

    private void validateBlockCondition(String line) throws IllegalSjavaFileException {
        // Extract condition between parentheses
        int start = line.indexOf('(');
        int end = line.lastIndexOf(')');

        if (start == -1 || end == -1 || start > end) {
            throw new IllegalSjavaFileException(
                    "Invalid block condition syntax",
                    lineNumber
            );
        }

        String condition = line.substring(start + 1, end).trim();

        // Empty condition
        if (condition.isEmpty()) {
            throw new IllegalSjavaFileException(
                    "Empty condition in if/while block",
                    lineNumber
            );
        }

        // Validate condition - could be a boolean literal, number, or variable
        if (condition.matches("^(true|false)$")) {
            return; // Valid boolean literal
        }

        try {
            // Check if it's a valid number
            Double.parseDouble(condition);
            return;
        } catch (NumberFormatException e) {
            // Not a number, must be a variable
            scopeValidator.validateVariableAccess(condition);
            Types type = scopeValidator.getVariableType(condition);

            // Check if type can be used in condition
            if (type != Types.BOOLEAN && type != Types.INT && type != Types.DOUBLE) {
                throw new IllegalSjavaFileException(
                        "Invalid condition type: " + type,
                        lineNumber
                );
            }
        }
    }
    private void processVariableDeclaration(String line, boolean isGlobal)
            throws IllegalSjavaFileException {
        // Remove trailing semicolon
        line = line.substring(0, line.length() - 1);

        // Split multiple declarations
        String[] declarations = line.split(",");

        for (String declaration : declarations) {
            declaration = declaration.trim();
            boolean isFinal = declaration.startsWith("final");

            if (isFinal) {
                declaration = declaration.substring(5).trim();
            }

            String[] parts = declaration.split("=", 2);
            String[] typeAndName = parts[0].trim().split("\\s+");

            Types type = Types.getType(typeAndName[typeAndName.length - 2]);
            String name = typeAndName[typeAndName.length - 1];
            boolean isInitialized = parts.length > 1;

            // Use existing ScopeValidator method
            scopeValidator.declareVariable(name, type, isFinal, isInitialized);
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
                    processVariableDeclaration(line, false);
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
                            "Return statement outside method at line " + lineNumber,
                            lineNumber);
                }
                break;
            case INVALID:
                throw new IllegalSjavaFileException("Invalid line format at line " + lineNumber, lineNumber);
        }
    }

    /** Processes a block start (if/while) */
    private void processBlockStart(String line) throws IllegalSjavaFileException {
        if (!inMethod) {
            throw new IllegalSjavaFileException("Block statement outside method at line " + lineNumber, lineNumber);
        }

        // Extract condition from between parentheses
        int start = line.indexOf('(');
        int end = line.lastIndexOf(')');
        if (start == -1 || end == -1) {
            throw new IllegalSjavaFileException("Invalid block condition at line " + lineNumber, lineNumber);
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