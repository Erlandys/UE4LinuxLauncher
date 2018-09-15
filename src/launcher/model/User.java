package launcher.model;

import org.json.JSONObject;

public class User {
	private String _lastLogin;
	private String _country;
	private String _lastName;
	private String _displayName;
	private String _phoneNumber;
	private String _name;
	private String _email;
	private String _id;
	public User(JSONObject data)
	{
		parseData(data);
	}

	private void parseData(JSONObject data)
	{
		_lastLogin = data.getString("lastLogin");
		_country = data.getString("country");
		_lastName = data.getString("lastName");
		_displayName = data.getString("displayName");
		_phoneNumber = data.getString("phoneNumber");
		_name = data.getString("name");
		_email = data.getString("email");
		_id = data.getString("id");
	}

	public String getLastLogin() {
		return _lastLogin;
	}

	public String getCountry() {
		return _country;
	}

	public String getLastName() {
		return _lastName;
	}

	public String getDisplayName() {
		return _displayName;
	}

	public String getPhoneNumber() {
		return _phoneNumber;
	}

	public String getName() {
		return _name;
	}

	public String getEmail() {
		return _email;
	}

	public String getId() {
		return _id;
	}
}
