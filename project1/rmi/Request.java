package rmi;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by musteryu on 2017/1/28.
 */
class Request implements Serializable {
    private Class<?>[] types;
    private Object[] args;
    private String methodName;

    Request(Method method) {
        this.types = null;
        this.args = null;
        this.methodName = method.getName();
    }

    Request(Method method, Object[] objects) {
        types = method.getParameterTypes();
        args = objects;
        this.methodName = method.getName();
    }

    boolean nonParams() {
        return types == null;
    }

    Class<?>[] unwrapTypes() throws ClassNotFoundException {
        if (types == null) return new Class<?>[0];
        return types;
    }

    Object[] unwrapParams() {
        if (args == null) return new Object[0];
        return args;
    }

    String unwrapMethodName() {
        return methodName;
    }
}
