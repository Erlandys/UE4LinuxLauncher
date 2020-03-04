package launcher.objects;

import launcher.managers.SessionManager;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Request {
	private static String USER_AGENT = "game=UELauncher, engine=UE4, build=7.14.2-4231683+++Portal+Release-Live";

	private static class Input {
		private String _name;
		private Object _value;

		Input(String name, Object value) {
			_name = name;
			_value = value;
		}

		String getName() {
			return _name;
		}

		Object getValue() {
			return _value;
		}

		@Override
		public String toString() {
			String value = _value.toString();
			if (_name.equalsIgnoreCase("email"))
				value = "******";
			else if (_name.equalsIgnoreCase("password"))
				value = "******";
			return "Input{" +
					"_name='" + _name + '\'' +
					", _value=" + value +
					'}';
		}
	}

	private enum ERequestType {
		GET,
		POST
	}

	private enum EPostContentType {
		Form,
		Json
	}

	private String _url;
	private boolean _followRedirects;
	private boolean _readOutput;

	private ERequestType _requestType;
	private EPostContentType _postContentType;

	private List<Input> _sentInputs;
	private List<AbstractMap.SimpleEntry<String, String>> _parameters;
	private List<AbstractMap.SimpleEntry<String, String>> _sentHeaders;
	private List<AbstractMap.SimpleEntry<String, String>> _sentCookies;

	private Map<String, List<String>> _receivedHeaders;
	private Map<String, AbstractMap.SimpleEntry<String, Integer>> _receivedCookies;

	private String _content;
	private String _errorContent;
	private int _responseCode = 0;

	private URL _urlObject;
	private HttpsURLConnection _connection;

	private int _expectedCode;

	private String _receivedXSRF;

	private boolean _addUserAgent;

	public Request(String url) throws IOException {
		_url = url;
		_urlObject = new URL(url);
		_connection = (HttpsURLConnection) _urlObject.openConnection();
		_connection.disconnect();
		initialize();
	}

	private void initialize() {
		_addUserAgent = true;
		_sentInputs = new LinkedList<>();
		_sentHeaders = new LinkedList<>();
		_sentCookies = new LinkedList<>();
		_receivedHeaders = new HashMap<>();
		_receivedCookies = new HashMap<>();
		_parameters = new LinkedList<>();
		_requestType = ERequestType.GET;
		_postContentType = EPostContentType.Form;
		_connection.setDoOutput(true);
		_readOutput = true;
		_connection.setInstanceFollowRedirects(false);
		_followRedirects = false;
	}

	public boolean execute() {
		return execute(-1);
	}

	public boolean execute(int expectedCode) {
		_expectedCode = expectedCode;
		try {
			prepareData();
			_responseCode = _connection.getResponseCode();
			if (_readOutput)
				_content = readOutput(true);
			_errorContent = readOutput(false);
			parseHeaders();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return _expectedCode == -1 || _expectedCode == _responseCode;
	}

	private void parseHeaders() {
		for (Map.Entry<String, List<String>> header : _connection.getHeaderFields().entrySet()) {
			if (header.getKey() == null)
				continue;
			if (header.getKey().equalsIgnoreCase("Set-Cookie")) {
				for (String headerValue : header.getValue()) {
					String[] headerParts = headerValue.split(";");
					String cookie = headerValue.contains(";") ? headerParts[0] : headerValue;
					String key;
					String value;
					if (cookie.contains("=")) {
						String[] parts = cookie.split("=");
						key = parts[0].trim();
						value = parts[1].trim();
					} else {
						key = cookie.trim();
						value = "";
					}

					int expireDate = 0;
					for (String part : headerParts) {
						if (!part.contains("="))
							continue;
						part = part.trim();
						String[] partParts = part.split("=");
						if (!partParts[0].equalsIgnoreCase("expires"))
							continue;
						try {
							expireDate = (int) (new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").parse(partParts[1]).getTime() / 1000);
						} catch (ParseException e) {
						}
					}

					_receivedCookies.put(key, new AbstractMap.SimpleEntry<>(value, expireDate));
					if (key.equalsIgnoreCase("XSRF-TOKEN")) {
						_receivedXSRF = value;
					}
				}
			} else {
				_receivedHeaders.put(header.getKey(), header.getValue());
			}
		}
	}

	public void removeUserAgent() {
		_addUserAgent = false;
	}

	private String readOutput(boolean output) {
		StringBuilder content = new StringBuilder();
		try {
			InputStream stream;
			if (output)
				stream = _connection.getInputStream();
			else
				stream = _connection.getErrorStream();
			if (stream == null)
				return "";
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String line;

			while ((line = in.readLine()) != null)
				content.append(line).append("\n");
		} catch (Exception ignored) {
		}

		return content.toString();
	}

	private void prepareData() throws IOException {
		String url = fillUrl();
		_urlObject = new URL(url);
		_connection = (HttpsURLConnection) _urlObject.openConnection();
		_connection.disconnect();
		byte[] postData = null;
		_connection.setDoOutput(_readOutput);
		_connection.setInstanceFollowRedirects(_followRedirects);
		if (_addUserAgent) {
			_sentHeaders.add(new AbstractMap.SimpleEntry<>("User-Agent", USER_AGENT));
			_sentHeaders.add(new AbstractMap.SimpleEntry<>("Origin", "erlandys_ue4_marketplace"));
		}
		if (_requestType == ERequestType.POST) {
			_connection.setDoInput(true);
			postData = getInputs().getBytes();
			switch (_postContentType) {
				case Form:
					_sentHeaders.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/x-www-form-urlencoded"));
					break;
				case Json:
					_sentHeaders.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
					break;
			}
			_connection.setDoInput(true);
			_sentHeaders.add(new AbstractMap.SimpleEntry<>("charset", "utf-8"));
			_sentHeaders.add(new AbstractMap.SimpleEntry<>("Content-Length", String.valueOf(postData.length)));
			_connection.setRequestMethod("POST");
		}
		for (AbstractMap.SimpleEntry<String, String> entry : _sentHeaders) {
			_connection.setRequestProperty(entry.getKey(), entry.getValue());
		}
		_connection.setRequestProperty("Cookie", getSendableCookies());

		if (_requestType == ERequestType.POST) {
			try (DataOutputStream writer = new DataOutputStream(_connection.getOutputStream())) {
				if (postData != null)
					writer.write(postData);
			}
		} else {
			_connection.connect();
		}
	}

	private String fillUrl() {
		StringBuilder result = new StringBuilder(_url);
		if (_parameters.isEmpty())
			return result.toString();
		boolean firstInput = !_url.contains("?");
		for (AbstractMap.SimpleEntry<String, String> entry : _parameters) {
			result.append(firstInput ? "?" : "&");
			result.append(entry.getKey()).append("=").append(entry.getValue());
			firstInput = false;
		}
		return result.toString();
	}

	public Request postRequest() {
		_requestType = ERequestType.POST;
		return this;
	}

	public Request jsonContentType() {
		_postContentType = EPostContentType.Json;
		return this;
	}

	public Request followRedirects() {
		_connection.setInstanceFollowRedirects(true);
		_followRedirects = true;
		return this;
	}

	public Request doNotReadOutput() {
		_connection.setDoOutput(false);
		_readOutput = false;
		return this;
	}

	public Request assignInput(String name, Object value) throws UnsupportedEncodingException {
		_sentInputs.add(new Input(name, value));
		return this;
	}

	public Request assignParameter(String name, Object value) throws UnsupportedEncodingException {
		name = URLEncoder.encode(name, "UTF-8");
		String valueString = URLEncoder.encode(value instanceof String ? (String) value : value.toString(), "UTF-8");
		_parameters.add(new AbstractMap.SimpleEntry<>(name, valueString));
		return this;
	}

	public Request assignHeader(String header, String value) throws UnsupportedEncodingException {
		_sentHeaders.add(new AbstractMap.SimpleEntry<>(URLEncoder.encode(header, "UTF-8"), value));
		return this;
	}

	public Request assignCookie(String header, String value) throws UnsupportedEncodingException {
		_sentCookies.add(new AbstractMap.SimpleEntry<>(URLEncoder.encode(header, "UTF-8"), URLEncoder.encode(value, "UTF-8")));
		return this;
	}

	public Request assignCookies(Session session) {
		_sentCookies.addAll(session.getCookies());
		return this;
	}

	public Request assignXSRF(String xsrf) throws UnsupportedEncodingException {
		if (xsrf == null)
			return this;
		if (xsrf.isEmpty())
			return this;
		_sentHeaders.add(new AbstractMap.SimpleEntry<>(URLEncoder.encode("X-XSRF-TOKEN", "UTF-8"), URLEncoder.encode(xsrf, "UTF-8")));
		return this;
	}

	public Request assignBearer() throws UnsupportedEncodingException {
		_sentHeaders.add(new AbstractMap.SimpleEntry<>(URLEncoder.encode("Authorization", "UTF-8"), "bearer " + SessionManager.getInstance().getUser().getOAuthAccessToken()));
		return this;
	}

	private String getSendableCookies() {
		StringBuilder result = new StringBuilder();
		for (AbstractMap.SimpleEntry<String, String> cookie : _sentCookies) {
			if (result.length() > 0)
				result.append("; ");
			result.append(cookie.getKey()).append("=").append(cookie.getValue());
		}
		return result.toString();
	}

	private String getInputs() throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		if (_postContentType == EPostContentType.Form) {
			for (Input input : _sentInputs) {
				if (result.length() > 0)
					result.append("&");
				if (input.getValue() instanceof String)
					result.append(input.getName()).append("=").append(input.getValue());
				else
					result.append(input.getName()).append("=").append(input.getValue());
			}
		} else {
			JSONObject obj = new JSONObject();
			for (Input input : _sentInputs)
				obj.put(input.getName(), input.getValue());
			result = new StringBuilder(obj.toString());
		}
		return result.toString();
	}

	public String getXSRF() {
		return _receivedXSRF;
	}

	public String getContent() {
		if (_content.isEmpty())
			return _errorContent;
		return _content;
	}

	public int getResponseCode() {
		return _responseCode;
	}

	public Map<String, List<String>> getHeaders() {
		return _receivedHeaders;
	}

	public Map<String, AbstractMap.SimpleEntry<String, Integer>> getCookies() {
		return _receivedCookies;
	}

	public String getCookie(String cookieName) {
		if (_receivedCookies.containsKey(cookieName))
			return _receivedCookies.get(cookieName).getKey();
		return "";
	}

	public List<String> getHeaders(String header) {
		if (_receivedHeaders.containsKey(header))
			return _receivedHeaders.get(header);
		return new ArrayList<>();
	}

	public String getHeader(String header) {
		if (_receivedHeaders.containsKey(header)) {
			if (_receivedHeaders.get(header).isEmpty())
				return "";
			return _receivedHeaders.get(header).get(0);
		}
		return "";
	}

	@Override
	public String toString() {
		StringBuilder message = new StringBuilder("Request {\n" +
				"\tURL = '" + _url + "',\n" +
				"\tFollow Redirects = " + _followRedirects + ",\n" +
				"\tExpected code = " + _expectedCode + ",\n" +
				"\tRead Outputs = " + _readOutput + ",\n" +
				"\tSent Headers = " + _sentHeaders + ",\n" +
				"\tSent Cookies Serialized = " + _sentCookies + ",\n" +
				"\tSent Cookies Raw = '" + getSendableCookies() + "',\n" +
				"\tRequest Type = " + (_requestType == ERequestType.GET ? "GET" : "POST") + ",\n");

		message.append("\tURL Parameters = [\n");
		for (Map.Entry<String, String> entry : _parameters) {
			message.append("\t\t").append(entry.getKey()).append(" = '").append(entry.getValue()).append("',\n");
		}
		message.append("\t],\n");
		message.append("\tFull URL = ").append(fillUrl()).append("\n");
		if (_requestType == ERequestType.POST) {
			message.append("\tSent Inputs = ").append(_sentInputs).append(",\n");
			message.append("\tContent type = ").append(_postContentType == EPostContentType.Form ? "Form" : "Json").append(",\n");
			try {
				message.append("\tSent Inputs Raw = ").append(getInputs()).append(",\n");
			} catch (Exception ignored) {
			}
		}

		message.append("\n");
		message.append("\tReceived Headers = [\n");
		for (Map.Entry<String, List<String>> entry : _receivedHeaders.entrySet()) {
			message.append("\t\t").append(entry.getKey()).append(" = [,\n");
			for (String value : entry.getValue())
				message.append("\t\t\t").append(value).append(",\n");
			message.append("\t\t],\n");
		}
		message.append("\t],\n\n");
		message.append("\tReceived XSRF = ").append(_receivedXSRF).append(",\n\n").append("\tReceived Cookies = [\n");
		for (Map.Entry<String, AbstractMap.SimpleEntry<String, Integer>> entry : _receivedCookies.entrySet()) {
			message.append("\t\t").append(entry.getKey()).append(" = '").append(entry.getValue().getKey());
			if (entry.getValue().getValue() > 0) {
				message.append("' [Expires at ").append((new Date(entry.getValue().getValue())).toString()).append("],\n");
			} else {
				message.append("',\n");
			}
		}
		message.append("\t],\n\n" + "\tResponse Code = ").append(_responseCode).append(",\n").append("\tReceived Error Content = '").append(_errorContent).append("',\n").append("\tReceived Content = '").append(_content).append("',\n").append('}');
		return message.toString();
	}
}
