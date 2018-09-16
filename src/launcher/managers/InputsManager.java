package launcher.managers;

import launcher.Main;
import launcher.objects.Input;
import launcher.utils.Utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputsManager {
	private static String NAME_VALUE_PATTERN = "(.*?)name=\"(.*?)\"(.*?)value=\"(.*?)\"(.*)";
	private static String NAME_PATTERN = "(.*?)<input(.*?)name=\"(.*?)\"(.*?)>(.*)";
	private static String VALUE_NAME_PATTERN = "(.*?)value=\"(.*?)\"(.*?)name=\"(.*?)\"(.*)";
	private static String FULL_INPUT_PATTERN = "(.*?)<input(.*?)>(.*)";
	private static String PART_OF_INPUT_PATTERN = "(.*?)<input(.*?)";
	private static String INPUT_END_PATTERN = "(.*?)>(.*)";

	private HashMap<String, Input> _inputs;
	private Pattern _nameValue, _valueName, _name;

	public InputsManager()
	{
		_inputs = new HashMap<>();
		_nameValue = Pattern.compile("(.*?)name=\"(.*?)\"(.*?)value=\"(.*?)\"(.*)");
		_valueName = Pattern.compile("(.*?)value=\"(.*?)\"(.*?)name=\"(.*?)\"(.*)");
		_name = Pattern.compile("(.*?)<input(.*?)name=\"(.*?)\"(.*?)>(.*)");
	}

	public void addInput(String name, String value)
	{
		_inputs.put(name.toLowerCase(), new Input(name, value));
	}

	public void addInputs(String content)
	{
		StringBuilder input = null;
		boolean inputNotFinished = false;
		for (String line : content.split("\n"))
		{
			if (inputNotFinished)
			{
				input.append(line);
				if (line.matches(INPUT_END_PATTERN))
				{
					parseInput(input.toString());
					input = null;
					inputNotFinished = false;
				}
			}
			else {
				if (line.matches(FULL_INPUT_PATTERN))
					parseInput(line);
				else if (line.matches(PART_OF_INPUT_PATTERN)) {
					input = new StringBuilder(line);
					inputNotFinished = true;
				}
			}
		}
	}

	private void parseInput(String input)
	{
		input = input.replaceAll("\n", "");
		input = input.replaceAll("\t", "");
		input = input.replaceAll("^( *)", "");
		input = input.replaceAll("( *)$", "");
		input = input.replaceAll("( {2,})", " ");
		if (input.matches(NAME_VALUE_PATTERN))
		{
			Matcher matcher = _nameValue.matcher(input);
			if (matcher.find())
			{
				String name = matcher.group(2);
				String value = matcher.group(4);
				_inputs.put(name.toLowerCase(), new Input(name, value));
			}
		}
		else if (input.matches(VALUE_NAME_PATTERN))
		{
			Matcher matcher = _valueName.matcher(input);
			if (matcher.find())
			{
				String name = matcher.group(4);
				String value = matcher.group(2);
				_inputs.put(name.toLowerCase(), new Input(name, value));
			}
		}
		else if (input.matches(NAME_PATTERN))
		{
			Matcher matcher = _name.matcher(input);
			if (matcher.find())
			{
				String name = matcher.group(3);
				_inputs.put(name.toLowerCase(), new Input(name, ""));
			}
		}
	}

	public String addInputs(HttpURLConnection connection, boolean inputStream) throws IOException
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
		addInputs(content.toString());
		try {
			Utils.printOutput(connection.getURL().toURI().getPath(), content.toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (Main.DEBUG) {
			StringBuilder result = new StringBuilder();
			_inputs.values().forEach(input -> {
				result.append(input.toString()).append("\n");
			});
			Utils.printDebug("Add inputs", result.toString());
		}
		return content.toString();
	}

	public String getAllInputs()
	{
		StringBuilder result = new StringBuilder();
		_inputs.values().forEach(input -> {
			try {
				if (!result.toString().isEmpty())
					result.append("&");
				result.append(input.getPostData());
			}
			catch (UnsupportedEncodingException ignored) {}
		});
		return result.toString();
	}

	public boolean hasInput(String name)
	{
		return _inputs.containsKey(name.toLowerCase());
	}

	public void writeData(HttpURLConnection connection) throws IOException
	{
		String data = getAllInputs();
		byte postData[] = data.getBytes(StandardCharsets.UTF_8);

		connection.setDoOutput( true );
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length", String.valueOf(postData.length));
		connection.setRequestMethod("POST");
		try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
			writer.write(postData);
		}

		if (Main.DEBUG) {
			StringBuilder result = new StringBuilder();
			_inputs.values().forEach(input -> {
				result.append(input.toString()).append("\n");
			});
			Utils.printDebug("Input data from", result.toString());
		}
	}

	public void clearInputs()
	{
		_inputs.clear();
	}

	public static InputsManager getInstance() {
		return SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final InputsManager _instance = new InputsManager();
	}
}
