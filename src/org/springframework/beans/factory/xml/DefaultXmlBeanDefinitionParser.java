package org.springframework.beans.factory.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DefaultXmlBeanDefinitionParser implements XmlBeanDefinitionParser {

	public static final String BEAN_NAME_DELIMITERS = ",; ";

	public static final String TRUE_VALUE = "true";
	public static final String DEFAULT_VALUE = "default";

	public static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";
	public static final String DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE = "default-dependency-check";
	public static final String DEFAULT_AUTOWIRE_ATTRIBUTE = "default-autowire";

	public static final String BEAN_ELEMENT = "bean";
	public static final String DESCRIPTION_ELEMENT = "description";
	public static final String CLASS_ATTRIBUTE = "class";
	public static final String PARENT_ATTRIBUTE = "parent";
	public static final String ID_ATTRIBUTE = "id";
	public static final String NAME_ATTRIBUTE = "name";
	public static final String SINGLETON_ATTRIBUTE = "singleton";
	public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";
	public static final String INIT_METHOD_ATTRIBUTE = "init-method";
	public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
	public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";
	public static final String INDEX_ATTRIBUTE = "index";
	public static final String TYPE_ATTRIBUTE = "type";
	public static final String PROPERTY_ELEMENT = "property";
	public static final String REF_ELEMENT = "ref";
	public static final String IDREF_ELEMENT = "idref";
	public static final String BEAN_REF_ATTRIBUTE = "bean";
	public static final String LOCAL_REF_ATTRIBUTE = "local";
	public static final String LIST_ELEMENT = "list";
	public static final String SET_ELEMENT = "set";
	public static final String MAP_ELEMENT = "map";
	public static final String KEY_ATTRIBUTE = "key";
	public static final String ENTRY_ELEMENT = "entry";
	public static final String VALUE_ELEMENT = "value";
	public static final String NULL_ELEMENT = "null";
	public static final String PROPS_ELEMENT = "props";
	public static final String PROP_ELEMENT = "prop";

	public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";

	public static final String DEPENDENCY_CHECK_ATTRIBUTE = "dependency-check";
	public static final String DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE = "all";
	public static final String DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE = "simple";
	public static final String DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE = "objects";

	public static final String AUTOWIRE_ATTRIBUTE = "autowire";
	public static final String AUTOWIRE_BY_NAME_VALUE = "byName";
	public static final String AUTOWIRE_BY_TYPE_VALUE = "byType";
	public static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";
	public static final String AUTOWIRE_AUTODETECT_VALUE = "autodetect";

	protected final Log logger = LogFactory.getLog(getClass());

	private BeanDefinitionRegistry beanFactory;

	private ClassLoader beanClassLoader;

	private Resource resource;

	private String defaultLazyInit;

	private String defaultDependencyCheck;

	private String defaultAutowire;

	public void registerBeanDefinitions(BeanDefinitionRegistry beanFactory, ClassLoader beanClassLoader, Document doc,
			Resource resource) {
		this.beanFactory = beanFactory;
		this.beanClassLoader = beanClassLoader;
		this.resource = resource;

		logger.debug("Loading bean definitions");
		Element root = doc.getDocumentElement();
		
		//xml 최상이 노드 속성 값 가져오기
		this.defaultLazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
		logger.debug("Default lazy init '" + this.defaultLazyInit + "'");
		this.defaultDependencyCheck = root.getAttribute(DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE);
		logger.debug("Default dependency check '" + this.defaultDependencyCheck + "'");
		this.defaultAutowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
		logger.debug("Default autowire '" + this.defaultAutowire + "'");

		NodeList nl = root.getChildNodes();
		int beanDefinitionCounter = 0;
		//자식중 bean 찾기
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && BEAN_ELEMENT.equals(node.getNodeName())) {
				beanDefinitionCounter++;
				loadBeanDefinition((Element) node);
			}
		}
		logger.debug("Found " + beanDefinitionCounter + " <" + BEAN_ELEMENT + "> elements defining beans");
	}
	
	protected BeanDefinitionRegistry getBeanFactory() {
		return beanFactory;
	}

	protected ClassLoader getBeanClassLoader() {
		return beanClassLoader;
	}

	protected String getDefaultLazyInit() {
		return defaultLazyInit;
	}

	protected String getDefaultDependencyCheck() {
		return defaultDependencyCheck;
	}

	protected String getDefaultAutowire() {
		return defaultAutowire;
	}
	
	protected void loadBeanDefinition(Element ele) {
		//bean id
		String id = ele.getAttribute(ID_ATTRIBUTE);
		//bean name
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		List aliases = new ArrayList();
		if (nameAttr != null && !"".equals(nameAttr)) {
			String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, BEAN_NAME_DELIMITERS, true, true);
			aliases.addAll(Arrays.asList(nameArr));
		}

		if (id == null || "".equals(id) && !aliases.isEmpty()) {
			id = (String) aliases.remove(0);
			logger.debug("No XML 'id' specified - using '" + id + "' as ID and " + aliases + " as aliases");
		}

		AbstractBeanDefinition beanDefinition = parseBeanDefinition(ele, id);

		if (id == null || "".equals(id)) {
			if (beanDefinition instanceof RootBeanDefinition) {
				id = ((RootBeanDefinition) beanDefinition).getBeanClassName();
				logger.debug("Neither XML 'id' nor 'name' specified - using bean class name [" + id + "] as ID");
			}
			else {
				throw new BeanDefinitionStoreException(this.resource, "",
																							 "Child bean definition has neither 'id' nor 'name'");
			}
		}

		logger.debug("Registering bean definition with id '" + id + "'");
		this.beanFactory.registerBeanDefinition(id, beanDefinition);
		for (Iterator it = aliases.iterator(); it.hasNext();) {
			this.beanFactory.registerAlias(id, (String) it.next());
		}
	}

}
