package launcher.managers;

import launcher.MainForm;
import launcher.objects.User;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class EngineManager {
	private static String VERSION_FILE_LOCATION_1 = "/Engine/Binaries/Linux/UE4Editor.version";
	private static String VERSION_FILE_LOCATION_2 = "/Engine/Binaries/UE4Editor.version";
	private static String VERSION_FILE_LOCATION_3 = "/Engine/Binaries/Linux/Binaries/Linux/UE4Editor.version";

	private static String EDITOR_SETTINGS_LOCATION = "/Engine/Saved/Config/Linux/EditorSettings.ini";

	public void readEngineData() {
		User user = SessionManager.getInstance().getUser();
		user.getProjects().clear();
		if (user.getUe4InstallLocation().isEmpty()) {
			// TODO: Update projects list...
			return;
		}

		String filePath = findVersionFile(user.getUe4InstallLocation());
		if (filePath == null || !parseVersionFile(user, filePath)) {
			// TODO: Update projects list...
			return;
		}

		filePath = findEditorSettings(user.getUe4InstallLocation());
		if (filePath == null || !parseEditorSettings(user, filePath)) {
			// TODO: Update projects list...
			return;
		}
	}

	private String findVersionFile(String path) {
		File versionFile = new File(path + VERSION_FILE_LOCATION_1);
		if (versionFile.exists())
			return path + VERSION_FILE_LOCATION_1;

		versionFile = new File(path + VERSION_FILE_LOCATION_2);
		if (versionFile.exists())
			return path + VERSION_FILE_LOCATION_2;

		versionFile = new File(path + VERSION_FILE_LOCATION_3);
		if (versionFile.exists())
			return path + VERSION_FILE_LOCATION_3;

		ArrayList<Path> files = findFile(path, "UE4Editor.version");

		if (files.isEmpty())
			return null;

		return files.get(0).toString();
	}

	private boolean parseVersionFile(User user, String filePath) {
		File file = new File(filePath);
		if (!file.exists())
			return false;
		Path path = Paths.get(filePath);
		try {
			String data = new String(Files.readAllBytes(path));
			JSONObject jsonObject = new JSONObject(data);
			double version;
			int majorVersion = jsonObject.getInt("MajorVersion");
			int minorVersion = jsonObject.getInt("MinorVersion");
			version = majorVersion + (minorVersion / 100.0);
			user.setUnrealEngineVersion(version);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	private String findEditorSettings(String path) {
		File editoSettingsFile = new File(path + EDITOR_SETTINGS_LOCATION);
		if (editoSettingsFile.exists())
			return path + EDITOR_SETTINGS_LOCATION;

		ArrayList<Path> files = findFile(path, "EditorSettings.ini");

		if (files.isEmpty())
			return null;

		return files.get(0).toString();
	}

	private boolean parseEditorSettings(User user, String filePath) {
		File file = new File(filePath);
		if (!file.exists())
			return false;
		Path path = Paths.get(filePath);
		try {
			String content = new String(Files.readAllBytes(path));
			for (String line : content.split("\n")) {
				if (!line.startsWith("RecentlyOpenedProjectFiles="))
					continue;
				String projectPath = line.split("RecentlyOpenedProjectFiles=")[1];
				String projectName = projectPath.substring(projectPath.lastIndexOf('/') + 1).split("\\.")[0];
				projectPath = projectPath.substring(0, projectPath.lastIndexOf('/') + 1);

				// check if project still exists before adding it to the list
				if(new File(projectPath).exists()) {
					user.getProjects().put(projectName, projectPath);
				}
			}
			MainForm.getInstance().updateProjectsList();
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	private ArrayList<Path> findFile(String path, String fileName) {
		ArrayList<Path> files = new ArrayList<>();
		try {
			Files.find(Paths.get(path),
					Integer.MAX_VALUE,
					(filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.endsWith(fileName))
					.forEach(files::add);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return files;
	}

	public static EngineManager getInstance() {
		return SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final EngineManager _instance = new EngineManager();
	}
}
