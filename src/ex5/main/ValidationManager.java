package ex5.main;

import ex5.IllegalSjavaFileException;
import ex5.parser.*;
import ex5.parser.LineParser.LineType;
import ex5.validators.ScopeValidator;
import ex5.validators.SyntaxValidator;
import ex5.validators.TypeValidator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the validation chain for s-Java code verification.
 * Coordinates different validators and ensures proper validation order.
 */
public class ValidationManager {
    private final LineParser lineParser;
    private final MethodParser methodParser;
    private final VariableParser variableParser;
    private final SyntaxValidator syntaxValidator;
    private final ScopeValidator scopeValidator;
    private final TypeValidator typeValidator;
    private int currentLine;
    private boolean lastLineWasReturn;

    // Patterns for specific validations
    private static final Pattern CONDITION_PATTERN = Pattern.compile("\\((.*?)\\)");
    private static final Pattern VARIABLE_ASSIGNMENT_PATTERN =
            Pattern.compile("^\\s*(\\w+)\\s*=\\s*(.+)\\s*$");

    public ValidationManager() {
        this.lineParser = new LineParser();
        this.methodParser = new MethodParser();
        this.variableParser = new VariableParser();
        this.syntaxValidator = new SyntaxValidator();
        this.scopeValidator = new ScopeValidator();
        this.typeValidator = new TypeValidator();
        this.currentLine = 0;
        this.lastLineWasReturn = false;
    }

    /**
     * Validates a single line of code through the complete validation chain
     */
    public void validateLine(String line, int lineNumber) throws IllegalSjavaFileException {
        this.currentLine = lineNumber;
        scopeValidator.setCurrentLine(lineNumber);

        // Skip empty lines and comments early
        LineType lineType = lineParser.getLineType(line);
        if (lineType == LineType.EMPTY || lineType == LineType.COMMENT) {
            return;
        }

        try {
            // Basic syntax validation first
            syntaxValidator.validateLineSyntax(line);
            lineParser.validateLineEnding(line, lineType);

            // Process based on line type
            switch (lineType) {
                case METHOD_DECLARATION -> processMethodDeclaration(line);
                case VARIABLE_DECLARATION -> processVariableDeclaration(line);
                case VARIABLE_ASSIGNMENT -> processVariableAssignment(line);
                case BLOCK_START -> processBlockStart(line);
                case BLOCK_END -> processBlockEnd();
                case RETURN_STATEMENT -> processReturnStatement(line);
                case METHOD_CALL -> processMethodCall(line);
                case INVALID -> throw new IllegalSjavaFileException(
                        "Invalid line format", currentLine
                );
            }

            // Update return tracking for method validation
            lastLineWasReturn = lineType == LineType.RETURN_STATEMENT;

        } catch (IllegalSjavaFileException e) {
            throw new IllegalSjavaFileException(
                    String.format("Line %d: %s", currentLine, e.getMessage()),
                    currentLine
            );
        }
    }

    /**
     * Processes method declarations
     */
    private void processMethodDeclaration(String line) throws IllegalSjavaFileException {
        if (!scopeValidator.isInMethod()) {
            methodParser.validateMethodDeclaration(line);
            scopeValidator.enterScope(true);

            // Process method parameters
            String[] params = methodParser.extractParameters(line);
            for (String param : params) {
                String[] paramParts = param.trim().split("\\s+");
                boolean isFinal = paramParts[0].equals("final");
                int typeIndex = isFinal ? 1 : 0;

                Types type = Types.getType(paramParts[typeIndex]);
                String name = paramParts[typeIndex + 1];

                scopeValidator.declareParameter(name, type, isFinal);
            }
        } else {
            throw new IllegalSjavaFileException(
                    "Nested method declarations are not allowed", currentLine);
        }
    }

    /**
     * Processes variable declarations
     */
    private void processVariableDeclaration(String line) throws IllegalSjavaFileException {
        variableParser.validateDeclaration(line);

        // Handle multiple variable declarations
        String[] declarations = line.substring(0, line.length() - 1).split(",");
        boolean isFinal = line.trim().startsWith("final");

        for (String declaration : declarations) {
            String[] parts = declaration.trim().split("=");
            String[] typeAndName = parts[0].trim().split("\\s+");

            // Handle final modifier in type extraction
            int typeIndex = isFinal ? 1 : 0;
            Types type = Types.getType(typeAndName[typeIndex]);
            String name = typeAndName[typeAndName.length - 1];
            boolean isInitialized = parts.length > 1;

            // If initialized, validate the value
            if (isInitialized) {
                String value = parts[1].trim();
                typeValidator.validateLiteralType(type, value);
            }

            scopeValidator.declareVariable(name, type, isFinal, isInitialized);
        }
    }

    /**
     * Processes variable assignments
     */
    private void processVariableAssignment(String line) throws IllegalSjavaFileException {
        String[] assignments = line.substring(0, line.length() - 1).split(",");

        for (String assignment : assignments) {
            Matcher matcher = VARIABLE_ASSIGNMENT_PATTERN.matcher(assignment);
            if (!matcher.matches()) {
                throw new IllegalSjavaFileException(
                        "Invalid assignment format", currentLine);
            }

            String varName = matcher.group(1);
            String value = matcher.group(2);

            Types varType = scopeValidator.getVariableType(varName);

            // Validate value type compatibility
            if (value.matches("\\w+")) {
                // Assignment from another variable
                Types valueType = scopeValidator.getVariableType(value);
                typeValidator.validateTypeCompatibility(varType, valueType);
            } else {
                // Assignment from literal
                typeValidator.validateLiteralType(varType, value);
            }

            scopeValidator.validateAssignment(varName);
        }
    }

    /**
     * Processes block starts (if/while)
     */
    private void processBlockStart(String line) throws IllegalSjavaFileException {
        if (!scopeValidator.isInMethod()) {
            throw new IllegalSjavaFileException(
                    "Block statement outside method", currentLine);
        }

        Matcher matcher = CONDITION_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalSjavaFileException(
                    "Invalid block condition format", currentLine);
        }

        String condition = matcher.group(1).trim();
        validateCondition(condition);

        scopeValidator.enterScope(false);
    }

    /**
     * Processes block ends
     */
    private void processBlockEnd() throws IllegalSjavaFileException {
        if (scopeValidator.isMethodEnd()) {
            if (!lastLineWasReturn) {
                throw new IllegalSjavaFileException(
                        "Missing return statement at method end", currentLine);
            }
            scopeValidator.exitScope(true);
        } else {
            scopeValidator.exitScope(false);
        }
    }

    /**
     * Processes return statements
     */
    private void processReturnStatement(String line) throws IllegalSjavaFileException {
        if (!scopeValidator.isInMethod()) {
            throw new IllegalSjavaFileException(
                    "Return statement outside method", currentLine);
        }

        if (!methodParser.isValidReturnStatement(line)) {
            throw new IllegalSjavaFileException(
                    "Invalid return statement format", currentLine);
        }
    }

    /**
     * Processes method calls
     */
    private void processMethodCall(String line) throws IllegalSjavaFileException {
        if (!scopeValidator.isInMethod()) {
            throw new IllegalSjavaFileException(
                    "Method call outside method body", currentLine);
        }

        methodParser.validateMethodCall(line);
    }

    /**
     * Validates conditions in if/while statements
     */
    private void validateCondition(String condition) throws IllegalSjavaFileException {
        // Handle boolean literals
        if (condition.equals("true") || condition.equals("false")) {
            return;
        }

        // Handle numeric literals
        try {
            Double.parseDouble(condition);
            return;
        } catch (NumberFormatException ignored) {
            // Not a number, continue to variable check
        }

        // Must be a variable - validate it exists and has compatible type
        Types type = scopeValidator.getVariableType(condition);
        typeValidator.validateConditionType(type);
    }

    /**
     * Get current method status
     */
    public boolean isInMethod() {
        return scopeValidator.isInMethod();
    }

    /**
     * Reset all validators' state
     */
    public void reset() {
        currentLine = 0;
        lastLineWasReturn = false;
        scopeValidator.reset();
    }

}