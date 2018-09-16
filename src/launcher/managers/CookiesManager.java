package launcher.managers;

import launcher.objects.Cookie;
import launcher.utils.Utils;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CookiesManager {
	private HashMap<String, Cookie> _cookies;

	public CookiesManager()
	{
		_cookies = new HashMap<>();
	}

	public void addCookie(String name, String value)
	{
		_cookies.put(name.toLowerCase(), new Cookie(name, value));
	}

	public void addCookies(List<String> data)
	{
		data.forEach(cookieData -> Arrays.stream(cookieData.split(";")).forEach(this::parseCookie));
	}

	public void addCookies(String content)
	{
		Arrays.stream(content.split(";")).forEach(this::parseCookie);
	}

	public void addCookies(HttpURLConnection connection)
	{
		if (connection == null)
			return;
		if (!connection.getHeaderFields().containsKey("Set-Cookie"))
			return;
		addCookies(connection.getHeaderFields().get("Set-Cookie"));
	}

	private void parseCookie(String cookie)
	{
		String nameValue[] = cookie.split("=");
		if (nameValue.length < 2)
			return;
		nameValue[0] = nameValue[0].trim();
/*		switch (nameValue[0].toLowerCase())
		{
			case "path":
				return;
			case "domain":
				return;
			case "expires":
				return;
			case "secure":
				return;
			case "httponly":
				return;
			case "max-age":
				return;
			case "version":
				return;
		}*/
		_cookies.put(nameValue[0].toLowerCase(), new Cookie(nameValue[0], nameValue[1]));
	}

	public void clearCookies()
	{
		_cookies.clear();
	}

	public String getCookies(String ... names)
	{
		StringBuilder result = new StringBuilder();
		for (String name : names) {
			if (_cookies.containsKey(name.toLowerCase()))
				result.append(_cookies.get(name.toLowerCase()));
		}
		Utils.printDebug("Cookies data", result.toString().replaceAll(";", "\n"));
		return result.toString();
	}

	public String getAllCookies()
	{
		StringBuilder result = new StringBuilder();
		_cookies.values().forEach(result::append);

		Utils.printDebug("Cookies data", result.toString().replaceAll(";", "\n"));
		return result.toString();
	}

	public String getCookie(String name)
	{
		if (_cookies.containsKey(name.toLowerCase()))
			return _cookies.get(name.toLowerCase()).toString();
		return "";
	}

	public String getCookieValue(String name)
	{
		if (_cookies.containsKey(name.toLowerCase()))
			return _cookies.get(name.toLowerCase()).getValue();
		return "";
	}

	public static CookiesManager getInstance() {
		return SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final CookiesManager _instance = new CookiesManager();
	}
}
