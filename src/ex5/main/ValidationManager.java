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
    private static final String FINAL_KEYWORD = "final";
    private static final String COMMA_SEPARATOR = ",";
    private static final String EQUALS_OPERATOR = "=";
    private static final String CONDITION_REGEX = "\\((.*?)\\)";
    private static final String LOGICAL_OPERATOR_REGEX = "\\s*(\\|\\||&&)\\s*";
    private static final String WHITESPACE_REGEX = "\\s+";
    private static final String COMMA_WITH_SPACES_REGEX = "\\s*,\\s*";
    private static final String EQUALS_WITH_SPACES_REGEX = "\\s*=\\s*";
    private static final String OR_OPERATOR = "||";
    private static final String AND_OPERATOR = "&&";
    private static final String TRUE_LITERAL = "true";
    private static final String FALSE_LITERAL = "false";

    // Error messages
    private static final String ERR_INVALID_LINE = "Invalid line format";
    private static final String ERR_FINAL_UNINITIALIZED = "Final variable must be initialized: ";
    private static final String ERR_INVALID_ASSIGNMENT = "Invalid assignment format";
    private static final String ERR_BLOCK_OUTSIDE_METHOD = "Block statement outside method at line:";
    private static final String ERR_INVALID_CONDITION = "Invalid block condition format at line:";
    private static final String ERR_MISSING_RETURN = "Missing return statement at method end";
    private static final String ERR_INVALID_OPERATORS = "Logical operators cannot be at start or end of condition";
    private static final String ERR_CONSECUTIVE_OPERATORS = "Cannot have consecutive operators";
    private static final String ERR_LINE_NUMBER_FORMAT = "Line %d: %s";

    private static final Pattern CONDITION_PATTERN = Pattern.compile(CONDITION_REGEX);

    private final LineParser lineParser;
    private final MethodParser methodParser;
    private final VariableParser variableParser;
    private final SyntaxValidator syntaxValidator;
    private final ScopeValidator scopeValidator;
    private final TypeValidator typeValidator;
    private boolean lastLineWasReturn;

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
                case INVALID -> throw new IllegalSjavaFileException(ERR_INVALID_LINE);
            }

            // Update return tracking for method validation
            lastLineWasReturn = lineType == LineType.RETURN_STATEMENT;

        } catch (IllegalSjavaFileException e) {
            throw new IllegalSjavaFileException(String.format(ERR_LINE_NUMBER_FORMAT, lineNumber, e.getMessage()));
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
        boolean isFinal = line.startsWith(FINAL_KEYWORD);
        Types type = Types.getType(line.split(WHITESPACE_REGEX)[isFinal ? 1 : 0]);
        int start = type.toString().length() + (isFinal ? FINAL_KEYWORD.length() + 1 : 0);
        String[] declarations = line.substring(start, line.length() - 1).trim().split(COMMA_WITH_SPACES_REGEX);

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
        String[] parts = declaration.split(EQUALS_WITH_SPACES_REGEX);
        String name = parts[0];
        boolean isInitialized = parts.length > 1;
        String value = isInitialized ? parts[1] : null;

        if (isFinal && !isInitialized) {
            throw new IllegalSjavaFileException(ERR_FINAL_UNINITIALIZED + name);
        }

        if (isInitialized) {
            validateVariableValue(value, type);
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
        line = line.trim();
        String[] assignments = line.substring(0, line.length() - 1).split(COMMA_WITH_SPACES_REGEX);

        for (String assignment : assignments) {
            String[] parts = assignment.split(EQUALS_WITH_SPACES_REGEX);
            if (parts.length != 2) {
                throw new IllegalSjavaFileException(ERR_INVALID_ASSIGNMENT);
            }
            String varName = parts[0];
            String value = parts[1];
            Types varType = scopeValidator.getVariableType(varName);

            validateVariableValue(value, varType);
            scopeValidator.validateAssignment(varName);
        }
    }

    /**
     * Checks if a given variable's value is valid
     * @param value the variable's value
     * @param type the variable's type
     * @throws IllegalSjavaFileException if the value is incompatible with the given variable
     */
    private void validateVariableValue(String value, Types type) throws IllegalSjavaFileException {
        try {
            // Try to validate as identifier first
            variableParser.validateIdentifier(value);
            scopeValidator.validateVariableInitialization(value);
            // If it's a valid identifier, check type compatibility
            typeValidator.validateTypeCompatibility(type, scopeValidator.getVariableType(value));
        } catch (IllegalSjavaFileException e) {
            typeValidator.validateLiteralType(type, value); // Not a valid identifier, try as literal
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
            throw new IllegalSjavaFileException(ERR_BLOCK_OUTSIDE_METHOD + line);
        }

        Matcher matcher = CONDITION_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalSjavaFileException(ERR_INVALID_CONDITION + line);
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
            throw new IllegalSjavaFileException(ERR_MISSING_RETURN);
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
        if (condition.startsWith(OR_OPERATOR) || condition.startsWith(AND_OPERATOR) ||
                condition.endsWith(OR_OPERATOR) || condition.endsWith(AND_OPERATOR)) {
            throw new IllegalSjavaFileException(ERR_INVALID_OPERATORS);
        }

        // Split by || and &&, discarding the operators
        String[] tokens = condition.split(LOGICAL_OPERATOR_REGEX);
        for (String token : tokens) {
            if (token.isEmpty()) {
                throw new IllegalSjavaFileException(ERR_CONSECUTIVE_OPERATORS);
            }
            validateSingleCondition(token);
        }
    }

    private void validateSingleCondition(String condition) throws IllegalSjavaFileException {
        // Check for boolean literals
        if (condition.equals(TRUE_LITERAL) || condition.equals(FALSE_LITERAL)) {
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