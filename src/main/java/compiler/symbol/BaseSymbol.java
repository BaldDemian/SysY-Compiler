package compiler.symbol;

import compiler.scope.Scope;
import compiler.type.Type;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.LinkedList;
import java.util.List;

public class BaseSymbol implements Symbol {
    private LLVMValueRef llvmValueRef;
    private List<List<Integer>> positions = new LinkedList<>();
    private String name;
    private final Type type;
    private final Scope scope;
    private boolean init; // initialized or not?
    private boolean isConst = false; // const or not?
    private int line; // line number of declaration

    public BaseSymbol(String name, Type type, Scope scope, LLVMValueRef llvmValueRef) {
        this.name = name;
        this.type = type;
        this.scope = scope;
        this.llvmValueRef = llvmValueRef;
    }

    public BaseSymbol(String name, Type type, Scope scope, boolean init, int line) {
        this.name = name;
        this.type = type;
        this.scope = scope;
        this.init = init;
        this.line = line;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return this.type.toString();
    }

    @Override
    public Type getRealType() {
        return this.type;
    }

    @Override
    public Scope getScope() {
        return this.scope;
    }

    @Override
    public LLVMValueRef getLLVMValueRef() {
        return this.llvmValueRef;
    }

    @Override
    public void setLLVMValueRef(LLVMValueRef llvmValueRef) {
        this.llvmValueRef = llvmValueRef;
    }

    @Override
    public void addPos(List<Integer> pos) {
        this.positions.add(pos);
    }

    @Override
    public List<List<Integer>> getPos() {
        return this.positions;
    }

    @Override
    public void markConst() {
        this.isConst = true;
    }

    @Override
    public boolean isConst() {
        return this.isConst;
    }


    @Override
    public String toString() {
        // for debugging use!!!
        return "Symbol " + name + " in scope " + scope.getName() + " Type " + type;
    }
}

