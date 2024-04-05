package compiler.scope;

public class LocalScope extends BaseScope {
    public LocalScope(String name, Scope enclosingScope) {
        super(name, enclosingScope);
    }

    public LocalScope(String name, Scope enclosingScope, int start) {
        super(name, enclosingScope, start);
    }
}
