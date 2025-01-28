package ex5.parser;

import ex5.IllegalSjavaFileException;
import ex5.validators.ScopeValidator;
import ex5.validators.ScopeValidator.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for handling method declarations and bodies in s-Java
 * also checks for valid return statements and method calls
 */
public class MethodParser extends BaseParser {
    private record Method(String name, List<Variable> parameters) { }

    private final List<Method> methods = new ArrayList<>();
    private final ScopeValidator scopeValidator;

    /**
     * The class main constructor
     *
     * @param scopeValidator A ScopeValidator instance
     */
    public MethodParser(ScopeValidator scopeValidator) {
        this.scopeValidator = scopeValidator;
    }

    /**
     * Validates a method declaration line according to s-Java rules
     *
     * @param line The declaration line to validate
     * @throws IllegalSjavaFileException if the declaration is invalid
     */
    public void validateMethodDeclaration(String line) throws IllegalSjavaFileException {
        if (scopeValidator.isInMethod()) {
            throw new IllegalSjavaFileException("Nested method declarations are not allowed");
        }

        String methodName = getMethodName(line, LineType.METHOD_DECLARATION);

        // Check for method overloading (not allowed in s-Java)
        if (getMethod(methodName) != null) {
            throw new IllegalSjavaFileException("Method overloading is not allowed: " + methodName);
        }

        // Validate method name (must start with a letter)
        if (!methodName.matches("^[a-zA-Z]\\w*$")) {
            throw new IllegalSjavaFileException("Invalid method name: " + methodName);
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
     * Processes return statements
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the return statement is outside a method, or improperly formatted
     */
    public void processReturnStatement(String line) throws IllegalSjavaFileException {
        if (!scopeValidator.isInMethod()) {
            throw new IllegalSjavaFileException("Return statement outside method");
        }

        if (!line.trim().matches("^return\\s*;$")) {
            throw new IllegalSjavaFileException("Invalid return statement format");
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
            throw new IllegalSjavaFileException("Method call outside method body");
        }

        // Remove trailing semicolon and whitespace
        line = line.trim();

        String methodName = getMethodName(line, LineType.METHOD_CALL);
        Method method = getMethod(methodName);
        if (method == null) {
            throw new IllegalSjavaFileException("Method not found: " + methodName);
        }

        String[] params = extractParameters(line);
        int expectedLength = method.parameters.size();
        if (params.length != expectedLength) {
            throw new IllegalSjavaFileException(
                    "Incompatible number of parameters: expected " + expectedLength + ", got " + params.length
            );
        }

        for (int i = 0; i < params.length; i++) {
            Types expectedType = method.parameters.get(i).getType();
            Types receivedType = scopeValidator.getVariableType(params[i]);
            if (!expectedType.equals(receivedType)) {
                throw new IllegalSjavaFileException(
                        "Incompatible parameter types: expected " + expectedType + ", got " + receivedType
                );
            }
            scopeValidator.validateVariableInitialization(params[i]);
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
        String start = line.substring(0, line.indexOf('(')).trim();
        return lineType == LineType.METHOD_DECLARATION ? start.split("\\s+")[1] : start;
    }

    /**
     * extracts the parameters from a method declaration / call
     *
     * @param line a method declaration / call string
     * @return an array of parameters
     */
    private String[] extractParameters(String line) {
        String params = line.substring(
                line.indexOf('(') + 1, line.lastIndexOf(')')
        ).trim();
        return params.isEmpty() ? new String[0] : params.split("\\s*,\\s*");
    }

    /**
     * Converts and declares a parameter string into a variable instance
     *
     * @param param the parameter as a string
     * @return it's variable equivalent
     * @throws IllegalSjavaFileException if the parameter's format is incorrect in any way
     */
    private Variable parseParameter(String param) throws IllegalSjavaFileException {
        String[] paramParts = param.split("\\s+");
        boolean isFinal = paramParts[0].equals("final");
        int typeIndex = isFinal ? 1 : 0;

        Types type = Types.getType(paramParts[typeIndex]);
        String name = paramParts[typeIndex + 1];
        scopeValidator.declareParameter(name, type, isFinal);
        return new Variable(name, type, isFinal, true);
    }

    /**
     * Validates method parameters
     *
     * @param params The parameters to validate
     * @throws IllegalSjavaFileException if the parameters are invalid
     */
    private void validateParameters(String[] params) throws IllegalSjavaFileException {
        for (String param : params) {
            String[] parts = param.split("\\s+");

            // Check for 2 or 3 parts (final modifier is optional)
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalSjavaFileException("Invalid parameter format: " + param);
            }

            int typeIndex = parts.length == 3 ? 1 : 0;

            // Validate final modifier if present
            if (parts.length == 3 && !parts[0].equals("final")) {
                throw new IllegalSjavaFileException("Invalid parameter modifier: " + parts[0]);
            }

            // Validate type
            Types.getType(parts[typeIndex]);

            // Validate parameter name
            validateIdentifier(parts[typeIndex + 1]);
        }
    }
}