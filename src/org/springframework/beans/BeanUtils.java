package org.springframework.beans;

public abstract class BeanUtils {
	
	//객체 생성
	public static Object instantiateClass(Class clazz) throws BeansException {
		try {
			return clazz.newInstance();
		}
		catch (InstantiationException ex) {
			throw new FatalBeanException("Could not instantiate class [" + clazz.getName() +
																	 "]; Is it an interface or an abstract class? Does it have a no-arg constructor?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new FatalBeanException("Could not instantiate class [" + clazz.getName() +
																	 "]; has class definition changed? Is there a public no-arg constructor?", ex);
		}
	}
	
	public static boolean isSimpleProperty(Class clazz) {
		return clazz.isPrimitive() || isPrimitiveArray(clazz) || isPrimitiveWrapperArray(clazz) ||
		    clazz.equals(String.class) || clazz.equals(String[].class) ||
		    clazz.equals(Class.class) || clazz.equals(Class[].class);
	}
	
	//기본타입 배열 검사
	public static boolean isPrimitiveArray(Class clazz) {
		return boolean[].class.equals(clazz) || byte[].class.equals(clazz) || char[].class.equals(clazz) ||
		    short[].class.equals(clazz) || int[].class.equals(clazz) || long[].class.equals(clazz) ||
		    float[].class.equals(clazz) || double[].class.equals(clazz);
	}
	
	//기본 타입 래퍼클레스 배열 검사
	public static boolean isPrimitiveWrapperArray(Class clazz) {
		return Boolean[].class.equals(clazz) || Byte[].class.equals(clazz) || Character[].class.equals(clazz) ||
		    Short[].class.equals(clazz) || Integer[].class.equals(clazz) || Long[].class.equals(clazz) ||
		    Float[].class.equals(clazz) || Double[].class.equals(clazz);
	}
	
	//같은 타입이지 검사
	public static boolean isAssignable(Class type, Object value) {
		return (type.isInstance(value) ||
		    (!type.isPrimitive() && value == null) ||
		    (type.equals(boolean.class) && value instanceof Boolean) ||
		    (type.equals(byte.class) && value instanceof Byte) ||
		    (type.equals(char.class) && value instanceof Character) ||
		    (type.equals(short.class) && value instanceof Short) ||
		    (type.equals(int.class) && value instanceof Integer) ||
		    (type.equals(long.class) && value instanceof Long) ||
		    (type.equals(float.class) && value instanceof Float) ||
		    (type.equals(double.class) && value instanceof Double));
	}
	
}
