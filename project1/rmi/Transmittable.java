package rmi;

import java.io.Serializable;
public class Transmittable implements Serializable{
    private Object carryOn;
    private boolean isException;

    public Transmittable(Object o, boolean isException){
        this.carryOn = o;
        this.isException = isException;
    }

    public boolean checkException(){
        return this.isException;
    }

    public Object retrieve(){
        return this.carryOn;
    }
}
