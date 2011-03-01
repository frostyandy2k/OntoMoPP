package org.emftext.languages.petrinets.lib;

public class StandardlibSemanticsLibrary {

	public static int add(int arg1, int arg2) {
		return arg1 + arg2;
	}

	public static int sub(int arg1, int arg2) {
		return arg1 - arg2;
	}
	
	public static int mult(int arg1, int arg2) {
		return arg1 * arg2;
	}
	
	public static int div(int arg1, int arg2) {
		return arg1 / arg2;
	}
	
	public static int power(int arg1, int arg2) {
		Double result = new Double(java.lang.Math.pow(arg1, arg2));
		return result.intValue();
	}
	
	public static String append(String arg1, String arg2) {
		return arg1.concat(arg2);
	}

	public static boolean and(boolean arg1, boolean arg2) {
		return  arg1 && arg2;
	}
	
	public static boolean or(boolean arg1, boolean arg2) {
		return  arg1 || arg2;
	}
	
	public static boolean not(boolean arg1) {
		return ! arg1;
	}
	
	public static boolean isNull(Object arg1) {
		return arg1 == null;
	}
	
	public static boolean isNotNull(Object arg1) {
		return ! (arg1 == null);
	}
}
