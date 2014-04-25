package norfenstein.util;

public final class Convert {
	private Convert() { }

	public static int toInt(Object stringObject, int defaultValue) {
		if (stringObject instanceof String) {
			return toInt((String)stringObject, defaultValue);
		} else {
			return defaultValue;
		}
	}
	public static int toInt(String stringObject, int defaultValue) {
		try {
			return Integer.parseInt(stringObject);
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}

	public static Integer toInt(Object stringObject) {
		if (stringObject instanceof String) {
			return toInt((String)stringObject);
		} else {
			return null;
		}
	}
	public static Integer toInt(String stringObject) {
		try {
			return Integer.valueOf(stringObject);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	public static float toFloat(Object stringObject, float defaultValue) {
		if (stringObject instanceof String) {
			return toFloat((String)stringObject, defaultValue);
		} else {
			return defaultValue;
		}
	}
	public static float toFloat(String stringObject, float defaultValue) {
		try {
			return Float.parseFloat(stringObject);
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}

	public static Float toFloat(Object stringObject) {
		if (stringObject instanceof String) {
			return toFloat((String)stringObject);
		} else {
			return null;
		}
	}
	public static Float toFloat(String stringObject) {
		try {
			return Float.valueOf(stringObject);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}
}

