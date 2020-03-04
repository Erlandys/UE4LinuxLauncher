package launcher.objects;

import launcher.managers.DatabaseManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.*;

public class Session {
	private String _xsrf;
	private JSONArray _bda;
	private int _timeObjectIndex;
	private String _userBrowser;
	private Map<String, AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, Integer>>> _cookies;

	public Session() {
		_cookies = new HashMap<>();
		_bda = null;
		_timeObjectIndex = -1;
	}

	public String getBDA() {
		if (_bda == null)
			return null;
		if (_timeObjectIndex == -1)
			return null;
		JSONObject obj = _bda.optJSONObject(_timeObjectIndex);
		if (obj == null)
			return null;
		obj.remove("value");
		obj.put("value", Base64.getEncoder().encodeToString(Integer.toString((int) (System.currentTimeMillis() / 1000)).getBytes()));
		return Base64.getEncoder().encodeToString(_bda.toString().getBytes());
	}

	public void setBDA(String bda) {
		setBDA(bda, true, true);
	}

	private void setBDA(String bda, boolean save, boolean isBase64) {
		_timeObjectIndex = -1;
		String decodedJSON;
		if (isBase64) {
			byte[] decoded = Base64.getDecoder().decode(bda);
			if (decoded.length < 1)
				return;
			decodedJSON = new String(decoded);
		}
		else
			decodedJSON = bda;
		_bda = new JSONArray(decodedJSON);
		for (int i = 0; i < _bda.length(); i++) {
			JSONObject obj = _bda.optJSONObject(i);
			if (obj == null)
				continue;
			if (!obj.has("key"))
				continue;
			String key = obj.getString("key");
			if (!key.equalsIgnoreCase("n"))
				continue;
			_timeObjectIndex = i;
			break;
		}
		if (_timeObjectIndex != -1 && save)
			save();
	}

	public String getUserBrowser() {
		return _userBrowser;
	}

	public void setUserBrowser(String userBrowser) {
		_userBrowser = userBrowser;
	}

	public String getXSRF() {
		return _xsrf;
	}

	public void setXSRF(String xsrf) {
		_xsrf = xsrf;
	}

	public void setCookie(String key, String value) {
		if (key.equalsIgnoreCase("xsrf-token"))
			_xsrf = value;
		_cookies.put(key.toLowerCase(), new AbstractMap.SimpleEntry<>(key, new AbstractMap.SimpleEntry<>(value, 0)));
	}

	public void setCookies(Map<String, AbstractMap.SimpleEntry<String, Integer>> cookies) {
		for (Map.Entry<String, AbstractMap.SimpleEntry<String, Integer>> entry : cookies.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("xsrf-token"))
				_xsrf = entry.getValue().getKey();
			_cookies.put(entry.getKey().toLowerCase(), new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
		}
	}

	public String getCookie(String key) {
		if (!_cookies.containsKey(key.toLowerCase()))
			return "";
		return _cookies.get(key.toLowerCase()).getValue().getKey();
	}

	public Collection<AbstractMap.SimpleEntry<String, String>> getCookies() {
		List<AbstractMap.SimpleEntry<String, String>> result = new LinkedList<>();
		for (AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, Integer>> entry : _cookies.values()) {
			result.add(new AbstractMap.SimpleEntry<String, String>(entry.getKey(), entry.getValue().getKey()));
		}
		return result;
	}

	public boolean hasCookie(String name) {
		return _cookies.containsKey(name.toLowerCase());
	}

	public void save() {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			saveCookies(connection);
			saveGlobalVariables(connection);
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private void saveCookies(Connection connection) throws SQLException {
		{
			Statement statement = connection.createStatement();
			statement.executeUpdate("DELETE FROM cookies");
		}
		{
			PreparedStatement statement = connection.prepareStatement("INSERT INTO cookies(name, value, expire_at) VALUES(?, ?, ?)");
			for (AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, Integer>> cookie : _cookies.values()) {
				statement.setString(1, cookie.getKey());
				statement.setString(2, cookie.getValue().getKey());
				statement.setInt(3, cookie.getValue().getValue());
				statement.executeUpdate();
			}
		}
	}

	private void saveGlobalVariables(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("REPLACE INTO global_variables(name, value) VALUES(?, ?)");
		{
			statement.setString(1, "bda");
			statement.setString(2, _bda.toString());
			statement.executeUpdate();
		}
		{
			statement.setString(1, "user_browser");
			statement.setString(2, _userBrowser);
			statement.executeUpdate();
		}
	}

	public boolean load() {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			loadCookies(connection);
			loadGlobalVariables(connection);
			return true;
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return false;
	}

	private void loadCookies(Connection connection) throws SQLException {
		{
			PreparedStatement statement = connection.prepareStatement("DELETE FROM cookies WHERE expire_at != 0 AND expire_at < ?");
			statement.setInt(1, (int) (System.currentTimeMillis() / 1000));
			statement.executeUpdate();
		}
		{
			Statement statement = connection.createStatement();
			ResultSet rset = statement.executeQuery("SELECT * FROM cookies");
			while (rset.next()) {
				_cookies.put(rset.getString("name").toLowerCase(), new AbstractMap.SimpleEntry<>(rset.getString("name"), new AbstractMap.SimpleEntry<>(rset.getString("value"), rset.getInt("expire_at"))));
			}
		}
	}

	private void loadGlobalVariables(Connection connection) throws SQLException {
		Statement statement = connection.createStatement();
		ResultSet rset = statement.executeQuery("SELECT * FROM global_variables");
		while (rset.next()) {
			String name = rset.getString("name");
			String value = rset.getString("value");
			if (name.equals("bda"))
				setBDA(value, false, false);
			else if (name.equals("user_browser"))
				setUserBrowser(value);
		}
	}

	public boolean isExpired() {
		return !hasCookie("EPIC_BEARER_TOKEN") || !hasCookie("EPIC_SESSION_AP") || !hasCookie("EPIC_SSO");
	}

	public boolean requiresFullLogin() {
		return _cookies.isEmpty();
	}

	public void clearSession() {
		_cookies.clear();
		_xsrf = "";
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("DELETE FROM cookies");
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}
}
