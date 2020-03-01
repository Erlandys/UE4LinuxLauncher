package launcher.objects;

import launcher.managers.DatabaseManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class EpicItemReleaseInfo {
	private int _id;
	private String _releaseId;
	private String _appId;
	private Map<Double, String> _compatibility;
	private List<String> _platforms;
	private double _lowestVersion;
	private double _highestVersion;
	private String _versionTitle;

	private EpicItemReleaseInfo() {
		_id = 0;
		_compatibility = new HashMap<>();
		_platforms = new LinkedList<>();
	}

	public EpicItemReleaseInfo(JSONObject object) {
		_id = 0;
		_platforms = new LinkedList<>();
		_compatibility = new HashMap<>();

		_releaseId = object.getString("id");
		_versionTitle = object.getString("versionTitle");
		_appId = object.getString("appId");
		JSONArray compatibility = object.getJSONArray("compatibleApps");
		_lowestVersion = Integer.MAX_VALUE;
		_highestVersion = Integer.MIN_VALUE;
		for (int h = 0; h < compatibility.length(); h++) {
			String engineVersion = compatibility.getString(h);
			if (engineVersion == null)
				continue;
			if (!engineVersion.startsWith("UE_"))
				continue;
			engineVersion = engineVersion.substring(3);
			double versionNumber = Double.parseDouble(engineVersion);
			if (versionNumber < _lowestVersion)
				_lowestVersion = versionNumber;
			if (versionNumber > _highestVersion)
				_highestVersion = versionNumber;
			_compatibility.put(versionNumber, engineVersion);
		}
		JSONArray platforms = object.getJSONArray("platform");
		for (int h = 0; h < platforms.length(); h++) {
			String platform = platforms.getString(h);
			if (platform == null)
				continue;
			_platforms.add(platform);
		}
	}

	public String getVersionTitle() {
		return _versionTitle;
	}

	public void setVersionTitle(String versionTitle) {
		_versionTitle = versionTitle;
	}

	public String getReleaseId() {
		return _releaseId;
	}

	public void setReleaseId(String releaseId) {
		_releaseId = releaseId;
	}

	public String getAppId() {
		return _appId;
	}

	public void setAppId(String appId) {
		_appId = appId;
	}

	public Map<Double, String> getCompatibility() {
		return _compatibility;
	}

	public double getLowestVersion() {
		return _lowestVersion;
	}

	public void setLowestVersion(double lowestVersion) {
		_lowestVersion = lowestVersion;
	}

	public double getHighestVersion() {
		return _highestVersion;
	}

	public void setHighestVersion(double highestVersion) {
		_highestVersion = highestVersion;
	}

	public List<String> getPlatforms() {
		return _platforms;
	}

	private String getPlatformsString() {
		JSONArray result = new JSONArray();
		for (String platform : _platforms) {
			result.put(platform);
		}
		return result.toString();
	}

	private String getCompatibilitiesString() {
		JSONArray result = new JSONArray();
		for (String version : _compatibility.values()) {
			result.put(version);
		}
		return result.toString();
	}

	public void save(int itemId) {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("INSERT INTO item_releases(item_id, release_id,  app_id, lowest_version, highest_version, version_title, compatibility, platforms) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, itemId);
			statement.setString(2, _releaseId);
			statement.setString(3, _appId);
			statement.setDouble(4, _lowestVersion);
			statement.setDouble(5, _highestVersion);
			statement.setString(6, _versionTitle);
			statement.setString(7, getCompatibilitiesString());
			statement.setString(8, getPlatformsString());
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static LinkedList<EpicItemReleaseInfo> loadReleaseInfosByItem(int itemId) {
		LinkedList<EpicItemReleaseInfo> result = new LinkedList<>();
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM item_releases WHERE item_id = ?");
			statement.setInt(1, itemId);
			ResultSet rset = statement.executeQuery();
			while (rset.next()) {
				EpicItemReleaseInfo release = new EpicItemReleaseInfo();
				release.setReleaseId(rset.getString("release_id"));
				release.setAppId(rset.getString("app_id"));
				release.setLowestVersion(rset.getDouble("lowest_version"));
				release.setHighestVersion(rset.getDouble("highest_version"));
				release.setVersionTitle(rset.getString("version_title"));
				JSONArray platforms = new JSONArray(rset.getString("platforms"));
				for (int i = 0; i < platforms.length(); i++) {
					release.getPlatforms().add(platforms.getString(i));
				}
				JSONArray compatibilities = new JSONArray(rset.getString("compatibility"));
				for (int i = 0; i < compatibilities.length(); i++) {
					release.getCompatibility().put(Double.parseDouble(compatibilities.getString(i)), compatibilities.getString(i));
				}
				result.add(release);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
}
