package com.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 */
public class ReflectionUtils {
    /**
     * 根据指定的类名创建实例
     * @param clazz 带创建对象的类
     * @return 创建好的对象
     * @param <T> 对象类型
     */
    public static <T> T newInstance(Class<T> clazz) {
       try {
           return clazz.newInstance();
       } catch (Exception e) {
           throw new IllegalStateException(e);
       }
    }

    /**
     * 获取当前类的公有方法
     * @param clazz
     * @return 当前类的所有公共方法
     */
    public static Method[] getPublicMethods(Class clazz) {
        //获取当前类不好含夫类的所有方法
        Method[] methods = clazz.getDeclaredMethods();
        List<Method> pmethods = new ArrayList<>();
        for (Method method : methods) {
            //获取修饰符 判断当前方法是否是public修饰
           if(Modifier.isPublic(method.getModifiers())){
               pmethods.add(method);
           }
        }
        return pmethods.toArray(new Method[0]);
    }

    /**
     * 调用指定对象的指定方法
     * @param object
     * @param method
     * @param args
     * @return
     */
    public static Object invoke(Object object, Method method, Object... args) {
        try {
            return method.invoke(object, args);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
