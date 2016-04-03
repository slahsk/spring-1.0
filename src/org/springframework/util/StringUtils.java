package org.springframework.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class StringUtils {
	
	public static String arrayToCommaDelimitedString(Object[] arr) {
		return arrayToDelimitedString(arr, ",");
	}
	//join 이랑 비슷
	public static String arrayToDelimitedString(Object[] arr, String delim) {
		if (arr == null) {
			return "null";
		}
		else {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < arr.length; i++) {
				if (i > 0)
					sb.append(delim);
				sb.append(arr[i]);
			}
			return sb.toString();
		}
	}
	
	
	
	public static String[] commaDelimitedListToStringArray(String s) {
		return delimitedListToStringArray(s, ",");
	}
	
	//문자 구분자 잆는거 가지고 배열료  split??
	public static String[] delimitedListToStringArray(String s, String delim) {
		if (s == null) {
			return new String[0];
		}
		if (delim == null) {
			return new String[]{s};
		}

		List l = new LinkedList();
		int pos = 0;
		int delPos = 0;
		while ((delPos = s.indexOf(delim, pos)) != -1) {
			l.add(s.substring(pos, delPos));
			pos = delPos + delim.length();
		}
		if (pos <= s.length()) {
			l.add(s.substring(pos));
		}

		return (String[]) l.toArray(new String[l.size()]);
	}
	
	public static String collectionToCommaDelimitedString(Collection c) {
		return collectionToDelimitedString(c, ",");
	}
	
	//Collection 객체 , 구분해서 하나의 스트링으로 변화 시키기
	public static String collectionToDelimitedString(Collection c, String delim) {
		if (c == null) {
			return "null";
		}
		StringBuffer sb = new StringBuffer();
		Iterator itr = c.iterator();
		int i = 0;
		while (itr.hasNext()) {
			if (i++ > 0) {
				sb.append(delim);
			}
			sb.append(itr.next());
		}
		return sb.toString();
	}
}
