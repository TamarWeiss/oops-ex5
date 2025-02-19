package ex5.main;

import ex5.IllegalSjavaFileException;
import ex5.parser.*;
import ex5.validators.ScopeValidator;
import ex5.validators.SyntaxValidator;
import ex5.validators.TypeValidator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ex5.Constants.*;

/**
 * Manages the validation chain for s-Java code verification.
 * Coordinates different validators and ensures proper validation order.
 */
public class ValidationManager {
    private static final String CONDITION_REGEX = "\\((.*?)\\)";
    private static final String LOGICAL_OPERATOR_REGEX = "\\s*(\\|\\||&&)\\s*";
    private static final String OR = "||";
    private static final String AND = "&&";
    private static final Pattern CONDITION_PATTERN = Pattern.compile(CONDITION_REGEX);

    // Error messages
    private static final String ERR_INVALID_LINE = "Invalid line format";
    private static final String ERR_UNINITIALIZED_FINAL = "Final variable must be initialized: ";
    private static final String ERR_INVALID_ASSIGNMENT = "Invalid assignment format";
    private static final String ERR_BLOCK_OUTSIDE_METHOD = "Block statement outside method at line:";
    private static final String ERR_INVALID_CONDITION = "Invalid block condition format at line:";
    private static final String ERR_MISSING_RETURN = "Missing return statement at method end";
    private static final String ERR_INVALID_OPERATORS =
            "Logical operators cannot be at start or end of condition";
    private static final String ERR_CONSECUTIVE_OPERATORS = "Cannot have consecutive operators";
    private static final String ERR_LINE_NUMBER_FORMAT = "Line %d: %s";

    private final LineParser lineParser = new LineParser();
    private final ScopeValidator scopeValidator = new ScopeValidator();
    private final MethodParser methodParser = new MethodParser(
            this.scopeValidator, this::validateVariableValue
    );
    private final VariableParser variableParser = new VariableParser();
    private final SyntaxValidator syntaxValidator = new SyntaxValidator();
    private final TypeValidator typeValidator = new TypeValidator();
    private boolean lastLineWasReturn = false;

    /**
     * Declares methods in the code
     *
     * @param line       the line of code
     * @param lineNumber the line's number
     */
    public void declareMethods(String line, int lineNumber) {
        try {
            if (lineParser.getLineType(line) == LineType.METHOD_DECLARATION) {
                methodParser.declareMethod(line);
            }
        } catch (IllegalSjavaFileException e) {
            throw new IllegalSjavaFileException(
                    String.format(ERR_LINE_NUMBER_FORMAT, lineNumber, e.getMessage())
            );
        }
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
        // Skip empty lines early
        if (lineType == LineType.EMPTY) {
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
                case RETURN_STATEMENT -> methodParser.validatesReturnStatement(line);
                case METHOD_CALL -> methodParser.validateMethodCall(line);
                case INVALID -> throw new IllegalSjavaFileException(ERR_INVALID_LINE);
            }

            // Update return tracking for method validation
            lastLineWasReturn = lineType == LineType.RETURN_STATEMENT;

        } catch (IllegalSjavaFileException e) {
            throw new IllegalSjavaFileException(
                    String.format(ERR_LINE_NUMBER_FORMAT, lineNumber, e.getMessage())
            );
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
        boolean isFinal = line.startsWith(FINAL);
        Types type = Types.getType(line.split(WHITESPACE)[isFinal ? 1 : 0]);
        int start = type.toString().length() + (isFinal ? FINAL.length() + 1 : 0);
        String[] declarations = line.substring(start, line.length() - 1).trim().split(COMMA);

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
        String[] parts = declaration.split(EQUALS);
        String name = parts[0];
        boolean isInitialized = parts.length > 1;

        if (isFinal && !isInitialized) {
            throw new IllegalSjavaFileException(ERR_UNINITIALIZED_FINAL + name);
        }

        if (isInitialized) {
            validateVariableValue(parts[1], type);
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
        String[] assignments = line.substring(0, line.length() - 1).split(COMMA);

        for (String assignment : assignments) {
            String[] parts = assignment.split(EQUALS);
            if (parts.length != 2) {
                throw new IllegalSjavaFileException(ERR_INVALID_ASSIGNMENT);
            }
            String varName = parts[0];
            validateVariableValue(parts[1], scopeValidator.getVariableType(varName));
            scopeValidator.validateAssignment(varName);
        }
    }

    /**
     * Checks if a given variable's value is valid
     *
     * @param value the variable's value
     * @param type  the variable's type
     * @throws IllegalSjavaFileException if the value is incompatible with the given variable
     */
    public void validateVariableValue(String value, Types type) throws IllegalSjavaFileException {
        Types valueType;
        try {
            variableParser.validateIdentifier(value);
            valueType = scopeValidator.getVariableType(value);
        } catch (IllegalSjavaFileException e) {
            typeValidator.validateLiteralType(type, value); //variable not found, try literal type instead
            return;
        }

        // If it's a valid identifier, check type compatibility
        typeValidator.validateTypeCompatibility(type, valueType);
        scopeValidator.validateVariableInitialization(value);
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

        validateCondition(matcher.group(1).trim());
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
        if (condition.startsWith(OR) || condition.startsWith(AND) ||
            condition.endsWith(OR) || condition.endsWith(AND)) {
            throw new IllegalSjavaFileException(ERR_INVALID_OPERATORS);
        }

        // Split by || and &&, discarding the operators
        for (String token : condition.split(LOGICAL_OPERATOR_REGEX)) {
            if (token.isEmpty()) {
                throw new IllegalSjavaFileException(ERR_CONSECUTIVE_OPERATORS);
            }
            validateSingleCondition(token);
        }
    }

    private void validateSingleCondition(String condition) throws IllegalSjavaFileException {
        // Check for boolean literals
        if (condition.equals(TRUE) || condition.equals(FALSE)) {
            return;
        }

        // Check for numeric literal
        try {
            Double.parseDouble(condition);
        } catch (NumberFormatException e) {
            // Must be a variable - validate it exists and has a compatible type
            typeValidator.validateConditionType(scopeValidator.getVariableType(condition));
            scopeValidator.validateVariableInitialization(condition);
        }
    }
}