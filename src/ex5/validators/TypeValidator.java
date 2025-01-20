package ex5.validators;

import ex5.IllegalSjavaFileException;

/**
 * Handles type compatibility validation for s-Java
 */
public class TypeValidator {

    /**
     * Checks if a value type can be assigned to a variable of target type
     * @param targetType Type of the variable being assigned to
     * @param valueType Type of the value being assigned
     * @throws IllegalSjavaFileException if types are incompatible
     */
    public void validateTypeCompatibility(String targetType, String valueType)
            throws IllegalSjavaFileException {
        if (targetType.equals(valueType)) {
            return; // Same types are always compatible
        }

        switch (targetType) {
            case "double":
                if (!valueType.equals("int")) {
                    throw new IllegalSjavaFileException(
                            "Cannot convert " + valueType + " to double");
                }
                break;

            case "boolean":
                if (!valueType.equals("int") && !valueType.equals("double")) {
                    throw new IllegalSjavaFileException(
                            "Cannot convert " + valueType + " to boolean");
                }
                break;

            default:
                throw new IllegalSjavaFileException(
                        "Cannot convert " + valueType + " to " + targetType);
        }
    }

    /**
     * Validates that a literal value matches its declared type
     * @param type The declared type
     * @param value The literal value as a string
     * @throws IllegalSjavaFileException if value doesn't match type
     */
    public void validateLiteralType(String type, String value)
            throws IllegalSjavaFileException {
        try {
            switch (type) {
                case "int":
                    Integer.parseInt(value);
                    break;

                case "double":
                    Double.parseDouble(value);
                    break;

                case "boolean":
                    if (!value.equals("true") && !value.equals("false")) {
                        // Check if it's a numeric value
                        try {
                            Double.parseDouble(value);
                        } catch (NumberFormatException e) {
                            throw new IllegalSjavaFileException(
                                    "Invalid boolean value: " + value);
                        }
                    }
                    break;

                case "char":
                    if (value.length() != 3 || value.charAt(0) != '\'' ||
                            value.charAt(2) != '\'') {
                        throw new IllegalSjavaFileException(
                                "Invalid char literal: " + value);
                    }
                    break;

                case "String":
                    if (!value.startsWith("\"") || !value.endsWith("\"")) {
                        throw new IllegalSjavaFileException(
                                "Invalid String literal: " + value);
                    }
                    break;

                default:
                    throw new IllegalSjavaFileException("Unknown type: " + type);
            }
        } catch (NumberFormatException e) {
            throw new IllegalSjavaFileException(
                    "Invalid " + type + " value: " + value);
        }
    }

    /**
     * Validates a condition expression (for if/while statements)
     * @param valueType The type of the condition expression
     * @throws IllegalSjavaFileException if type can't be used in condition
     */
    public void validateConditionType(String valueType)
            throws IllegalSjavaFileException {
        if (!valueType.equals("boolean") &&
                !valueType.equals("int") &&
                !valueType.equals("double")) {
            throw new IllegalSjavaFileException(
                    "Invalid condition type: " + valueType);
        }
    }
}