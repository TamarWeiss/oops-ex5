package ex5.validators;

import ex5.exceptions.IllegalSjavaFileException;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/** Handles scope validation and variable tracking in s-Java */
public class ScopeValidator {
    private final Map<String, Variable> globalScope = new HashMap<>();
    private final Stack<Map<String, Variable>> scopeStack = new Stack<>();
    private int currentScopeDepth = 0;  // Track nested scope depth
    private boolean inMethod = false;

    /**
     * Checks if the current closing brace ends a method
     *
     * @return true if this closes a method, false if it closes a nested block
     */
    public boolean isMethodEnd() {
        return inMethod && currentScopeDepth == 1;
    }

    /** Represents a variable and its properties */
    private static class Variable {
        final String type;
        final boolean isFinal;
        boolean isInitialized;

        Variable(String type, boolean isFinal, boolean isInitialized) {
            this.type = type;
            this.isFinal = isFinal;
            this.isInitialized = isInitialized;
        }
    }

    /** Enters a new scope (method or block) */
    public void enterScope(boolean isMethod) {
        if (isMethod) {
            inMethod = true;
            currentScopeDepth = 0;
        }
        currentScopeDepth++;
        scopeStack.push(new HashMap<>());
    }

    /** Exits the current scope */
    public void exitScope(boolean isMethod) {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
            currentScopeDepth--;
        }
        if (isMethod) {
            inMethod = false;
        }
    }

    /** Declares a variable in the current scope */
    public void declareVariable(
            String name, String type, boolean isFinal, boolean isInitialized
    ) throws IllegalSjavaFileException {
        Map<String, Variable> currentScope = scopeStack.isEmpty() ? globalScope : scopeStack.peek();

        // Check if variable exists in current scope
        if (currentScope.containsKey(name)) {
            throw new IllegalSjavaFileException("Variable already declared in current scope: " + name);
        }

        // For global scope, check no other global has same name
        if (currentScope == globalScope && findVariable(name) != null) {
            throw new IllegalSjavaFileException("Global variable name conflict: " + name);
        }

        currentScope.put(name, new Variable(type, isFinal, isInitialized));
    }

    /** Checks if a variable can be assigned a value */
    public void validateAssignment(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        if (var == null) {
            throw new IllegalSjavaFileException("Variable not declared: " + name);
        }
        if (var.isFinal && var.isInitialized) {
            throw new IllegalSjavaFileException("Cannot reassign final variable: " + name);
        }
        var.isInitialized = true;
    }

    /** Checks if a variable is accessible and initialized */
    public void validateVariableAccess(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        if (var == null) {
            throw new IllegalSjavaFileException("Variable not declared: " + name);
        }
        if (!var.isInitialized && !globalScope.containsKey(name)) {
            throw new IllegalSjavaFileException("Local variable not initialized: " + name);
        }
    }

    /** Finds a variable in the current scope chain */
    private Variable findVariable(String name) {
        // Check local scopes from innermost to outermost
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Variable var = scopeStack.get(i).get(name);
            if (var != null) {
                return var;
            }
        }
        // Check global scope
        return globalScope.get(name);
    }

    /** Gets the type of variable if it exists and is accessible */
    public String getVariableType(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        if (var == null) {
            throw new IllegalSjavaFileException("Variable not declared: " + name);
        }
        return var.type;
    }
}