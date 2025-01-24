package ex5.validators;

import ex5.IllegalSjavaFileException;
import ex5.parser.Types;

import java.util.*;

/**
 * Manages scope and variable tracking for s-Java verification.
 * Handles both global and local scopes, variable declarations, and access validation.
 */
public class ScopeValidator {
    /** Represents a variable and its properties */
    private static class Variable {
        final Types type;
        final boolean isFinal;
        boolean isInitialized;
        final int declarationLine;

        Variable(Types type, boolean isFinal, boolean isInitialized, int declarationLine) {
            this.type = type;
            this.isFinal = isFinal;
            this.isInitialized = isInitialized;
            this.declarationLine = declarationLine;
        }
    }

    /** Represents a single scope level */
    private static class Scope {
        final Map<String, Variable> variables;
        final boolean isMethodScope;
        final Set<String> methodParameters;

        Scope(boolean isMethodScope) {
            this.variables = new HashMap<>();
            this.isMethodScope = isMethodScope;
            this.methodParameters = isMethodScope ? new HashSet<>() : null;
        }
    }

    private final Map<String, Variable> globalScope;
    private final Deque<Scope> scopeStack;
    private boolean inMethod;
    private int currentLine;

    private int nestingLevel = 0;
    private static final int MAX_NESTING_LEVEL = Integer.MAX_VALUE;

    public ScopeValidator() {
        this.globalScope = new HashMap<>();
        this.scopeStack = new ArrayDeque<>();
        this.inMethod = false;
        this.currentLine = 0;
    }

    /** Sets the current line number for better error reporting */
    public void setCurrentLine(int lineNumber) {
        this.currentLine = lineNumber;
    }

    /**
     * Enters a new scope (method or block)
     *
     * @param isMethodScope true if this is a method scope, false for block scope
     */
    public void enterScope(boolean isMethodScope) throws IllegalSjavaFileException {
        nestingLevel++;
        if (nestingLevel > MAX_NESTING_LEVEL) {
            throw new IllegalSjavaFileException("Maximum nesting level exceeded", currentLine);
        }
        if (isMethodScope) {
            inMethod = true;
        }
        scopeStack.push(new Scope(isMethodScope));
    }

    /**
     * Exits the current scope
     *
     * @param isMethodEnd true if this is ending a method, false for block
     */
    public void exitScope(boolean isMethodEnd) throws IllegalSjavaFileException {
        nestingLevel--;
        if (scopeStack.isEmpty()) {
            throw new IllegalSjavaFileException("Unexpected scope end", currentLine);
        }

        Scope scope = scopeStack.pop();
        if (isMethodEnd) {
            if (!scope.isMethodScope) {
                throw new IllegalSjavaFileException("Mismatched scope end", currentLine);
            }
            inMethod = false;
        }
    }

    /** Declares a method parameter */
    public void declareParameter(String name, Types type, boolean isFinal) throws IllegalSjavaFileException {
        if (scopeStack.isEmpty() || !scopeStack.peek().isMethodScope) {
            throw new IllegalSjavaFileException(
                    "Method parameter declaration outside method scope", currentLine
            );
        }

        Scope methodScope = scopeStack.peek();
        if (methodScope.methodParameters.contains(name)) {
            throw new IllegalSjavaFileException("Duplicate parameter name: " + name, currentLine);
        }

        methodScope.methodParameters.add(name);
        methodScope.variables.put(name, new Variable(type, isFinal, true, currentLine));
    }

    /** Declares a variable in the current scope */
    public void declareVariable(String name, Types type, boolean isFinal, boolean isInitialized)
    throws IllegalSjavaFileException {
        // Validate variable name doesn't start with double underscore
        if (name.startsWith("__")) {
            throw new IllegalSjavaFileException(
                    "Variable names cannot start with double underscore: " + name, currentLine
            );
        }

        Map<String, Variable> currentScope = scopeStack.isEmpty() ? globalScope : scopeStack.peek().variables;

        // Check for variable redeclaration in the current scope
        if (currentScope.containsKey(name)) {
            throw new IllegalSjavaFileException(
                    "Variable already declared in current scope: " + name, currentLine
            );
        }

        // For global variables, ensure no other global has the same name
        if (currentScope == globalScope) {
            Variable existing = findVariable(name);
            if (existing != null) {
                throw new IllegalSjavaFileException("Global variable name conflict: " + name, currentLine);
            }
        }

        // Add the variable to the current scope
        currentScope.put(name, new Variable(type, isFinal, isInitialized, currentLine));
    }

    /** Validates variable assignment */
    public void validateAssignment(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        if (var == null) {
            throw new IllegalSjavaFileException("Variable not declared: " + name, currentLine);
        }
        if (var.isFinal && var.isInitialized) {
            throw new IllegalSjavaFileException("Cannot reassign final variable: " + name, currentLine);
        }
        var.isInitialized = true;
    }

    /** Validates variable access and returns its type */
    public Types getVariableType(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        if (var == null) {
            throw new IllegalSjavaFileException("Variable not declared: " + name, currentLine);
        }
        // For local variables, check initialization
        if (!isGlobalVariable(name) && !var.isInitialized) {
            throw new IllegalSjavaFileException("Local variable not initialized: " + name, currentLine);
        }
        return var.type;
    }

    /** Checks if currently in a method scope */
    public boolean isInMethod() {
        return inMethod;
    }

    /** Checks if the current scope is a method's outermost scope */
    public boolean isMethodEnd() {
        return !scopeStack.isEmpty() && scopeStack.peek().isMethodScope;
    }

    /** Finds a variable in the current scope chain */
    private Variable findVariable(String name) {
        // Check local scopes from innermost to outermost
        for (Scope scope : scopeStack) {
            Variable var = scope.variables.get(name);
            if (var != null) {
                return var;
            }
        }
        // Check global scope
        return globalScope.get(name);
    }

    /** Checks if a variable is in the global scope */
    private boolean isGlobalVariable(String name) {
        return globalScope.containsKey(name);
    }

    /** Resets the validator state */
    public void reset() {
        globalScope.clear();
        scopeStack.clear();
        inMethod = false;
        currentLine = 0;
        nestingLevel = 0;
    }
}