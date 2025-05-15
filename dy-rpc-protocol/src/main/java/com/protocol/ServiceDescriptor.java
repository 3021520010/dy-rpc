package com.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 表示服务 存了某个类的方法的所有信息,当作注册服务的key
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceDescriptor {
    private String clazz;
    private String method;
    private String returnType;
    private String[] parameterTypes;
    public static ServiceDescriptor from(Class clazz, Method method){
        ServiceDescriptor descriptor = new ServiceDescriptor();
        descriptor.setClazz(clazz.getName());
        descriptor.setMethod(method.getName());
        descriptor.setReturnType(method.getReturnType().getName());
        Class<?>[] parameterClasses = method.getParameterTypes();
        String[] parameterTypes = new String[parameterClasses.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            parameterTypes[i] = parameterClasses[i].getName();
        }
        descriptor.setParameterTypes(parameterTypes);
        return descriptor;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }
        if(obj == null || obj.getClass() != getClass()){
            return false;
        }
        ServiceDescriptor that = (ServiceDescriptor) obj;
        return this.toString().equals(that.toString());
    }

    @Override
    public String toString() {
       return clazz+method+returnType+ Arrays.toString(parameterTypes);
    }
}
