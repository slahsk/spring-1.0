package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ListableBeanFactory;

public interface ConfigurableListableBeanFactory extends ListableBeanFactory, ConfigurableBeanFactory, AutowireCapableBeanFactory{
	void preInstantiateSingletons();
}
