package compiler.scope;

import compiler.symbol.Symbol;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
    private final Scope enclosingScope; // parent scope
    private String name; // name of this scope
    private final Map<String, Symbol> symbols = new HashMap<>();
    private int start; // start line number of this scope
    private int end; // end line number of this scope

    public BaseScope(String name, Scope enclosingScope) {
        this.name = name;
        this.enclosingScope = enclosingScope;
    }

    public BaseScope(String name, Scope enclosingScope, int start) {
        this.name = name;
        this.enclosingScope = enclosingScope;
        this.start = start;
    }

    @Override
    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    @Override
    public void define(String name, Symbol symbol) {
        symbols.put(name, symbol);
    }

    @Override
    public Symbol resolve(String name) {
        Symbol res = this.symbols.get(name);
        if (res != null) {
            return res; // find symbol in this scope
        }
        if (this.enclosingScope == null) {
            return null; // cannot find symbol in any scope!
        }
        return this.enclosingScope.resolve(name);
    }


    @Override
    public Map<String, Symbol> getSymbols() {
        return this.symbols;
    }

    @Override
    public Scope getEnclosingScope() {
        return this.enclosingScope;
    }
}
