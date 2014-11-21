package org.cloudfoundry.identity.uaa.login.test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Replaces the named bean with a standard RestTemplate for
 * compatibility with MockRestServiceServer
 */
public class UaaRestTemplateBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    private String beanName;

    public UaaRestTemplateBeanFactoryPostProcessor(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition(beanName);
        beanDefinition.setBeanClassName(RestTemplate.class.getCanonicalName());
        beanDefinition.getConstructorArgumentValues().clear();
    }
}
