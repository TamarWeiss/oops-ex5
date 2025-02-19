package ex5.parser;

import ex5.IllegalSjavaFileException;

import java.util.regex.Pattern;

/**
 * Base class for all parsers in the s-Java verifier
 * Contains common functionality and utilities used by specific parsers
 */
public abstract class BaseParser {
    /** Regex pattern for a valid s-Java identifier */
    protected static final String IDENTIFIER = "([a-zA-Z]\\w*|_[a-zA-Z\\d]\\w*)";
    /** Regex pattern for a valid s-Java type */
    protected static final String RETURN_PATTERN = "^\\s*return\\s*;\\s*$";
    private static final String EMPTY_IDENTIFIER_ERR = "Empty identifier";
    private static final String INVALID_IDENTIFIER_ERR = "Invalid identifier: ";

    /**
     * Validates that a given identifier follows s-Java naming rules
     *
     * @param identifier The identifier to validate
     * @throws IllegalSjavaFileException if the identifier is invalid
     */
    public void validateIdentifier(String identifier) throws IllegalSjavaFileException {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalSjavaFileException(EMPTY_IDENTIFIER_ERR);
        }

        if (!Pattern.matches(IDENTIFIER, identifier)) {
            throw new IllegalSjavaFileException(INVALID_IDENTIFIER_ERR + identifier);
        }
    }
}