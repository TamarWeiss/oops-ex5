package ex5.parser;

import ex5.IllegalSjavaFileException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser for handling method declarations and bodies in s-Java */
public class MethodParser extends BaseParser {
    // Method declaration pattern
    private static final String METHOD_PATTERN =
            "^\\s*void\\s+([a-zA-Z]\\w*)\\s*\\(\\s*(" +
            "(?:(?:final\\s+)?(?:" + String.join("|", LEGAL_TYPES) + ")\\s+" + IDENTIFIER +
            "(?:\\s*,\\s*(?:final\\s+)?(?:" + String.join("|", LEGAL_TYPES) + ")\\s+" + IDENTIFIER + ")*" +
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
            throw new IllegalSjavaFileException("Invalid method declaration format", -1);
        }

        String methodName = matcher.group(1);

        // Check for method overloading (not allowed in s-Java)
        if (methodNames.contains(methodName)) {
            throw new IllegalSjavaFileException("Method overloading is not allowed: " + methodName, -1);
        }
        methodNames.add(methodName);

        // Validate method name (must start with a letter)
        if (!methodName.matches("^[a-zA-Z]\\w*$")) {
            throw new IllegalSjavaFileException("Invalid method name: " + methodName, -1);
        }

        // Validate parameters if present
        String params = matcher.group(2);
        if (params != null && !params.trim().isEmpty()) {
            validateParameters(params.trim());
        }
    }

    /**
     * Validates method parameters
     *
     * @param params The parameter strings to validate
     * @throws IllegalSjavaFileException if the parameters are invalid
     */
    private void validateParameters(String params) throws IllegalSjavaFileException {
        String[] paramList = params.split("\\s*,\\s*");

        for (String param : paramList) {
            String[] parts = param.trim().split("\\s+");

            // Check for 2 or 3 parts (final modifier is optional)
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalSjavaFileException("Invalid parameter format: " + param, -1);
            }

            int typeIndex = parts.length == 3 ? 1 : 0;

            // Validate final modifier if present
            if (parts.length == 3 && !parts[0].equals("final")) {
                throw new IllegalSjavaFileException("Invalid parameter modifier: " + parts[0], -1);
            }

            // Validate type
            validateType(parts[typeIndex]);

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
}