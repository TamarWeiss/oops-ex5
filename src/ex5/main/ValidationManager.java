package ex5.main;

import ex5.IllegalSjavaFileException;
import ex5.parser.*;
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
    private boolean lastLineWasReturn;

    // Patterns for specific validations
    private static final Pattern CONDITION_PATTERN = Pattern.compile("\\((.*?)\\)");
    private static final Pattern VARIABLE_ASSIGNMENT_PATTERN = Pattern.compile(
            "^\\s*(\\w+)\\s*=\\s*(.+)\\s*$"
    );

    // Patterns for specific validations
    private static final String LOGICAL_OPERATOR = "\\s*(\\|\\||&&)\\s*";

    /**
     * Constructor for ValidationManager.
     * Initializes all parsers and validators.
     */
    public ValidationManager() {
        this.lineParser = new LineParser();
        this.scopeValidator = new ScopeValidator();
        this.methodParser = new MethodParser(scopeValidator);
        this.variableParser = new VariableParser();
        this.syntaxValidator = new SyntaxValidator();
        this.typeValidator = new TypeValidator();
        this.lastLineWasReturn = false;
    }

    /**
     * Validates a single line of code through the complete validation chain
     *
     * @param line       the aforementioned line of code
     * @param lineNumber the line's number
     * @throws IllegalSjavaFileException if the line isn't formatted correctly
     */
    public void validateLine(String line, int lineNumber) throws IllegalSjavaFileException {
        LineType lineType = lineParser.getLineType(line);
        // Skip empty lines and comments early
        if (lineType == LineType.EMPTY || lineType == LineType.COMMENT) {
            return;
        }

        try {
            // Basic syntax validation first
            syntaxValidator.validateLineSyntax(line);

            // Process based on a line type
            switch (lineType) {
                case METHOD_DECLARATION -> methodParser.validateMethodDeclaration(line);
                case VARIABLE_DECLARATION -> processVariableDeclaration(line);
                case VARIABLE_ASSIGNMENT -> processVariableAssignment(line);
                case BLOCK_START -> processBlockStart(line);
                case BLOCK_END -> processBlockEnd();
                case RETURN_STATEMENT -> methodParser.processReturnStatement(line);
                case METHOD_CALL -> methodParser.validateMethodCall(line);
                case INVALID -> throw new IllegalSjavaFileException("Invalid line format");
            }

            // Update return tracking for method validation
            lastLineWasReturn = lineType == LineType.RETURN_STATEMENT;

        } catch (IllegalSjavaFileException e) {
            throw new IllegalSjavaFileException(String.format("Line %d: %s", lineNumber, e.getMessage()));
        }
    }

    /**
     * Get current method status
     *
     * @return the current method status
     */
    public boolean isInMethod() {
        return scopeValidator.isInMethod();
    }

    /**
     * Reset all validators' state
     * makes false the last line
     */
    public void reset() {
        lastLineWasReturn = false;
        scopeValidator.reset();
    }

    //---------------------------- private method ----------------------------------------

    /**
     * Processes variable declarations
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException for invalid variable declaration
     */
    private void processVariableDeclaration(String line) throws IllegalSjavaFileException {
        variableParser.validateDeclaration(line);
        line = line.trim();

        // Handle multiple variable declarations
        boolean isFinal = line.startsWith("final");
        Types type = Types.getType(line.split("\\s+")[isFinal ? 1 : 0]);
        int start = type.toString().length() + (isFinal ? "final".length() + 1 : 0);
        String[] declarations = line.substring(start, line.length() - 1).trim().split("\\s*,\\s*");

        // Process each declaration
        for (String declaration : declarations) {
            processSingleDeclaration(declaration, type, isFinal);
        }
    }

    /**
     * Processes a single variable declaration
     *
     * @param declaration the variable declaration
     * @param type        the variable's type
     * @param isFinal     if the variable is final
     * @throws IllegalSjavaFileException if the variable declaration is invalid
     */
    private void processSingleDeclaration(String declaration, Types type, boolean isFinal)
    throws IllegalSjavaFileException {
        String[] parts = declaration.split("\\s*=\\s*");
        String name = parts[0];
        boolean isInitialized = parts.length > 1;
        String value = isInitialized ? parts[1] : null;

        if (isFinal && !isInitialized) {
            throw new IllegalSjavaFileException("Final variable must be initialized: " + name);
        }

        if (isInitialized) {
            try {
                // Try to validate as identifier first
                variableParser.validateIdentifier(value);  // use existing value variable
                // If it's a valid identifier, check type compatibility
                Types valueType = scopeValidator.getVariableType(value);
                scopeValidator.validateVariableInitialization(value);
                typeValidator.validateTypeCompatibility(type, valueType);
            } catch (IllegalSjavaFileException e) {
                typeValidator.validateLiteralType(type, value); // Not a valid identifier, try as literal
            }
        }

        scopeValidator.declareVariable(name, type, isFinal, isInitialized);
    }

    /**
     * Processes variable assignments
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the assignment format is invalid
     */
    private void processVariableAssignment(String line) throws IllegalSjavaFileException {
        String[] assignments = line.substring(0, line.length() - 1).split(",");

        for (String assignment : assignments) {
            Matcher matcher = VARIABLE_ASSIGNMENT_PATTERN.matcher(assignment);
            if (!matcher.matches()) {
                throw new IllegalSjavaFileException("Invalid assignment format");
            }

            String varName = matcher.group(1);
            String value = matcher.group(2).trim();
            if (value.endsWith(";")) {
                value = value.substring(0, value.length() - 1).trim();
            }

            Types varType = scopeValidator.getVariableType(varName);

            try {
                // Try to validate as identifier first
                variableParser.validateIdentifier(value);
                // If it's a valid identifier, check type compatibility
                Types valueType = scopeValidator.getVariableType(value);
                scopeValidator.validateVariableInitialization(value);
                typeValidator.validateTypeCompatibility(varType, valueType);
            } catch (IllegalSjavaFileException e) {
                // Not a valid identifier, try as literal
                typeValidator.validateLiteralType(varType, value);
            }

            scopeValidator.validateAssignment(varName);
        }
    }

    /**
     * Processes block starts (if/while)
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the block statement isn't formatted correctly
     */
    private void processBlockStart(String line) throws IllegalSjavaFileException {
        if (!isInMethod()) {
            throw new IllegalSjavaFileException("Block statement outside method" + " at line:" + line);
        }

        Matcher matcher = CONDITION_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalSjavaFileException("Invalid block condition format" + " at line:" + line);
        }

        String condition = matcher.group(1).trim();
        validateCondition(condition);
        scopeValidator.enterScope(false);
    }

    /**
     * Processes block ends
     *
     * @throws IllegalSjavaFileException if the block's end isn't formatted correctly
     */
    private void processBlockEnd() throws IllegalSjavaFileException {
        boolean isMethodEnd = scopeValidator.isMethodEnd();
        if (isMethodEnd && !lastLineWasReturn) {
            throw new IllegalSjavaFileException("Missing return statement at method end");
        }
        scopeValidator.exitScope(isMethodEnd);
    }

    /**
     * Validates conditions in if/while statements
     *
     * @param condition a string representing a condition
     * @throws IllegalSjavaFileException if the condition isn't formatted properly
     */
    private void validateCondition(String condition) throws IllegalSjavaFileException {
        // Check for invalid operator placement at start/end
        if (condition.startsWith("||") || condition.startsWith("&&") ||
            condition.endsWith("||") || condition.endsWith("&&")) {
            throw new IllegalSjavaFileException("Logical operators cannot be at start or end of condition");
        }

        // Split by || and &&, discarding the operators
        String[] tokens = condition.split(LOGICAL_OPERATOR);
        for (String token : tokens) {
            if (token.isEmpty()) {
                throw new IllegalSjavaFileException("Cannot have consecutive operators");
            }
            validateSingleCondition(token);
        }
    }

    private void validateSingleCondition(String condition) throws IllegalSjavaFileException {
        // Check for boolean literals
        if (condition.equals("true") || condition.equals("false")) {
            return;
        }

        // Check for numeric literal
        try {
            Double.parseDouble(condition);
        } catch (NumberFormatException ignored) {
            // Must be a variable - validate it exists and has a compatible type
            Types type = scopeValidator.getVariableType(condition);
            typeValidator.validateConditionType(type);
            scopeValidator.validateVariableInitialization(condition);
        }
    }
}