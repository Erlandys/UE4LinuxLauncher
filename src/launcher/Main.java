package launcher;

import launcher.objects.EpicCategory;
import launcher.objects.EpicItem;
import launcher.objects.EpicOwnedAsset;
import launcher.objects.HtmlUtils;

import java.util.Collection;
import java.util.HashMap;

public class Main {
    private static Main _instance;

    public static void main(String[] args) {
        _instance = new Main();
    }

    private LoginForm _loginForm;
    private MainForm _mainForm;
    private EpicAPI _epicAPI;
    private DownloadForm _downloadForm;
    private HashMap<String, EpicCategory> _categories;
    private HashMap<String, EpicItem> _items;
    private HashMap<String, EpicOwnedAsset> _ownedAssets;
    private String _username;
    private String _ue4InstallDir;
    private double _engineVersion;
    private HashMap<String, String> _projects;
    private String _currentProject;

    private Main()
    {
        _epicAPI = new EpicAPI();
        _categories = new HashMap<>();
        _items = new HashMap<>();
        _ownedAssets = new HashMap<>();
        _downloadForm = null;
        _ue4InstallDir = "";
        _engineVersion = 0;
        _projects = new HashMap<>();
        checkIfLoggedIn();
        HtmlUtils.initData();
    }

    private void checkIfLoggedIn()
    {
        _loginForm = new LoginForm();
        if (!_epicAPI.doAutoLogin(this)) {
            _loginForm.setLoginData(_epicAPI.getUsername(), _epicAPI.getPassword());
            _loginForm.allowActions();
        }
        else {
            _loginForm.dispose();
            _username = _epicAPI.getUsername();
            _mainForm = new MainForm(_epicAPI.getUsername());
            new Thread(() -> {
                _mainForm.setEngineInstallDir(getUe4InstallDir());
                _mainForm.disableActions();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                _epicAPI.readEngineData(_ue4InstallDir);
                _epicAPI.updateCategories(false);
                _epicAPI.generateItems();
                _epicAPI.getOwnedAssets();
                getMainForm().hideLoading();
                getMainForm().enableActions();
            }).start();
        }
    }

    public LoginForm getLoginForm() {
        return _loginForm;
    }

    public EpicAPI getEpicAPI() {
        return _epicAPI;
    }

    public MainForm getMainForm()
    {
        return _mainForm;
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

    public void successfulLogin(String username)
    {
        _loginForm.dispose();
        _username = username;
        _mainForm = new MainForm(_username);
        new Thread(() -> {
            _mainForm.disableActions();
            if (_ue4InstallDir != null && _ue4InstallDir.length() > 0)
            {
                _mainForm.setEngineInstallDir(_ue4InstallDir);
                _epicAPI.readEngineData(_ue4InstallDir);
            }
            _epicAPI.updateCategories(false);
            _epicAPI.generateItems();
            _epicAPI.getOwnedAssets();
            getMainForm().hideLoading();
            getMainForm().enableActions();
        }).start();
    }

    public void createCategory(String path, String name)
    {
        _categories.put(name, new EpicCategory(path, name));
    }

    public void updateCategoriesList()
    {
        _mainForm.updateCategoriesList(_categories.keySet());
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(String username) {
        _username = username;
    }

    public void showDownloadForm(EpicItem item)
    {
        _downloadForm = new DownloadForm(item);
    }

    public DownloadForm getDownloadForm() {
        return _downloadForm;
    }

    public String getUe4InstallDir() {
        return _ue4InstallDir;
    }

    public void setUe4InstallDir(String ue4InstallDir) {
        _ue4InstallDir = ue4InstallDir;
    }

    public double getEngineVersion() {
        return _engineVersion;
    }

    public void setEngineVersion(double engineVersion) {
        _engineVersion = engineVersion;
        _mainForm.setEngineVersion(engineVersion);
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

    public void doLogout()
    {
        _mainForm.dispose();
        _loginForm = new LoginForm();
        _loginForm.setLoginData(_epicAPI.getUsername(), _epicAPI.getPassword());
        _loginForm.allowActions();
    }

    public static Main getInstance()
    {
        return _instance;
    }
}
