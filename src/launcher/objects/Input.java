package launcher.objects;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Input {
	private String _name, _value;

	public Input(String name, String value)
	{
		_name = name;
		_value = value;
	}

	public String getName() {
		return _name;
	}

	public String getValue() {
		return _value;
	}

	public String getPostData() throws UnsupportedEncodingException
	{
		return URLEncoder.encode(_name, "UTF-8") + "=" + URLEncoder.encode(_value, "UTF-8");
	}

	@Override
	public String toString() {
		return _name + "=" + _value + ";";
	}
}
