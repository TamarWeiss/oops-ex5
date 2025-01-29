package ex5.validators;

import ex5.parser.Types;

/** Represents a variable and its properties */
public class Variable {
    private final String name;
    private final Types type;
    private final boolean isFinal;
    private boolean isInitialized;

    /**
     * Creates a new variable
     *
     * @param name         the variable's name
     * @param type         the variable's type
     * @param isFinal      if the variable is final
     * @param isInitialized if the variable is initialized
     */
    public Variable(String name, Types type, boolean isFinal, boolean isInitialized) {
        this.name = name;
        this.type = type;
        this.isFinal = isFinal;
        this.isInitialized = isInitialized;
    }

    /**
     * @return the variable's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the variable's type
     */
    public Types getType() {
        return type;
    }

    /**
     * @return if the variable is final
     */
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * @return if the variable is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Sets the variable's initialization status
     *
     * @param initialized the new initialization status
     */
    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

}
