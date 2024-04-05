package compiler.type;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class Arr implements Type {
    private int len;
    public int[] val;
    public LLVMValueRef vectorPointer;
    public LLVMTypeRef vectorType;
    public boolean isConst = false;
    public Type elementType; // basic type of its elements
    public int dimension;

    @Override
    public String toString() {
        return "Arr";
    }

    public Arr(int len) {
        this.len = len;
        this.val = new int[len];
    }

    public Arr() {

    }

    public void setLen(int i) {
        this.len = i;
    }

    public int getLen() {
        return this.len;
    }

    public void setVal(int index, int i) {
        val[index] = i;
    }

    public int getVal(int index) {
        return this.val[index];
    }

    public void setConst(boolean b) {
        isConst = b;
    }

    public boolean getConst() {
        return this.isConst;
    }

    public void setElementType(Type type) {
        this.elementType = type;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getDimension() {
        return this.dimension;
    }
}
