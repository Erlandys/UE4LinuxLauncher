package launcher.utils;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class Utils {
	public static String randomString(int length) {
		if (length < 1)
			return "";
		StringBuilder result = new StringBuilder();
		Random rnd = new Random();
		byte[] array = new byte[Math.max((int) Math.ceil(length / 2.0), 1)];
		rnd.nextBytes(array);
		for (byte b : array)
			result.append(String.format("%02X", b));
		if (length % 2 == 1)
			result = new StringBuilder(result.substring(0, result.length() - 1));
		return result.toString();
	}

	private static SimpleDateFormat HMS_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

	private static String getTime() {
		Date date = new Date(System.currentTimeMillis());
		return HMS_FORMATTER.format(date);
	}

	// cached resource loader, used to parse HTML files
	static HashMap<String, String> resourceCache = new HashMap<>();
	public static String getResource(String path){

		if(resourceCache.containsKey(path))
			return resourceCache.get(path);

		String data = "";
		File file = new File(Utils.class.getClassLoader().getResource(path).getFile());

		if(file.exists()){
			try {
				for(String line : Files.readAllLines(file.toPath()))
					data+=line;

			} catch (Exception ex){
				System.out.println("failed to load html resource");
			}

			resourceCache.put(path, data);
		}

		return data;

	}
}
