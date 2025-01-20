package ex5.parser;

import ex5.IllegalSjavaFileException;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Base class for all parsers in the s-Java verifier
 * Contains common functionality and utilities used by specific parsers
 */
public abstract class BaseParser {
    // Common regex patterns
    protected static final String WHITESPACE = "\\s*";
    protected static final String IDENTIFIER = "[a-zA-Z][a-zA-Z0-9_]*";
    protected static final String FINAL = "final";

    // Legal types in s-Java
    protected static final String[] LEGAL_TYPES = {"int", "double", "String", "boolean", "char"};

    /**
     * Validates that a given identifier follows s-Java naming rules
     * @param identifier The identifier to validate
     * @throws IllegalSjavaFileException if the identifier is invalid
     */
    protected void validateIdentifier(String identifier) throws IllegalSjavaFileException {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalSjavaFileException("Empty identifier");
        }

        if (!Pattern.matches(IDENTIFIER, identifier)) {
            throw new IllegalSjavaFileException("Invalid identifier: " + identifier);
        }
    }

    /**
     * Checks if a given type is a legal s-Java type
     * @param type The type to check
     * @return true if the type is legal, false otherwise
     */
    protected boolean isLegalType(String type) {
        return Arrays.asList(LEGAL_TYPES).contains(type);
    }

    /**
     * Validates that a type is legal in s-Java
     * @param type The type to validate
     * @throws IllegalSjavaFileException if the type is not legal
     */
    protected void validateType(String type) throws IllegalSjavaFileException {
        if (!isLegalType(type)) {
            throw new IllegalSjavaFileException("Invalid type: " + type);
        }
    }
}