**这个小项目是我读过一点Spring的源码后,模仿Spring的IOC写的一个简易的IOC,当然Spring的在天上,我写的在马里亚纳海沟,哈哈**

项目中有两种方式实现IOC:
* 第一种是基于dom4j实现的解析XML配置文件版 
* 第二种是基于自定义注解实现全配置版

### 全注解版
**模仿Spring原生的IOC机制如下:**

* **Interface类型的beanDefinition不会被实例化****
* **String类型的beanDefinition不会被实例化**
* **维护三个核心的map容器**
    * **使用底层存放实例化对象的容器是一个叫`singletonObjects`的CurrentHashMap**
    * **第二个用来辅助解决循环依赖的容器叫`singletonFactories`类型:CurrentHashMap**
    * **第二个用来存放bean定义信息的map容器叫`beanDefinitionMap`类型:CurrentHashMap**

Spring底层的自己还封装了BeanDefinition, 当然我没干这件事,直接用的类的描述对象 Class

#### 自定义了四种注解如下:
* CDao 用于标识持久层的对象
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CDao {
    String value()default "";
}
```
* CService 用来标识服务层的对象
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CService {
    String value()default "";
}
```
* CComponentScan 用来标识主配置类,提供包扫描需要的base-packet
```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CComponentScan {
    String value()default "";
}
```
* CAutowired 用来标识需要自动装配的对象
```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CAutowired {
    String value()default "";
}
```

**当然他跟Spring原生的@Autowired是没法比的,Spring自动装配类型默认是Autowired_no, 但是被Spring原生标记上的对象会先按照默认的装配类型进行装配,如果没有默认的装配类型,再按照byType,如果容器中存在多个相同类型的对象,就按照byName, 名字再一样就直接报错了**

> **Spring是允许程序员去改这个默认的装配类型的**

**然后在我的IOC中就比较逊色了,直接默认按照byType,没有合适的类型再按照byName进行自动装配**

#### 解决了循环依赖的问题

在我手动写如何解决循环依赖的时候,那时候我还没有去看源码, 当时我花了几个流程图,但是还是卡壳了, 于是我去调试Spring的实现, **简直了!Spring的作者们简直是真神!**
其实说Spring如何解决循环依赖的,我前面有几个源码阅读的博客,感兴趣可以去看看

这里我就简单的说下, 这件事是一个叫`AutowiredAnnotationBeanDefinitonPostprocessor`的后置处理器完成的, Spring在做这件事是时候,前前后后是一个偌大的继承体系在支持,**但是归根结底是Spring玩了个漂亮的递归,方法名是getBean(),当然这个递归还有几个辅助容器,这几个容器就是我上面说的几个map ,我的IOC能写成,就得益于这一点**


### XML版

注解版的IOC我是用DOM4j解析XML配置文件实现的, 做了下面的功能
* 支持setter方法依赖注入

标识性的信息是 `property`
```xml
<bean id="dao1" class="com.changwu.dao.DaoImpl1"></bean>
<bean id="service" class="com.changwu.service.UserServiceImpl4">
    <property ref="dao1" name="daoImpl"></property>
</bean>
```

* 支持构造方法的依赖注入

标识性的信息是 `constructor-arg`
```xml
<bean id="DaoImpl" class="com.changwu.dao.DaoImpl1"></bean>
<bean id="service" class="com.changwu.service.UserServiceImpl3">
    <constructor-arg ref ="DaoImpl" name="DaoImpl1"></constructor-arg>
</bean>
</bean>
```

* 支持byType的自动装配

标识性的信息是 `byType`
```xml
<beans  default-autowire="byType">
```

* 主持byName的自动装配
```xml
<beans  default-autowire="byName">
```

**感兴趣的小伙伴可以去我的github拉取代码看着玩**