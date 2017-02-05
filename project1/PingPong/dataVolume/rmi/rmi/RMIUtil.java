package rmi;

import java.lang.reflect.Method;

/**
 * Created by musteryu on 2017/1/18.
 */
class RMIUtil {
    static <T> void checkInterface(Class<T> c) {
        if (!c.isInterface()) {
            throw new Error(c.getName() + " is not an interface");
        }
        for (Method method: c.getMethods()) {
            boolean interfaceMismatch = true;
            for (Class<?> excType: method.getExceptionTypes()) {
                if (excType.equals(RMIException.class)) {
                    interfaceMismatch = false;
                }
            }
            if (interfaceMismatch) {
                throw new Error(c.getName() + " does not represent a remote interface");
            }
        }
    }

    static void checkNotNull(Object... objects) {
        for (Object object: objects) {
            if (object == null) throw new NullPointerException();
        }
    }

}
