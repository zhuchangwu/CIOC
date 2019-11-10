package com.changwu.test;

import com.changwu.config.MainConfig;
import com.changwu.dao.DaoImpl1;
import com.changwu.factory.AnnotationBeanFactory;
import com.changwu.factory.XmlBeanFactory;
import com.changwu.service.UserService;
import com.changwu.service.UserServiceImpl;
import org.junit.jupiter.api.Test;

/**
 * @Author: Changwu
 * @Date: 2019/10/10 18:35
 */
public class MainTest {

    @Test
    public void testForAnnotationBeanFactory() {
        AnnotationBeanFactory annotationBeanFactory = new AnnotationBeanFactory(MainConfig.class);
        // UserServiceImpl myService =(UserServiceImpl) annotationBeanFactory.getBean("myService2");
        UserServiceImpl myService = (UserServiceImpl) annotationBeanFactory.getBean(UserServiceImpl.class);
        // UserServiceImpl myService =(UserServiceImpl) annotationBeanFactory.getBean(UserService.class);
        myService.find();
    }

    @Test
    public void testForXmlBeanFactory() {
        XmlBeanFactory beanFactory = new XmlBeanFactory("spring.xml");
        UserService service = (UserService) beanFactory.getBean("service");
        service.find();
    }

}
