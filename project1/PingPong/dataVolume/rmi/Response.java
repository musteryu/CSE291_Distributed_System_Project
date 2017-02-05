package rmi;

import java.io.Serializable;

class Response implements Serializable{
    private Object o;
    private Throwable t;
    private State state;
    private enum State {
        success, exception
    }

    Response(Object o) {
        this.o = o;
        this.state = State.success;
    }

    Response(Throwable t){
        this.t = t;
        this.state = State.exception;
    }

    static Response result(Object obj) {
        return new Response(obj);
    }

    static Response except(Throwable t) {
        return new Response(t);
    }

    Object getOrThrow() throws Throwable {
        if (state == State.exception) throw t;
        return o;
    }



    @Override
    public String toString() {
        switch (state) {
            case success:
                return "<Response, Object: " + this.o + " >";
            case exception:
                return "<Response, Exception: " + this.t + " >";
        }
        return "";
    }
}
