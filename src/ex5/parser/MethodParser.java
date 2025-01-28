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

    public MethodParser(ScopeValidator scopeValidator) {
        this.scopeValidator = scopeValidator;
    }

    private Method getMethod(String methodName) {
        for (Method method : methods) {
            if (methodName.equals(method.name())) {
                return method;
            }
        }
        return null;
    }

    private Variable parseParameter(String param) throws IllegalSjavaFileException {
        String[] paramParts = param.split("\\s+");
        boolean isFinal = paramParts[0].equals("final");
        int typeIndex = isFinal ? 1 : 0;

        Types type = Types.getType(paramParts[typeIndex]);
        String name = paramParts[typeIndex + 1];
        return new Variable(name, type, isFinal, true);
    }

    public List<Variable> parseParameters(String[] params) throws IllegalSjavaFileException {
        List<Variable> parameters = new ArrayList<>();
        for (String param : params) {
            parameters.add(parseParameter(param));
        }
        return parameters;
    }

    /**
     * Validates a method declaration line according to s-Java rules
     *
     * @param line The declaration line to validate
     * @throws IllegalSjavaFileException if the declaration is invalid
     */
    public void validateMethodDeclaration(String line) throws IllegalSjavaFileException {
        String methodName = line.substring(0, line.indexOf('(')).split("\\s+")[1];

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
        methods.add(new Method(methodName, parseParameters(params)));
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

    /**
     * Checks if a line is a valid return statement
     *
     * @param line The line to check
     * @return true if it's a valid return statement
     */
    public boolean isValidReturnStatement(String line) {
        return line.trim().matches("^\\s*return\\s*;\\s*$");
    }

    /**
     * extracts the parameters from a declaration
     *
     * @param declaration a declaration string
     * @return an array of parameters
     */
    public String[] extractParameters(String declaration) {
        int start = declaration.indexOf('(');
        int end = declaration.lastIndexOf(')');
        String params = declaration.substring(start + 1, end).trim();
        return params.isEmpty() ? new String[0] : params.split("\\s*,\\s*");
    }

    /**
     * Check if the method call is valid
     *
     * @param line a single line of code
     * @throws IllegalSjavaFileException if the method call isn't formatted correctly
     */
    public void validateMethodCall(String line) throws IllegalSjavaFileException {
        // Remove trailing semicolon and whitespace
        line = line.trim();

        String methodName = line.substring(0, line.indexOf('('));
        Method method = getMethod(methodName);
        if (method == null) {
            throw new IllegalSjavaFileException("Method not found: " + methodName);
        }

        String[] params = extractParameters(line);
        int expectedLength = method.parameters().size();
        if (params.length != expectedLength) {
            throw new IllegalSjavaFileException(
                    "Incompatible number of parameters: expected " + expectedLength + ", got " + params.length
            );
        }

        for (int i = 0; i < params.length; i++) {
            Types expectedType = method.parameters().get(i).getType();
            Types receivedType = scopeValidator.getVariableType(params[i]);
            if (!expectedType.equals(receivedType)) {
                throw new IllegalSjavaFileException(
                        "Incompatible parameter types: expected " + expectedType + ", got " + receivedType
                );
            }
            scopeValidator.validateVariableInitialization(params[i]);
        }
    }
}