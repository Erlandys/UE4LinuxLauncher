package launcher;

import launcher.managers.CookiesManager;
import launcher.managers.InputsManager;
import launcher.model.User;
import launcher.objects.*;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

public class Main {
    public static boolean DEBUG = false;
    private static Main _instance;

    public static void main(String[] args) {
        _instance = new Main();
    }

    private LoginForm _loginForm;
    private MainForm _mainForm;
    private EpicAPI _epicAPI;
    private DownloadForm _downloadForm;
    private TwoFactorForm _twoFactorForm;
    private User _user;

    private Main()
    {
        if (DEBUG)
        {
            File directory = new File("output");
            if (directory.exists() && directory.isDirectory())
            {
                File files[] = directory.listFiles();
                if (files != null)
                    for (File file : files)
                    {
                        if (file.exists())
                            file.delete();
                    }
                directory.delete();
            }
        }
        CookiesManager.getInstance();
        InputsManager.getInstance();
        _user = new User();
        _epicAPI = new EpicAPI();
        _downloadForm = null;
        _twoFactorForm = null;
        checkIfLoggedIn();
        HtmlUtils.initData();
    }

    private void checkIfLoggedIn()
    {
        _loginForm = new LoginForm();
        if (!_epicAPI.doAutoLogin(this) || !_epicAPI.isLoggedIn(_user.getAccessToken())) {
            _loginForm.setLoginData(_user.getUsername(), _user.getPassword());
            _loginForm.allowActions();
        }
        else {
            _loginForm.dispose();
            _mainForm = new MainForm(_user.getUsername());
            new Thread(() -> {
                _mainForm.setEngineInstallDir(_user.getUe4InstallDir());
                _mainForm.disableActions();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                _epicAPI.readEngineData(_user.getUe4InstallDir());
                _epicAPI.getAccountInfo();
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

    public TwoFactorForm getTwoFactorForm() {
        return _twoFactorForm;
    }

    public void setTwoFactorForm(TwoFactorForm twoFactorForm) {
        _twoFactorForm = twoFactorForm;
    }

    public void successfulLogin()
    {
        _loginForm.dispose();
        _mainForm = new MainForm(_user.getUsername());
        new Thread(() -> {
            _mainForm.disableActions();
            if (_user.getUe4InstallDir() != null && _user.getUe4InstallDir().length() > 0)
            {
                _mainForm.setEngineInstallDir(_user.getUe4InstallDir());
                _epicAPI.readEngineData(_user.getUe4InstallDir());
            }
            _epicAPI.isLoggedIn(_user.getAccessToken());
            _epicAPI.getAccountInfo();
            _epicAPI.updateCategories(false);
            _epicAPI.generateItems();
            _epicAPI.getOwnedAssets();
            getMainForm().hideLoading();
            getMainForm().enableActions();
        }).start();
    }

    public void showDownloadForm(EpicItem item)
    {
        _downloadForm = new DownloadForm(item);
    }

    public DownloadForm getDownloadForm() {
        return _downloadForm;
    }

    public void doLogout()
    {
        _mainForm.dispose();
        File file = new File("data.dat");
        if (file.exists())
            file.delete();
        User oldUser = _user;
        _user = new User();
        _user.setUsername(oldUser.getUsername());
        _user.setPassword(oldUser.getPassword());
        _loginForm = new LoginForm();
        _loginForm.setLoginData(_user.getUsername(), _user.getPassword());
        _loginForm.allowActions();
    }

    public User getUser() {
        return _user;
    }

    public static Main getInstance()
    {
        return _instance;
    }
}
