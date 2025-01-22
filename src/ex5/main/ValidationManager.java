package ex5.main;

import ex5.IllegalSjavaFileException;
import ex5.parser.LineParser;
import ex5.parser.LineParser.LineType;
import ex5.validators.ScopeValidator;
import ex5.validators.SyntaxValidator;
import ex5.validators.TypeValidator;

/**
 * Manages the validation chain for s-Java code verification.
 * This class coordinates the different validators and ensures proper order of validation.
 */
public class ValidationManager {
    private final LineParser lineParser;
    private final SyntaxValidator syntaxValidator;
    private final ScopeValidator scopeValidator;
    private final TypeValidator typeValidator;
    private int currentLine;

    public ValidationManager() {
        this.lineParser = new LineParser();
        this.syntaxValidator = new SyntaxValidator();
        this.scopeValidator = new ScopeValidator();
        this.typeValidator = new TypeValidator();
        this.currentLine = 0;
    }

    /**
     * Validates a single line of code through the complete validation chain
     *
     * @param line The line to validate
     * @param lineNumber The current line number in the file
     * @throws IllegalSjavaFileException if any validation fails
     */
    public void validateLine(String line, int lineNumber) throws IllegalSjavaFileException {
        this.currentLine = lineNumber;

        // Skip empty lines and comments early
        LineType lineType = lineParser.getLineType(line);
        if (lineType == LineType.EMPTY || lineType == LineType.COMMENT) {
            return;
        }

        try {
            // Basic syntax validation first
            syntaxValidator.validateLineSyntax(line);

            // Process based on line type
            switch (lineType) {
                case METHOD_DECLARATION -> processMethodDeclaration(line);
                case VARIABLE_DECLARATION -> processVariableDeclaration(line);
                case BLOCK_START -> processBlockStart(line);
                case BLOCK_END -> processBlockEnd();
                case RETURN_STATEMENT -> processReturnStatement();
                case INVALID -> throw new IllegalSjavaFileException(
                        "Invalid line format", currentLine
                );
            }
        } catch (IllegalSjavaFileException e) {
            // Enhance error message with line number if not already present
            throw new IllegalSjavaFileException(
                    String.format("Line %d: %s", currentLine, e.getMessage()),
                    currentLine
            );
        }
    }

    private void processMethodDeclaration(String line) throws IllegalSjavaFileException {
        // Method-specific validation
        scopeValidator.enterScope(true);
        // Additional method validation logic...
    }

    private void processVariableDeclaration(String line) throws IllegalSjavaFileException {
        // Variable-specific validation
        // This includes type compatibility, scope rules, etc.
    }

    private void processBlockStart(String line) throws IllegalSjavaFileException {
        scopeValidator.enterScope(false);
        // Validate block conditions (if/while)
    }

    private void processBlockEnd() throws IllegalSjavaFileException {
        if (scopeValidator.isMethodEnd()) {
            // Handle method end
            scopeValidator.exitScope(true);
        } else {
            // Handle block end
            scopeValidator.exitScope(false);
        }
    }

    private void processReturnStatement() throws IllegalSjavaFileException {
        // Validate return statement location and syntax
    }

    /**
     * Get current state of scope validation
     * @return true if currently inside a method
     */
    public boolean isInMethod() {
        return scopeValidator.isInMethod();
    }

    /**
     * Reset the validation state
     * This should be called before processing a new file
     */
    public void reset() {
        currentLine = 0;
        // Reset all validators' state
    }
}