package com.changwu.factory;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: Changwu
 * @Date: 2019/10/10 18:50
 */
public class XmlBeanFactory {

    Map map = new HashMap<String, Object>();

    public XmlBeanFactory(String xml) {
        parseXml(xml);
    }

    /**
     * 解析xml
     *
     * @param xml
     */
    private void parseXml(String xml) {
        SAXReader reader = new SAXReader();
        //target目录里面的信息
        File file = new File(this.getClass().getResource("/").getPath() + "//" + xml);
        try {
            Document document = reader.read(file);
            Element rootElement = document.getRootElement();

            // 标记是否是自动装配
            boolean flag = false;
            // 判断是否开启了自动驻入
            Attribute rootAttribute = rootElement.attribute("default-autowire");
            if (rootAttribute != null) {
                flag = true;
            }
            // 实例化对象
            for (Iterator<Element> it = rootElement.elementIterator(); it.hasNext(); ) {
                Object object = null;
                Element element1 = it.next();
                // 解析<bean> 标签级别的对象
                String beanName = element1.attribute("id").getValue();
                String className = element1.attribute("class").getValue();
                Class clazz = Class.forName(className);

                // 二级迭代,检查当前<bean>下面是有还有子element
                for (Iterator<Element> it2 = element1.elementIterator(); it2.hasNext(); ) {
                    // 获取<bean> 标签的子标签
                    Element element2 = it2.next();
                    // 通过property注入==> setter方法
                    if (element2.getName().equals("property")) {
                        // 使用set方法完成bean的注入,按理说存在无参的构造函数,故直接newInstance()
                        object = clazz.newInstance();
                        Attribute ref = element2.attribute("ref");
                        if (ref != null) {
                            String refValue = ref.getValue();
                            Object injectObj = map.get(refValue);
                            if (injectObj != null) {
                                // 获取bean对象中对应的field,通过反射为他赋值
                                String attrName = element2.attribute("name").getValue();
                                Field field = clazz.getDeclaredField(attrName);
                                field.setAccessible(true);
                                // 完成依赖注入
                                field.set(object, injectObj);
                            }
                        }
                    // 构造方法注入
                    } else if (element2.getName().equals("constructor-arg")) {
                        // 使用构造方法完成bean的注入,默认的无参构造被覆盖,而不能直接newInstance()
                        Attribute ref = element2.attribute("ref");
                        if (ref != null) {
                            String refValue = ref.getValue();
                            if (refValue != null) {
                                // 从map中直接获取到它引用的对象实例
                                Object injectObj = map.get(refValue);
                                // 根据指定的参数的Class描述,反射出获取指定的构造方法
                                Constructor constructor = clazz.getConstructor(injectObj.getClass());
                                object = constructor.newInstance(injectObj);
                            }
                        }
                    } else {
                        throw new RuntimeException("非法的标签");
                    }
                }

                if (flag) {
                    // 满足自动注入的条件
                    if (rootAttribute.getValue().equals("byType")) {
                        // 如果是byType类型的自动装配,就是遍历目标类中的属性字段 和 map中的value的类型是否一致
                        Field[] declaredFields = clazz.getDeclaredFields();
                        for (Field declaredField : declaredFields) {
                            // Stirng aa => String.class
                            Class<?> type = declaredField.getType();
                            AtomicInteger count = new AtomicInteger();
                            AtomicReference injectObject = new AtomicReference();
                            map.forEach((k, v) -> {
                                // 遍历map, 判断map中的value的类型 == 当前Bean的属性的类型,就满足注入的条件
                                if (v.getClass().getName().equals(type.getName())) {
                                    // 如果存在相同情况,说明需要去自动装配
                                    injectObject.set(v);
                                    count.getAndIncrement();
                                }
                            });
                            if (count.get() > 1) {
                                throw new RuntimeException("需要一个对象,但是找到了两个相同类型的对象");
                            } else {
                                // 完成注入
                                // com.changwu.factory.BeanFactory can not access a member of class com.changwu.service.UserServiceImpl with modifiers "
                                declaredField.setAccessible(true);
                                object = clazz.newInstance();
                                declaredField.set(object, injectObject.get());
                            }
                        }
                    } else if (rootAttribute.getValue().equals("byName")) {
                        // 根据name完成主动注入,根据什么name呢? 就是目标对象的set方法中setXXX中的name
                        // 获取出所有的field
                        Method[] declaredMethods = clazz.getDeclaredMethods();
                        for (Method declaredMethod : declaredMethods) {
                            String methodName = declaredMethod.getName();
                            if (methodName.startsWith("set")){
                                // 判断入参位置的参数,是否是存在于Map中,如果不存在,就没有注入的必要
                                Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                                if(parameterTypes.length>1){
                                    continue;
                                }
                                Class paramType = parameterTypes[0];
                                AtomicBoolean flag1 = new AtomicBoolean(false);
                                map.forEach((k,v)->{
                                    if (v.getClass().getName().equals(paramType.getName())){
                                        flag1.set(true);
                                    }
                                });
                                if (flag1.get()){
                                    String injectObjName = methodName.substring(3, methodName.length());
                                    System.out.println("injectObjName== "+injectObjName);
                                    Object o = map.get(injectObjName);
                                    Object o1 = map.get("DaoImpl");
                                    if (o!=null){
                                        // 完成自动装配
                                        object = clazz.newInstance();
                                        Field[] declaredFields = clazz.getDeclaredFields();
                                        for (Field declaredField : declaredFields) {
                                            System.out.println( "declaredField.getType()  "+declaredField.getType());
                                            System.out.println( "paramType  "+paramType);
                                            if (declaredField.getType().equals(paramType)&&declaredField.getType().isAssignableFrom(o.getClass())){
                                                declaredField.setAccessible(true);
                                                declaredField.set(object,o);
                                            }else{
                                                throw new RuntimeException("byName 类型装配,命名异常,导致无法自动装配成功");
                                            }
                                        }
                                    }else{
                                        throw new RuntimeException("byName 类型装配,命名异常,导致无法自动装配成功");
                                    }
                                }
                            }
                        }
                    }
                }
            if (object == null) {
                // 没有字标签
                object = clazz.newInstance();
            }
            map.put(beanName, object);
        }
    } catch(Exception e) {
        e.printStackTrace();
    }

}
    public Object getBean(String name) {
        return map.get(name);
    }
}
