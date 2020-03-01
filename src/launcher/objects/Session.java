package launcher.objects;

import launcher.managers.DatabaseManager;

import java.sql.*;
import java.util.*;

public class Session {
	private String _xsrf;
	private Map<String, AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, Integer>>> _cookies;

	public Session() {
		_cookies = new HashMap<>();
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

	public boolean load() {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			loadCookies(connection);
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
