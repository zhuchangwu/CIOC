package com.changwu.factory;

import com.changwu.anno.CAutowired;
import com.changwu.anno.CComponentScan;
import com.changwu.anno.CDao;
import com.changwu.anno.CService;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: Changwu
 * @Date: 2019/10/11 10:57
 */
public class AnnotationBeanFactory {
    private ConcurrentHashMap<String, Class> beanDefinitionMap = new ConcurrentHashMap<String, Class>();
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>();
    private ConcurrentHashMap<String, Object> singletonFactories = new ConcurrentHashMap<String, Object>();
    private List<DTO> dtoList = new ArrayList<>();

    public AnnotationBeanFactory(Class connfigClass) {
        Annotation annotation = connfigClass.getAnnotation(CComponentScan.class);
        if (annotation == null)
            throw new RuntimeException("请给Bean工厂指定包扫描相关的配置类");
        scan(((CComponentScan) annotation).value());
    }

    /**
     * spring包扫描的思路就是通过报名得到 target/ 中编译好了的xxx.class 的全路径, 然后将.class去掉就得到了带包名的全路径,随即反射出对象
     *
     * @param basePacket
     */
    public void scan(String basePacket) {
        String rootPath = this.getClass().getResource("/").getPath();
        basePacket = basePacket.replaceAll("\\.", "/");
        // 包扫描
        recursiveScan(rootPath + basePacket, basePacket);
        // 自动装配
        myAutowoired();
    }

    /**
     * 递归扫描
     *
     * @param rootName
     * @param basePacketName
     */
    public void recursiveScan(String rootName, String basePacketName) {
        File file = new File(rootName);
        if (file == null) {
            throw new RuntimeException("输入的目录有误,无法加载目录下的相关文件");
        }
        String fileName = file.getName();
        if (fileName.endsWith(".class")) {
            String className = rootName.substring(rootName.lastIndexOf(basePacketName, rootName.length())).replaceAll("/", ".").replaceAll(".class", "");
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(CService.class) && !clazz.isInterface() && !clazz.isAssignableFrom(String.class)) {
                    // 解析名字: 默认使用注解上的名字
                    String beanName = clazz.getAnnotation(CService.class).value();
                    resolveBeanName(beanName, clazz, className);
                } else if (clazz.isAnnotationPresent(CDao.class) && !clazz.isInterface() && !clazz.isAssignableFrom(String.class)) {
                    // 解析名字:默认使用注解上的名字
                    String beanName = clazz.getAnnotation(CDao.class).value();
                    resolveBeanName(beanName, clazz, className);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            String[] list = file.list();
            if (list.length >= 1) {
                for (String name : list) {
                    recursiveScan(rootName + "/" + name, basePacketName);
                }
            }
        }
        return;
    }

    public void resolveBeanName(String beanName, Class clazz, String className) {
        if (!beanName.equals("")) {
            beanDefinitionMap.put(beanName, clazz);
            dtoList.add(new DTO(beanName));
        } else {
            // 注解上没有添加就使用类名首字母小写版
            beanName = className.substring(className.lastIndexOf(".") + 1, className.length());
            String firstCase = beanName.substring(0, 1).toLowerCase();
            beanName = firstCase + beanName.substring(1, beanName.length());
            beanDefinitionMap.put(beanName, clazz);
            dtoList.add(new DTO(beanName));
        }
    }

    /**
     * 自动装配的逻辑
     *
     * @param
     * @return
     */
    public void myAutowoired() {
        for (DTO dto : dtoList) {
            getBean(dto);
        }
    }

    /**
     * 自动装配的逻辑
     *
     * @param dto
     * @return
     */
    private Object getBean(DTO dto) {
        // 容器启动后通过这种方式从IOC中往外获取对象
        AtomicReference<Object> bean1 = new AtomicReference<>();
        AtomicInteger tag= new AtomicInteger();

        this.singletonObjects.forEach((k, v) -> {
            if (v.getClass() == dto.getType()&&v.getClass().isAssignableFrom(dto.getType())) {
                tag.getAndIncrement();
                bean1.set(v);
            }
        });
        if (tag.get()>1){
            throw new RuntimeException("期待根据类型注入: "+dto.getType()+" 容器中却存在两个");
        }
        if (bean1.get() != null) {
            return bean1.get();
        }

        Object bean = null;
        // 如果已经存在了现成的对象直接返回
        if (dto.getName()!=null){
            Object alreadyCreatedBean = this.singletonFactories.get(dto.getName());
            if (alreadyCreatedBean != null) {
                return alreadyCreatedBean;
            }
        }

        try {
            // 默认按照类型
            if (dto.getType() != null && beanDefinitionMap.contains(dto.getType())) {
                bean = dto.getType().newInstance();
            } else if (dto.getName() != null && beanDefinitionMap.containsKey(dto.getName())) {
                // 再按照名称
                Class aClass = beanDefinitionMap.get(dto.getName());
                // 储存标记
                bean = aClass.newInstance();
            } else {
                throw new RuntimeException("非法的注入类型: 容器中没有您期望注入的类型");
            }
            this.singletonFactories.put(dto.getName(), bean);
            Field[] declaredFields = bean.getClass().getDeclaredFields();
            if (declaredFields.length > 0 && null != declaredFields) {
                for (Field declaredField : declaredFields) {
                    if (!declaredField.getType().isAssignableFrom(String.class)) {
                        CAutowired annotation = declaredField.getAnnotation(CAutowired.class);
                        if (annotation != null) {
                            //模仿Spring的做法，通过反射的方式按照类型，完成注入
                            //获取当前字段的类型为当前field完成注入
                            Object resultBean = getBean(new DTO(declaredField.getName(), declaredField.getType()));
                            declaredField.setAccessible(true);
                            declaredField.set(bean, resultBean);
                        }
                    }
                }
            }
            this.singletonObjects.put(dto.getName(), bean);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }

    class DTO {
        String name;
        Class type;

        public DTO() {
        }

        public DTO(Class type) {
            this.type = type;
        }

        public DTO(String name) {
            this.name = name;
        }

        public DTO(String name, Class type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Class getType() {
            return type;
        }

    }

    public Object getBean(String name) {
        return getBean(new DTO(name));
    }

    public Object getBean(Class clazz) {
        return getBean(new DTO(clazz));
    }

}
