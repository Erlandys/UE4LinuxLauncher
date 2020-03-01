package launcher.objects;

import launcher.MainForm;
import launcher.managers.DatabaseManager;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class User {
	private String _displayName;
	private String _email, _password;
	private String _ue4InstallLocation;

	private String _exchangeCode;
	private String _oAuthAccessToken;
	private int _oAuthAccessTokenExpirationDate;
	private String _oAuthExchangeCode;
	private String _oAuthRefreshToken;
	private int _oAuthRefreshTokenExpirationDate;

	private String _accountId;
	private String _clientId;
	private String _appId;
	private String _deviceId;

	private double _unrealEngineVersion;

	private List<EpicItem> _ownedItems;
	private Map<String, String> _projects;

	private String _currentProject;

	public User() {
		_ownedItems = new LinkedList<>();
		_projects = new HashMap<>();
		_ue4InstallLocation = "";
		_currentProject = "";
	}

	public String getDisplayName() {
		return _displayName;
	}

	public void setDisplayName(String displayName) {
		_displayName = displayName;
	}

	public String getEmail() {
		return _email;
	}

	public void setEmail(String email) {
		_email = email;
	}

	public String getPassword() {
		return _password;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public String getUe4InstallLocation() {
		return _ue4InstallLocation;
	}

	public void setUe4InstallLocation(String ue4InstallLocation) {
		_ue4InstallLocation = ue4InstallLocation;
	}

	public double getUnrealEngineVersion() {
		return _unrealEngineVersion;
	}

	public void setUnrealEngineVersion(double unrealEngineVersion) {
		_unrealEngineVersion = unrealEngineVersion;
		MainForm.getInstance().setEngineVersion(_unrealEngineVersion);
	}

	public Map<String, String> getProjects() {
		return _projects;
	}

	public String getExchangeCode() {
		return _exchangeCode;
	}

	public void setExchangeCode(String exchangeCode) {
		_exchangeCode = exchangeCode;
	}

	public String getOAuthAccessToken() {
		return _oAuthAccessToken;
	}

	public void setOAuthAccessToken(String oAuthAccessToken) {
		_oAuthAccessToken = oAuthAccessToken;
	}

	public String getOAuthExchangeCode() {
		return _oAuthExchangeCode;
	}

	public void setOAuthExchangeCode(String oAuthExchangeCode) {
		_oAuthExchangeCode = oAuthExchangeCode;
	}

	public void setOAuthAccessTokenExpirationDate(String expireAt) {
		try {
			Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(expireAt);
			_oAuthAccessTokenExpirationDate = (int) (date.getTime() / 1000);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public boolean oAuthAccessTokenExpired() {
		return (long) _oAuthAccessTokenExpirationDate * 1000 < System.currentTimeMillis();
	}

	public String getOAuthRefreshToken() {
		return _oAuthRefreshToken;
	}

	public void setOAuthRefreshToken(String oAuthRefreshToken) {
		_oAuthRefreshToken = oAuthRefreshToken;
	}

	public void setOAuthRefreshTokenExpirationDate(String expireAt) {
		try {
			Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(expireAt);
			_oAuthRefreshTokenExpirationDate = (int) (date.getTime() / 1000);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public boolean oAuthRefreshTokenExpired() {
		return (long) _oAuthRefreshTokenExpirationDate * 1000 < System.currentTimeMillis();
	}

	public void setAccountId(String accountId) {
		_accountId = accountId;
	}

	public void setClientId(String clientId) {
		_clientId = clientId;
	}

	public void setAppId(String appId) {
		_appId = appId;
	}

	public void setDeviceId(String deviceId) {
		_deviceId = deviceId;
	}

	public void save() {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			saveUserData(connection);
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private void saveUserData(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("REPLACE INTO user_data(email, password, name, engine_install_dir, exchange_code, oauth_access_key, oauth_access_key_expiration, oauth_exchange_code, oauth_refresh_token, oauth_refresh_token_expiration, account_id, client_id, app_id, device_id) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		statement.setString(1, _email);
		statement.setString(2, _password);
		statement.setString(3, _displayName);
		statement.setString(4, _ue4InstallLocation);
		statement.setString(5, _exchangeCode);
		statement.setString(6, _oAuthAccessToken);
		statement.setInt(7, _oAuthAccessTokenExpirationDate);
		statement.setString(8, _oAuthExchangeCode);
		statement.setString(9, _oAuthRefreshToken);
		statement.setInt(10, _oAuthRefreshTokenExpirationDate);
		statement.setString(11, _accountId);
		statement.setString(12, _clientId);
		statement.setString(13, _appId);
		statement.setString(14, _deviceId);
		statement.executeUpdate();
	}

	public void load() {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			loadUserData(connection);
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private void loadUserData(Connection connection) throws SQLException {
		Statement statement = connection.createStatement();
		ResultSet rset = statement.executeQuery("SELECT * FROM user_data LIMIT 1");
		if (!rset.next())
			return;
		_email = rset.getString("email");
		_password = rset.getString("password");
		_displayName = rset.getString("name");
		_ue4InstallLocation = rset.getString("engine_install_dir");
		_exchangeCode = rset.getString("exchange_code");
		_oAuthAccessToken = rset.getString("oauth_access_key");
		_oAuthAccessTokenExpirationDate = rset.getInt("oauth_access_key_expiration");
		_oAuthExchangeCode = rset.getString("oauth_exchange_code");
		_oAuthRefreshToken = rset.getString("oauth_refresh_token");
		_oAuthRefreshTokenExpirationDate = rset.getInt("oauth_refresh_token_expiration");
		_accountId = rset.getString("account_id");
		_clientId = rset.getString("client_id");
		_appId = rset.getString("app_id");
		_deviceId = rset.getString("device_id");
	}

	public void addOwnedItem(EpicItem item) {
		_ownedItems.add(item);
	}

	public List<EpicItem> getOwnedItems() {
		return _ownedItems;
	}

	public String getCurrentProject() {
		return _currentProject;
	}

	public void setCurrentProject(String currentProject) {
		_currentProject = currentProject;
	}
}
