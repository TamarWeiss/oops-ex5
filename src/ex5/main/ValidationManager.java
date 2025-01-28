
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
    private static final String ERROR_NESTED_METHOD = "Nested method declarations are not allowed";
    private static final String ERROR_INVALID_ASSIGNMENT = "Invalid assignment format";
    private static final String ERROR_BLOCK_OUTSIDE_METHOD = "Block statement outside method";
    private static final String ERROR_INVALID_BLOCK_CONDITION = "Invalid block condition format";
    private static final String ERROR_MISSING_RETURN = "Missing return statement at method end";
    private static final String ERROR_RETURN_OUTSIDE_METHOD = "Return statement outside method";
    private static final String ERROR_INVALID_RETURN = "Invalid return statement format";
    private static final String ERROR_METHOD_CALL_OUTSIDE = "Method call outside method body";
    private static final String ERROR_CONSECUTIVE_OPERATORS = "Cannot have consecutive operators";
    private static final String ERROR_LOGICAL_OPERATORS = "Logical operators cannot be at start or end of condition";
    private static final String ERROR_FINAL_VAR_UNINITIALIZED = "Final variable must be initialized: ";
    private static final String FINAL_KEYWORD = "final";
    private static final String ASSIGNMENT_OPERATOR = "=";
    private static final String SEMICOLON = ";";
    private static final String COMMA = ",";
    private static final String SPACE = " ";
    private static final String EMPTY_STRING = "";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String INVALID_IDENTIFIER = "Invalid identifier: ";
    private static final String INVALID_LITERAL = "Invalid literal value: ";
    private static final String INVALID_LINE_FORMAT = "Invalid line format";
    private static final String LOGICAL_OPERATORS = "\\|\\||&&";


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
    private static final Pattern LOGICAL_OPERATOR = Pattern.compile("\\s*(\\|\\||&&)\\s*");

    /**
     * Constructor for ValidationManager.
     * Initializes all parsers and validators.
     */
    public ValidationManager() {
        this.lineParser = new LineParser();
        this.methodParser = new MethodParser();
        this.variableParser = new VariableParser();
        this.syntaxValidator = new SyntaxValidator();
        this.scopeValidator = new ScopeValidator();
        this.typeValidator = new TypeValidator();
        this.lastLineWasReturn = false;
    }

    /**
     * Validates a single line of code through the complete validation chain.
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
            syntaxValidator.validateLineEnding(line, lineType);

            // Process based on a line type
            switch (lineType) {
                case METHOD_DECLARATION -> processMethodDeclaration(line);
                case VARIABLE_DECLARATION -> processVariableDeclaration(line);
                case VARIABLE_ASSIGNMENT -> processVariableAssignment(line);
                case BLOCK_START -> processBlockStart(line);
                case BLOCK_END -> processBlockEnd();
                case RETURN_STATEMENT -> processReturnStatement(line);
                case METHOD_CALL -> processMethodCall(line);
                case INVALID -> throw new IllegalSjavaFileException(INVALID_LINE_FORMAT);
            }

            // Update return tracking for method validation
            lastLineWasReturn = lineType == LineType.RETURN_STATEMENT;

        } catch (IllegalSjavaFileException e) {
            throw new IllegalSjavaFileException(String.format("Line %d: %s", lineNumber, e.getMessage()));
        }
    }

    /**
     * Get current method status.
     *
     * @return the current method status
     */
    public boolean isInMethod() {
        return scopeValidator.isInMethod();
    }

    /** Reset all validators' state */
    public void reset() {
        lastLineWasReturn = false;
        scopeValidator.reset();
    }

    //---------------------------- private method ----------------------------------------

    /**
     * Processes method declarations.
     *
     * @param line A single line of code
     * @throws IllegalSjavaFileException if a nested method declaration occurred
     */
    private void processMethodDeclaration(String line) throws IllegalSjavaFileException {
        if (!isInMethod()) {
            methodParser.validateMethodDeclaration(line);
            scopeValidator.enterScope(true);

            // Process method parameters
            String[] params = methodParser.extractParameters(line);
            for (String param : params) {
                String[] paramParts = param.split("\\s+");
                boolean isFinal = paramParts[0].equals(FINAL_KEYWORD);
                int typeIndex = isFinal ? 1 : 0;

                Types type = Types.getType(paramParts[typeIndex]);
                String name = paramParts[typeIndex + 1];
                scopeValidator.declareParameter(name, type, isFinal);
            }
        } else {
            throw new IllegalSjavaFileException(ERROR_NESTED_METHOD);
        }
    }

    /**
     * Processes variable declarations.
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException for invalid variable declaration
     */
    private void processVariableDeclaration(String line) throws IllegalSjavaFileException {
        variableParser.validateDeclaration(line);

        // Handle multiple variable declarations
        String[] declarations = line.substring(0, line.length() - 1).split(COMMA);
        String firstDec = declarations[0].trim();
        boolean isFinal = firstDec.startsWith(FINAL_KEYWORD);

        // Extract type correctly
        String typeData = firstDec;
        if (isFinal) {
            typeData = firstDec.substring(FINAL_KEYWORD.length()).trim();
        }

        String[] typeAndName = typeData.split("\\s+");
        Types type = Types.getType(typeAndName[0]);

        // Process each declaration
        for (int i = 0; i < declarations.length; i++) {
            String declaration = declarations[i].trim();
            String name;
            boolean isInitialized;
            String value = null;

            if (i == 0) {
                // First declaration
                name = typeAndName[1];
                isInitialized = declaration.contains(ASSIGNMENT_OPERATOR);
                if (isInitialized) {
                    value = declaration.substring(declaration.indexOf(ASSIGNMENT_OPERATOR) + 1).trim();
                    if (value.endsWith(SEMICOLON)) {
                        value = value.substring(0, value.length() - 1).trim();
                    }
                }
            } else {
                // Subsequent declarations
                String[] parts = declaration.split(ASSIGNMENT_OPERATOR);
                name = parts[0].trim();
                isInitialized = parts.length > 1;
                if (isInitialized) {
                    value = parts[1].trim();
                    if (value.endsWith(SEMICOLON)) {
                        value = value.substring(0, value.length() - 1).trim();
                    }
                }
            }

            if (isFinal && !isInitialized) {
                throw new IllegalSjavaFileException(ERROR_FINAL_VAR_UNINITIALIZED + name);
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
                    // Not a valid identifier, try as literal
                    typeValidator.validateLiteralType(type, value);
                }
            }

            scopeValidator.declareVariable(name, type, isFinal, isInitialized);
        }
    }

    /**
     * Processes variable assignments.
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the assignment format is invalid
     */
    private void processVariableAssignment(String line) throws IllegalSjavaFileException {
        String[] assignments = line.substring(0, line.length() - 1).split(COMMA);

        for (String assignment : assignments) {
            Matcher matcher = VARIABLE_ASSIGNMENT_PATTERN.matcher(assignment);
            if (!matcher.matches()) {
                throw new IllegalSjavaFileException(ERROR_INVALID_ASSIGNMENT);
            }

            String varName = matcher.group(1);
            String value = matcher.group(2).trim();
            if (value.endsWith(SEMICOLON)) {
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
     * Processes block starts (if/while).
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the block statement isn't formatted correctly
     */
    private void processBlockStart(String line) throws IllegalSjavaFileException {
        if (!isInMethod()) {
            throw new IllegalSjavaFileException(ERROR_BLOCK_OUTSIDE_METHOD + " at line:" + line);
        }

        Matcher matcher = CONDITION_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalSjavaFileException(ERROR_INVALID_BLOCK_CONDITION + " at line:" + line);
        }

        String condition = matcher.group(1).trim();
        validateCondition(condition);
        scopeValidator.enterScope(false);
    }

    /**
     * Processes block ends.
     *
     * @throws IllegalSjavaFileException if the block's end isn't formatted correctly
     */
    private void processBlockEnd() throws IllegalSjavaFileException {
        boolean isMethodEnd = scopeValidator.isMethodEnd();
        if (isMethodEnd && !lastLineWasReturn) {
            throw new IllegalSjavaFileException(ERROR_MISSING_RETURN);
        }
        scopeValidator.exitScope(isMethodEnd);
    }

    /**
     * Processes return statements.
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the return statement is invalid
     */
    private void processReturnStatement(String line) throws IllegalSjavaFileException {
        if (!isInMethod()) {
            throw new IllegalSjavaFileException(ERROR_RETURN_OUTSIDE_METHOD);
        }

        if (!methodParser.isValidReturnStatement(line)) {
            throw new IllegalSjavaFileException(ERROR_INVALID_RETURN);
        }
    }

    /**
     * Processes method calls.
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the method call is invalid
     */
    private void processMethodCall(String line) throws IllegalSjavaFileException {
        if (!isInMethod()) {
            throw new IllegalSjavaFileException(ERROR_METHOD_CALL_OUTSIDE);
        }
        String[] params = methodParser.validateMethodCall(line);

        for (String param : params) {
            scopeValidator.getVariableType(param);
            scopeValidator.validateVariableInitialization(param);
        }
    }

    /**
     * Validates conditions in if/while statements.
     *
     * @param condition a string representing a condition
     * @throws IllegalSjavaFileException if the condition isn't formatted properly
     */
    private void validateCondition(String condition) throws IllegalSjavaFileException {
        // Remove leading/trailing whitespace
        condition = condition.trim();

        // Check for invalid operator placement at start/end
        if (condition.startsWith("||") || condition.startsWith("&&") ||
                condition.endsWith("||") || condition.endsWith("&&")) {
            throw new IllegalSjavaFileException(ERROR_LOGICAL_OPERATORS);
        }

        // Split by || and &&, discarding the operators
        String[] tokens = condition.split(LOGICAL_OPERATORS);

        for (String s : tokens) {
            String token = s.trim();
            if (token.isEmpty()) {
                throw new IllegalSjavaFileException(ERROR_CONSECUTIVE_OPERATORS);
            }
            validateSingleCondition(token);
        }
    }

    /**
     * Validates a single condition within an if/while statement.
     *
     * @param condition a string representing a single condition
     * @throws IllegalSjavaFileException if the condition isn't formatted properly
     */
    private void validateSingleCondition(String condition) throws IllegalSjavaFileException {
        // Check for boolean literals
        if (condition.equals(TRUE) || condition.equals(FALSE)) {
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