package com.genersoft.iot.vmp.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**    
 * @description:spring beanGet the factory and get the initialized in springbean
 * @author: swwheihei
 * @date:   2019June 25, 2018, afternoon4:51:52   
 * 
 */
@Component
public class SpringBeanFactory implements ApplicationContextAware {

	// Springapplication context
    private static ApplicationContext applicationContext;
    
    /**
     * realizeApplicationContextAwareThe callback method of the interface sets the context environment
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
    	SpringBeanFactory.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Get the object. The bean method is overridden here and plays a major role.
     */
    public static  <T> T getBean(String beanId) throws BeansException {
        if (applicationContext == null) {
            return null;
        }
        return (T) applicationContext.getBean(beanId);
    }

    /**
     * Get the current environment
     */
    public static String getActiveProfile() {
        return applicationContext.getEnvironment().getActiveProfiles()[0];
    }

}
