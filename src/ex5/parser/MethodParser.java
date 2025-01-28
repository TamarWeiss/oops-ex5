package ex5.parser;

import ex5.IllegalSjavaFileException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser for handling method declarations and bodies in s-Java */
public class MethodParser extends BaseParser {
    // Method declaration pattern
    private static final String METHOD_PATTERN = "^\\s*void\\s+([a-zA-Z]\\w*)\\s*\\(\\s*(" +
            "(?:(?:final\\s+)?(?:" + Types.LEGAL_TYPES + ")\\s+" + IDENTIFIER +
            "(?:\\s*,\\s*(?:final\\s+)?(?:" + Types.LEGAL_TYPES + ")\\s+" + IDENTIFIER + ")*" +
            ")?\\s*)\\)\\s*\\{\\s*$";

    private final List<String> methodNames = new ArrayList<>();

    /**
     * Validates a method declaration line according to s-Java rules
     *
     * @param line The declaration line to validate
     * @throws IllegalSjavaFileException if the declaration is invalid
     */
    public void validateMethodDeclaration(String line) throws IllegalSjavaFileException {
        Matcher matcher = Pattern.compile(METHOD_PATTERN).matcher(line);
        if (!matcher.matches()) {
            throw new IllegalSjavaFileException("Invalid method declaration format");
        }
        String methodName = matcher.group(1);

        // Check for method overloading (not allowed in s-Java)
        if (methodNames.contains(methodName)) {
            throw new IllegalSjavaFileException("Method overloading is not allowed: " + methodName);
        }
        methodNames.add(methodName);

        // Validate method name (must start with a letter)
        if (!methodName.matches("^[a-zA-Z]\\w*$")) {
            throw new IllegalSjavaFileException("Invalid method name: " + methodName);
        }

        // Validate parameters if present
        validateParameters(extractParameters(line));
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
     * @return the method's parameters
     */
    public String[] validateMethodCall(String line) throws IllegalSjavaFileException {
        // Remove trailing semicolon and whitespace
        line = line.trim();

        String methodName = line.substring(0, line.indexOf('('));
        if (!methodNames.contains(methodName)) {
            throw new IllegalSjavaFileException("Method not found: " + methodName);
        }

        return extractParameters(line);
    }
}