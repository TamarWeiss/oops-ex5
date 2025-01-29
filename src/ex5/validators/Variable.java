package ex5.validators;

import ex5.parser.Types;

/** Represents a variable and its properties */
public class Variable {
    private final String name;
    private final Types type;
    private final boolean isFinal;
    private boolean isInitialized;

    public Variable(String name, Types type, boolean isFinal, boolean isInitialized) {
        this.name = name;
        this.type = type;
        this.isFinal = isFinal;
        this.isInitialized = isInitialized;
    }

    public String getName() {
        return name;
    }

    public Types getType() {
        return type;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

}
