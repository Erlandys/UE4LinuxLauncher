package launcher.model;

import launcher.Main;
import launcher.objects.EpicCategory;
import launcher.objects.EpicItem;
import launcher.objects.EpicOwnedAsset;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;

public class User {
	private String _lastLogin;
	private String _country;
	private String _lastName;
	private String _displayName;
	private String _phoneNumber;
	private String _name;
	private String _email;
	private String _id;
	private String _clientId;
	private String _username, _password;
	private boolean _twoFactorAuth;
	private String _accessToken;
	private String _exchangeCode;

	private HashMap<String, EpicCategory> _categories;
	private HashMap<String, EpicItem> _items;
	private HashMap<String, EpicOwnedAsset> _ownedAssets;
	private String _ue4InstallDir;
	private double _engineVersion;
	private HashMap<String, String> _projects;
	private String _currentProject;

	public User()
	{
		_twoFactorAuth = false;
		_categories = new HashMap<>();
		_items = new HashMap<>();
		_ownedAssets = new HashMap<>();
		_ue4InstallDir = "";
		_engineVersion = 0;
		_projects = new HashMap<>();
	}

	public void parseData(JSONObject data)
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

	public void setClientId(String clientId) {
		_clientId = clientId;
	}

	public String getClientId() {
		return _clientId;
	}

	public String getUsername() {
		return _username;
	}

	public void setUsername(String username) {
		_username = username;
	}

	public String getPassword() {
		return _password;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public boolean isTwoFactorAuth() {
		return _twoFactorAuth;
	}

	public void setTwoFactorAuth(boolean twoFactorAuth) {
		_twoFactorAuth = twoFactorAuth;
	}

	public String getAccessToken() {
		return _accessToken;
	}

	public void setAccessToken(String accessToken) {
		_accessToken = accessToken;
	}

	public String getExchangeCode() {
		return _exchangeCode;
	}

	public void setExchangeCode(String exchangeCode) {
		_exchangeCode = exchangeCode;
	}

	public String getUe4InstallDir() {
		return _ue4InstallDir;
	}

	public EpicCategory getCategoryByPath(String path)
	{
		return _categories.values().stream().filter(category -> category.getPath().equals(path)).findFirst().orElse(null);
	}

	public EpicCategory getCategory(String name)
	{
		return _categories.get(name);
	}

	public EpicItem getItemByCatalogId(String catalogItemId)
	{
		return _items.get(catalogItemId);
	}

	public boolean containsOwnedAsset(String catalogItemId)
	{
		return _ownedAssets.containsKey(catalogItemId);
	}

	public EpicOwnedAsset getOwnedAsset(String catalogItemId)
	{
		return _ownedAssets.get(catalogItemId);
	}

	public Collection<EpicOwnedAsset> getOwnedAssets()
	{
		return _ownedAssets.values();
	}

	public void addOwnedAsset(EpicOwnedAsset asset)
	{
		_ownedAssets.put(asset.getCatalogItemId(), asset);
	}

	public void clearOwnedAssets()
	{
		_ownedAssets.clear();
	}

	public void addItem(EpicItem item)
	{
		_items.put(item.getCatalogItemId(), item);
	}

	public void createCategory(String path, String name)
	{
		_categories.put(name, new EpicCategory(path, name));
	}

	public void updateCategoriesList()
	{
		Main.getInstance().getMainForm().updateCategoriesList(_categories.keySet());
	}

	public void setEngineVersion(double engineVersion) {
		_engineVersion = engineVersion;
		Main.getInstance().getMainForm().setEngineVersion(engineVersion);
	}

	public HashMap<String, String> getProjects() {
		return _projects;
	}

	public String getCurrentProject() {
		return _currentProject;
	}

	public void setCurrentProject(String currentProject) {
		_currentProject = currentProject;
	}

	public void setUe4InstallDir(String ue4InstallDir) {
		_ue4InstallDir = ue4InstallDir;
	}

	public double getEngineVersion() {
		return _engineVersion;
	}
}
