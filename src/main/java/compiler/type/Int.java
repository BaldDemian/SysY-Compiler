package compiler.type;


public class Int implements Type {
    private int val;
    public boolean isConst = false;

    @Override
    public String toString() {
        return "Int";
    }

    public void setConst(boolean b) {
        this.isConst = b;
    }

    public boolean getConst() {
        return this.isConst;
    }

    public void setVal(int i) {
        this.val = i;
    }

    public int getVal() {
        return this.val;
    }
}
