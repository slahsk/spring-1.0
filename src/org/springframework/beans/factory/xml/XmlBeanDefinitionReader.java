package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.xml.sax.EntityResolver;

public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader{
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean validating = true;

	private EntityResolver entityResolver;

	private Class parserClass = DefaultXmlBeanDefinitionParser.class;

}
