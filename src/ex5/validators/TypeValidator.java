package ex5.validators;

import ex5.IllegalSjavaFileException;
import ex5.parser.Types;

/** Handles type compatibility validation for s-Java */
public class TypeValidator {
    /**
     * Checks if a value type can be assigned to a variable of target type
     *
     * @param targetType Type of the variable being assigned to
     * @param valueType  Type of the value being assigned
     * @throws IllegalSjavaFileException if types are incompatible
     */
    public void validateTypeCompatibility(Types targetType, Types valueType)
    throws IllegalSjavaFileException {
        // Same types are always compatible
        if (targetType == valueType) { return; }

        boolean isValid = true;
        switch (targetType) {
            case DOUBLE:
                if (valueType != Types.INT) {
                    isValid = false;
                }
                break;
            case BOOLEAN:
                if (valueType != Types.INT && valueType != Types.DOUBLE) {
                    isValid = false;
                }
                break;
            default:
                isValid = false;
        }

        if (!isValid) {
            throw new IllegalSjavaFileException("Cannot convert " + valueType + " to " + targetType);
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
                    if (!value.equals("true") && !value.equals("false")) {
                        // Check if it's a numeric value
                        try {
                            Double.parseDouble(value);
                        } catch (NumberFormatException e) {
                            throw new IllegalSjavaFileException("Invalid boolean value: " + value);
                        }
                    }
                    break;
                case CHAR:
                    if (value.length() != 3 || value.charAt(0) != '\'' || value.charAt(2) != '\'') {
                        throw new IllegalSjavaFileException("Invalid char literal: " + value);
                    }
                    break;
                case STRING:
                    if (!value.startsWith("\"") || !value.endsWith("\"")) {
                        throw new IllegalSjavaFileException("Invalid String literal: " + value);
                    }
            }
        } catch (NumberFormatException e) {
            throw new IllegalSjavaFileException("Invalid " + type + " value: " + value);
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
            throw new IllegalSjavaFileException("Invalid condition type: " + valueType);
        }
    }
}