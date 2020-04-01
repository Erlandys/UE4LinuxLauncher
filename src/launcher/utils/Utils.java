package launcher.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

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

		Class clazz = null;
		try {
			clazz = Class.forName("launcher.Main");
		} catch(Exception ex){
			ex.printStackTrace();
			return "";
		}

		if(clazz != null) try {
			for (String line : readResourceLocation(clazz, path))
				data += line;
		} catch (NullPointerException ex){
			ex.printStackTrace();
			return "";
		}

		if(data.length() > 0)
			resourceCache.put(path, data);

		return data;
	}


	public static ArrayList<String> readResourceLocation(Class modClazz, String location){
		InputStream is = null;
		try {
			is = modClazz.getClassLoader().getResourceAsStream(location);
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF8")));
			final ArrayList<String> lines = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			return lines;
		} catch (Throwable ignored) {
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignored) {
				}
			}
		}
	}
}
