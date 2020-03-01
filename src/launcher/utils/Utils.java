package launcher.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
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
}
