package org.springframework.beans.factory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.BeansException;

public abstract class BeanFactoryUtils {

	
	public static Map beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class type,boolean includePrototypes, boolean includeFactoryBeans)
		    throws BeansException {
			Map result = new HashMap();
			result.putAll(lbf.getBeansOfType(type, includePrototypes, includeFactoryBeans));
			//계층 구조 부모 객체가 있으면 부모객체도 재귀호출해서 맵에 추가
			if (lbf instanceof HierarchicalBeanFactory) {
				HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
				if (hbf.getParentBeanFactory() != null && hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
					Map parentResult = beansOfTypeIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(),
																													 type, includePrototypes, includeFactoryBeans);
					for (Iterator it = parentResult.keySet().iterator(); it.hasNext();) {
						Object key = it.next();
						if (!result.containsKey(key)) {
							result.put(key, parentResult.get((key)));
						}
					}
				}
			}
			return result;
		}
	
	
}
