package launcher.utils;

import launcher.Main;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {
	private static SimpleDateFormat HMS_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
	public static String getContent(HttpURLConnection connection, boolean inputStream) throws IOException
	{
		StringBuilder content = new StringBuilder();
		InputStream stream;
		if (inputStream)
			stream = connection.getInputStream();
		else
			stream = connection.getErrorStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		String line;

		while ((line = in.readLine()) != null)
			content.append(line).append("\n");

		return content.toString();
	}


	public static void writeData(HttpURLConnection connection, String content) throws IOException
	{
		connection.setDoOutput(true);
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		byte postData[] = content.getBytes(StandardCharsets.UTF_8);

		connection.setRequestProperty("Content-Length", String.valueOf(postData.length));
		connection.setRequestMethod("POST");
		try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
			writer.write(postData);
		}
	}

	public static void printError(String title, String text) {
		if (!Main.DEBUG)
			return;
		String time = getTime();
		String error = time + ": " + getSection("Error");
		error += time + ": " + getSection(getStackTrace());
		error += time + ": " + getSection(title);
		error += time + ": " + text + "\n";
		error += time + ": " + "# ======================================================================================================\n";
		System.out.print(error);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("log.txt", true))) {
			writer.write(error);
		}
		catch (IOException ignored) {}
	}

	public static void printError(String title, int text) {
		printError(title, String.valueOf(text));
	}

	public static void printDebug(String title, String text) {
		if (!Main.DEBUG)
			return;
		String time = getTime();
		String debug = time + ": " + getSection("Debug");
		debug += time + ": " + getSection(getStackTrace());
		debug += time + ": " + getSection(title);
		debug += time + ": " + text + "\n";
		debug += time + ": " + "# ====================================================================================================== #\n";
		System.out.print(debug);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("log.txt", true))) {
			writer.write(debug);
		}
		catch (IOException ignored) {}
	}

	public static void printDebug(String title, int text) {
		printDebug(title, String.valueOf(text));
	}

	public static void printOutput(String url, String content)
	{
		if (!Main.DEBUG)
			return;
		File file = new File("output");
		if (!file.exists())
			file.mkdirs();
		String caller = "";
		caller = Thread.currentThread().getStackTrace()[2].getMethodName();
		if (caller.equalsIgnoreCase("addInputs"))
			caller = Thread.currentThread().getStackTrace()[3].getMethodName();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("output/" + caller + ".htm", true))) {
			writer.write(url + "\n\n");
			writer.write(content);
		}
		catch (Exception ignored) {}
	}

	private static String getStackTrace()
	{
		StringBuilder trace = new StringBuilder();
		for (int i = Thread.currentThread().getStackTrace().length - 1; i > 2; i--) {
			trace.append(Thread.currentThread().getStackTrace()[i].getMethodName());
			if (i > 3)
				trace.append(" -> ");
		}

		return trace.toString();
	}

	private static String getSection(String text)
	{
		StringBuilder sectionText = new StringBuilder("# ");
		int partLength = (100 - text.length()) / 2;
		int fill = (100 - text.length()) % 2;
		for (int i = 0; i < partLength + fill; i++)
			sectionText.append("=");
		sectionText.append(" ").append(text).append(" ");
		for (int i = 0; i < partLength; i++)
			sectionText.append("=");
		sectionText.append(" #\n");
		return sectionText.toString();
	}

	private static String getTime()
	{
		Date date = new Date(System.currentTimeMillis());
		return HMS_FORMATTER.format(date);
	}
}
