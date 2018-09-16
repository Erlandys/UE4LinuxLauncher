package launcher.objects;

public class Cookie {
	private String _name, _value;

	public Cookie(String name, String value)
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

	@Override
	public String toString() {
		return _name + "=" + _value + "; ";
	}
}
