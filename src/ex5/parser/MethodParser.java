package ex5.parser;

import ex5.IllegalSjavaFileException;
import ex5.validators.ScopeValidator;
import ex5.validators.ScopeValidator.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static ex5.Constants.*;

/**
 * Parser for handling method declarations and bodies in s-Java
 * also checks for valid return statements and method calls
 */
public class MethodParser extends BaseParser {
    private static final String METHOD_NAME_REGEX = "^[a-zA-Z]\\w*$";
    private static final String OPEN_PAREN = "(";
    private static final String CLOSE_PAREN = ")";
    private static final int MIN_VARIABLE_LENGTH = 2;
    private static final int MAX_VARIABLE_LENGTH = 3;

    // Error messages
    private static final String ERR_NESTED_METHODS = "Nested method declarations are not allowed";
    private static final String ERR_METHOD_OVERLOAD = "Method overloading is not allowed: ";
    private static final String ERR_INVALID_METHOD_NAME = "Invalid method name: ";
    private static final String ERR_RETURN_OUTSIDE = "Return statement outside method";
    private static final String ERR_INVALID_RETURN = "Invalid return statement format";
    private static final String ERR_CALL_OUTSIDE = "Method call outside method body";
    private static final String ERR_METHOD_NOT_FOUND = "Method not found: ";
    private static final String ERR_PARAM_COUNT = "Incompatible number of parameters: expected ";
    private static final String ERR_PARAM_FORMAT = "Invalid parameter format: ";
    private static final String ERR_PARAM_MODIFIER = "Invalid parameter modifier: ";
    private static final String GOT = ", got ";

    private record Method(String name, List<Variable> parameters) { }

    private final List<Method> methods = new ArrayList<>();
    private final ScopeValidator scopeValidator;
    private final BiConsumer<String, Types> validateValueCallback;

    /**
     * The class main constructor
     *
     * @param scopeValidator A ScopeValidator instance
     */
    public MethodParser(ScopeValidator scopeValidator, BiConsumer<String, Types> validateValueCallback) {
        this.scopeValidator = scopeValidator;
        this.validateValueCallback = validateValueCallback;
    }

    /**
     * Validates a method declaration line according to s-Java rules
     *
     * @param line The declaration line to validate
     * @throws IllegalSjavaFileException if the declaration is invalid
     */
    public void validateMethodDeclaration(String line) throws IllegalSjavaFileException {
        if (scopeValidator.isInMethod()) {
            throw new IllegalSjavaFileException(ERR_NESTED_METHODS);
        }

        String methodName = getMethodName(line, LineType.METHOD_DECLARATION);

        // Check for method overloading (not allowed in s-Java)
        if (getMethod(methodName) != null) {
            throw new IllegalSjavaFileException(ERR_METHOD_OVERLOAD + methodName);
        }

        // Validate method name (must start with a letter)
        if (!methodName.matches(METHOD_NAME_REGEX)) {
            throw new IllegalSjavaFileException(ERR_INVALID_METHOD_NAME + methodName);
        }

        String[] params = extractParameters(line);
        validateParameters(params); // Validate parameters if present
        scopeValidator.enterScope(true);

        Method method = new Method(methodName, new ArrayList<>());
        for (String param : params) {
            method.parameters.add(parseParameter(param));
        }
        methods.add(method);
    }

    /**
     * Validates return statements
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the return statement is outside a method, or improperly formatted
     */
    public void validatesReturnStatement(String line) throws IllegalSjavaFileException {
        if (!scopeValidator.isInMethod()) {
            throw new IllegalSjavaFileException(ERR_RETURN_OUTSIDE);
        }

        if (!line.matches(RETURN_PATTERN)) {
            throw new IllegalSjavaFileException(ERR_INVALID_RETURN);
        }
    }

    /**
     * Check if the method call is valid
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the method call isn't formatted correctly
     */
    public void validateMethodCall(String line) throws IllegalSjavaFileException {
        if (!scopeValidator.isInMethod()) {
            throw new IllegalSjavaFileException(ERR_CALL_OUTSIDE);
        }

        String methodName = getMethodName(line, LineType.METHOD_CALL);
        Method method = getMethod(methodName);
        if (method == null) {
            throw new IllegalSjavaFileException(ERR_METHOD_NOT_FOUND + methodName);
        }

        String[] params = extractParameters(line);
        int expectedLength = method.parameters.size();
        if (params.length != expectedLength) {
            throw new IllegalSjavaFileException(ERR_PARAM_COUNT + expectedLength + GOT + params.length);
        }

        for (int i = 0; i < params.length; i++) {
            validateValueCallback.accept(params[i], method.parameters.get(i).getType());
        }
    }

    //----------------------- private method -----------------------------------------

    /**
     * Searched and return a Method instance with the received name
     *
     * @param methodName the method's name
     * @return the method's instance, null if such a method does not exist
     */
    private Method getMethod(String methodName) {
        for (Method method : methods) {
            if (methodName.equals(method.name)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Extracts the method's name according to the line type.
     *
     * @param line     a single line of code.
     * @param lineType its type, either a method declaration or a method call
     * @return the method's name
     */
    private String getMethodName(String line, LineType lineType) {
        String start = line.substring(0, line.indexOf(OPEN_PAREN)).trim();
        return lineType == LineType.METHOD_DECLARATION ? start.split(WHITESPACE)[1] : start;
    }

    /**
     * extracts the parameters from a method declaration / call
     *
     * @param line a method declaration / call string
     * @return an array of parameters
     */
    private String[] extractParameters(String line) {
        String params = line.substring(
                line.indexOf(OPEN_PAREN) + 1, line.lastIndexOf(CLOSE_PAREN)
        ).trim();
        return params.isEmpty() ? new String[0] : params.split(COMMA);
    }

    /**
     * Converts and declares a parameter string into a variable instance
     *
     * @param param the parameter as a string
     * @return it's variable equivalent
     * @throws IllegalSjavaFileException if the parameter's format is incorrect in any way
     */
    private Variable parseParameter(String param) throws IllegalSjavaFileException {
        String[] paramParts = param.split(WHITESPACE);
        boolean isFinal = paramParts[0].equals(FINAL);
        int typeIndex = isFinal ? 1 : 0;

        Types type = Types.getType(paramParts[typeIndex]);
        String name = paramParts[typeIndex + 1];
        scopeValidator.declareParameter(name, type, isFinal);
        return new Variable(type, isFinal, true);
    }

    /**
     * Validates method parameters
     *
     * @param params The parameters to validate
     * @throws IllegalSjavaFileException if the parameters are invalid
     */
    private void validateParameters(String[] params) throws IllegalSjavaFileException {
        for (String param : params) {
            String[] parts = param.split(WHITESPACE);
            boolean isFinal = parts.length == MAX_VARIABLE_LENGTH;

            // Check for 2 or 3 parts (final modifier is optional)
            if (parts.length < MIN_VARIABLE_LENGTH || parts.length > MAX_VARIABLE_LENGTH) {
                throw new IllegalSjavaFileException(ERR_PARAM_FORMAT + param);
            }

            // Validate final modifier if present
            if (isFinal && !parts[0].equals(FINAL)) {
                throw new IllegalSjavaFileException(ERR_PARAM_MODIFIER + parts[0]);
            }

            int typeIndex = isFinal ? 1 : 0;
            Types.getType(parts[typeIndex]); // Validate type
            validateIdentifier(parts[typeIndex + 1]); // Validate parameter name
        }
    }
}