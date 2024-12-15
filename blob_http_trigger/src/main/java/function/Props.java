package function;

public class Props {

	public static String get(String key){
		return get(key, "key_not_found");
	}

	public static String get(String key, String defaultValue) {
		var val = System.getProperty(key);
		return val == null ? defaultValue : val;
	}

	public static <T> T get(String key, Class<T> clazz) {
		var val = System.getProperty(key);
		if( val == null )
			return null;
		return JSON.decode(val, clazz);
	}

	public static void load() {
		try {
			System.getenv().forEach( System::setProperty );
		}
		catch( Exception x  ) {
			x.printStackTrace();
		}
	}
}
