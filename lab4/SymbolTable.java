import java.util.*;

class Symbol {
    String name;
    String type;
    boolean initialized;
    int declaredLine; // для сообщений
    Symbol(String name, String type, int line) {
        this.name = name; this.type = type; this.initialized = false; this.declaredLine = line;
    }
}

class Scope {
    Map<String, Symbol> symbols = new HashMap<>();
    Scope parent;
    Scope(Scope parent) { this.parent = parent; }
    void put(Symbol s) { symbols.put(s.name, s); }
    Symbol get(String name) {
        for (Scope sc = this; sc != null; sc = sc.parent) {
            if (sc.symbols.containsKey(name)) return sc.symbols.get(name);
        }
        return null;
    }
    boolean containsLocal(String name) { return symbols.containsKey(name); }
}

class SymbolTable {
    Scope currentScope;
    SymbolTable() { currentScope = new Scope(null); }
    void enterScope() { currentScope = new Scope(currentScope); }
    void leaveScope() { currentScope = currentScope.parent; }
    void declare(String name, String type, int line) throws SemanticException {
        if (currentScope.containsLocal(name))
            throw new SemanticException("Повторное объявление переменной " + name, line);
        currentScope.put(new Symbol(name, type, line));
    }
    Symbol lookup(String name) { return currentScope.get(name); }
    void setInitialized(String name) {
        Symbol s = lookup(name);
        if (s != null) s.initialized = true;
    }
    boolean isInitialized(String name) {
        Symbol s = lookup(name);
        return s != null && s.initialized;
    }
}

class SemanticException extends Exception {
    String msg; int line;
    SemanticException(String msg, int line) { super(msg + " (строка ~" + line + ")"); this.msg = msg; this.line = line; }
}