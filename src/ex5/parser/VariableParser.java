package ex5.parser;

import ex5.IllegalSjavaFileException;

import java.util.regex.Pattern;

import static ex5.Constants.*;

/** Parser for handling variable declarations and assignments in s-Java */
public class VariableParser extends BaseParser {
    private static final String DECLARATION_PATTERN =
            "^\\s*(" + FINAL + WHITESPACE + ")?(" + Types.LEGAL_TYPES + ")" + WHITESPACE +
            "(" + IDENTIFIER + "(?:" + EQUALS + "[^,;]+)?" + "(?:" + COMMA + IDENTIFIER +
            "(?:" + EQUALS + "[^,;]+)?)*)" + SEMICOLON + "$";
    private static final String DOUBLE_UNDERSCORE_PATTERN = ".*\\b__\\w+.*";
    private static final String IDENTIFIER_SEPARATOR = "\\s*[=,;]\\s*";

    private static final String ERR_INVALID_FORMAT = "Invalid variable declaration format";
    private static final String ERR_UNDERSCORE = "Variable names cannot start with __";

    /**
     * Validates a variable declaration line according to s-Java rules
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if the declaration is invalid
     */
    public void validateDeclaration(String line) throws IllegalSjavaFileException {
        if (!Pattern.matches(DECLARATION_PATTERN, line)) {
            throw new IllegalSjavaFileException(ERR_INVALID_FORMAT);
        }

        // Verify no double underscores at the start
        if (line.matches(DOUBLE_UNDERSCORE_PATTERN)) {
            throw new IllegalSjavaFileException(ERR_UNDERSCORE);
        }

        // Extract and validate each variable name
        for (String part : line.trim().split(IDENTIFIER_SEPARATOR)) {
            if (part.matches(IDENTIFIER)) {
                validateIdentifier(part);
            }
        }
    }
}