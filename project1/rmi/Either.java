package rmi;

import java.io.Serializable;
public class Either implements Serializable{
    public enum Case {
        LEFT, RIGHT
    }
    private Object left;
    private Throwable right;
    private Case sf;

    private Either(Object o) {
        this.left = o;
        this.sf = Case.LEFT;
    }

    private Either(Exception ex){
        this.right = ex;
        this.sf = Case.RIGHT;
    }

    public static Either left(Object obj) {
        return new Either(obj);
    }

    public static Either right(Throwable t) {
        return new Either(t);
    }

    public Object getLeftOrThrowRight() throws Throwable {
        if (this.sf == Case.LEFT) return this.left;
        else throw this.right;
    }
}
