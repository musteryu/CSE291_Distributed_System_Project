package rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StubInvocationHandler implements java.lang.reflect.InvocationHandler
{
    Object obj;
    public StubInvocationHandler(Object obj)
    {
        this.obj = obj;
    }
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
    {
        try {
            // 1. get the IP address somehow
            // 2. Create the connection
            // 3. Serilize everything and receive
            // 4. Recontruct the return value and return
            m.invoke(proxy,args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
        return null;
    }
}
