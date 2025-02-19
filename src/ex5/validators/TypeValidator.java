package ex5.validators;

import ex5.IllegalSjavaFileException;
import ex5.parser.Types;

import static ex5.Constants.FALSE;
import static ex5.Constants.TRUE;

/** Handles type compatibility validation for s-Java */
public class TypeValidator {
    // Error messages
    private static final String ERR_TYPE_CONVERSION = "Cannot convert %s to %s";
    private static final String ERR_INVALID_CHAR = "Invalid char literal: ";
    private static final String ERR_INVALID_STRING = "Invalid String literal: ";
    private static final String ERR_INVALID_VALUE = "Invalid %s value: %s";
    private static final String ERR_INVALID_CONDITION = "Invalid condition type: ";

    // Literal values
    private static final char SINGLE_QUOTE = '\'';
    private static final String DOUBLE_QUOTE = "\"";

    // Constants for validation
    private static final int CHAR_LENGTH = 3;

    /**
     * Checks if a value type can be assigned to a variable of target type
     *
     * @param target Type of the variable being assigned to
     * @param value  Type of the value being assigned
     * @throws IllegalSjavaFileException if types are incompatible
     */
    public void validateTypeCompatibility(Types target, Types value) throws IllegalSjavaFileException {
        boolean isValid = target == value || (target == Types.DOUBLE && value == Types.INT) ||
                          (target == Types.BOOLEAN && (value == Types.INT || value == Types.DOUBLE));

        if (!isValid) {
            throw new IllegalSjavaFileException(String.format(ERR_TYPE_CONVERSION, value, target));
        }
    }

    /**
     * Validates that a literal value matches its declared type
     *
     * @param type  The declared type
     * @param value The literal value as a string
     * @throws IllegalSjavaFileException if value doesn't match type
     */
    public void validateLiteralType(Types type, String value) throws IllegalSjavaFileException {
        try {
            switch (type) {
                case INT:
                    Integer.parseInt(value);
                    break;
                case DOUBLE:
                    Double.parseDouble(value);
                    break;
                case BOOLEAN:
                    if (!value.equals(TRUE) && !value.equals(FALSE)) {
                        Double.parseDouble(value); // Check if it's a numeric value
                    }
                    break;
                case CHAR:
                    if (value.length() != CHAR_LENGTH || value.charAt(0) != SINGLE_QUOTE ||
                        value.charAt(CHAR_LENGTH - 1) != SINGLE_QUOTE) {
                        throw new IllegalSjavaFileException(ERR_INVALID_CHAR + value);
                    }
                    break;
                case STRING:
                    if (!value.startsWith(DOUBLE_QUOTE) || !value.endsWith(DOUBLE_QUOTE)) {
                        throw new IllegalSjavaFileException(ERR_INVALID_STRING + value);
                    }
            }
        } catch (NumberFormatException e) {
            throw new IllegalSjavaFileException(String.format(ERR_INVALID_VALUE, type, value));
        }
    }

    /**
     * Validates a condition expression (for if/while statements)
     *
     * @param valueType The type of the condition expression
     * @throws IllegalSjavaFileException if type can't be used in condition
     */
    public void validateConditionType(Types valueType) throws IllegalSjavaFileException {
        if (valueType != Types.BOOLEAN && valueType != Types.INT && valueType != Types.DOUBLE) {
            throw new IllegalSjavaFileException(ERR_INVALID_CONDITION + valueType);
        }
    }
}