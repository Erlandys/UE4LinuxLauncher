package launcher;

import launcher.model.User;
import launcher.objects.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class EpicAPI {
    public static void main(String args[])
    {
        new EpicAPI();
    }

    private Map<String, String> _cookies;
    private Map<String, String> _inputParameters;
    private JSONObject _epicOAuth;
    private String _accessToken;
    private String _exchangeCode;
    private String _username;
    private String _password;
    private User _user;

    public EpicAPI()
    {
        _inputParameters = new HashMap<>();
        _cookies = new HashMap<>();
        _epicOAuth = null;
    }

    public boolean doAutoLogin(Main main)
    {
        Path path = Paths.get("data.dat");
        try {
            byte[] data = Files.readAllBytes(path);
            if (data.length < 4)
                return false;
            int pos = 0;
            int usernameLength = data[pos++] << 24 | (data[pos++] & 0xFF) << 16 | (data[pos++] & 0xFF) << 8 | (data[pos++] & 0xFF);
            StringBuilder username = new StringBuilder();
            for (int i = pos; i < pos + usernameLength; i++)
                username.append((char) data[i]);
            pos += usernameLength;
            int passwordLength = data[pos++] << 24 | (data[pos++] & 0xFF) << 16 | (data[pos++] & 0xFF) << 8 | (data[pos++] & 0xFF);
            StringBuilder password = new StringBuilder();
            for (int i = pos; i < pos + passwordLength; i++)
                password.append((char) data[i]);
            pos += passwordLength;
            int accessTokenLength = data[pos++] << 24 | (data[pos++] & 0xFF) << 16 | (data[pos++] & 0xFF) << 8 | (data[pos++] & 0xFF);
            StringBuilder accessToken = new StringBuilder();
            for (int i = pos; i < pos + accessTokenLength; i++)
                accessToken.append((char) data[i]);
            pos += accessTokenLength;
            _accessToken = accessToken.toString();
            int exchangeCodeLength = data[pos++] << 24 | (data[pos++] & 0xFF) << 16 | (data[pos++] & 0xFF) << 8 | (data[pos++] & 0xFF);
            StringBuilder exchangeCode = new StringBuilder();
            for (int i = pos; i < pos + exchangeCodeLength; i++)
                exchangeCode.append((char) data[i]);
            pos += exchangeCodeLength;
            int cookiesLength = data[pos++] << 24 | (data[pos++] & 0xFF) << 16 | (data[pos++] & 0xFF) << 8 | (data[pos++] & 0xFF);
            StringBuilder cookies = new StringBuilder();
            for (int i = pos; i < pos + cookiesLength; i++)
                cookies.append((char) data[i]);
            pos += cookiesLength;
            int installDirLength = data[pos++] << 24 | (data[pos++] & 0xFF) << 16 | (data[pos++] & 0xFF) << 8 | (data[pos++] & 0xFF);
            if (installDirLength > 0) {
                StringBuilder installDir = new StringBuilder();
                for (int i = pos; i < pos + installDirLength; i++)
                    installDir.append((char) data[i]);
                main.setUe4InstallDir(installDir.toString());
                pos += installDirLength;
            }
            _exchangeCode = exchangeCode.toString();
            _username = username.toString();
            _password = password.toString();
            parseStartupCookies(cookies.toString());
            return getItemsCount(true) == 1;
        } catch (IOException e) {
        }

        return false;
    }

    public void readEngineData() {
        readEngineData(Main.getInstance().getUe4InstallDir());
    }

    public void readEngineData(String enginePath)
    {
        Main.getInstance().getProjects().clear();
        if (enginePath.length() < 1) {
            Main.getInstance().getMainForm().updateProjectsList();
            return;
        }
        File f = new File(enginePath + "/Engine/Binaries/Linux/UE4Editor.version");
        if (f.exists()) {
            Path path = Paths.get(enginePath + "/Engine/Binaries/Linux/UE4Editor.version");
            try {
                String data = new String(Files.readAllBytes(path));
                JSONObject jsonObject = new JSONObject(data);
                double version;
                int majorVersion = jsonObject.getInt("MajorVersion");
                int minorVersion = jsonObject.getInt("MinorVersion");
                version = majorVersion + (minorVersion / 100.0);
                Main.getInstance().setEngineVersion(version);
                readProjects(enginePath);
            }
            catch (Exception e) {
                Main.getInstance().setEngineVersion(0);
            }
        }
        else
            Main.getInstance().setEngineVersion(0);
        Main.getInstance().getMainForm().updateProjectsList();
    }

    public void readProjects(String pathToEngine)
    {
        Path path = Paths.get(pathToEngine + "/Engine/Saved/Config/Linux/EditorSettings.ini");
        try {
            String content = new String(Files.readAllBytes(path));
            for (String line : content.split("\n"))
            {
                if (!line.startsWith("RecentlyOpenedProjectFiles="))
                    continue;
                String projectPath = line.split("RecentlyOpenedProjectFiles=")[1];
                String projectName = projectPath.substring(projectPath.lastIndexOf('/') + 1).split("\\.")[0];
                projectPath = projectPath.substring(0, projectPath.lastIndexOf('/') + 1);
                Main.getInstance().getProjects().put(projectName, projectPath);
            }
        } catch (IOException e) {
        }
    }

    public void updateInstallDir(String installDir)
    {
        Main.getInstance().setUe4InstallDir(installDir);
        writeToFile(_username, _password);
        Main.getInstance().getMainForm().setEngineInstallDir(installDir);
        readEngineData();
    }

    private void writeToFile(String username, String password)
    {
        password = password.replaceAll("\n", "");
        _accessToken = _accessToken.replaceAll(" ", "");
        _exchangeCode = _exchangeCode.replaceAll(" ", "");
        try (FileOutputStream fos = new FileOutputStream("data.dat")) {
            fos.write(toByteArray(username.length()));
            fos.write(username.getBytes());
            fos.write(toByteArray(password.length()));
            fos.write(password.getBytes());
            fos.write(toByteArray(_accessToken.length()));
            fos.write(_accessToken.getBytes());
            fos.write(toByteArray(_exchangeCode.length()));
            fos.write(_exchangeCode.getBytes());
            String cookies = getCookiesString();
            fos.write(toByteArray(cookies.length()));
            fos.write(cookies.getBytes());
            fos.write(toByteArray(Main.getInstance().getUe4InstallDir().length()));
            if (Main.getInstance().getUe4InstallDir().length() > 0)
                fos.write(Main.getInstance().getUe4InstallDir().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] toByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value };
    }
    String _clientId = "";
    private void getCookies()
    {
        String url = "";
        URL oracle = null;
        try {
            oracle = new URL("https://accounts.unrealengine.com/login");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setInstanceFollowRedirects( false );
            parseCookies(conn.getHeaderFields().get("Set-Cookie"));
            url = conn.getHeaderFields().get("Location").get(0);
            Main.getInstance().getLoginForm().increaseProgressBarValue(4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            oracle = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setInstanceFollowRedirects( false );
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("DNT", "1");
            url = conn.getHeaderFields().get("Location").get(0);
            Main.getInstance().getLoginForm().increaseProgressBarValue(4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            oracle = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setInstanceFollowRedirects( false );
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("DNT", "1");
            url = conn.getHeaderFields().get("Location").get(0);
            _cookies.clear();
            parseCookies(conn.getHeaderFields().get("Set-Cookie"));
            Main.getInstance().getLoginForm().increaseProgressBarValue(4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            oracle = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("DNT", "1");
            Main.getInstance().getLoginForm().increaseProgressBarValue(4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        _clientId = url.split("client_id=")[1].split("&")[0];
        try {
            oracle = new URL("https://accounts.unrealengine.com/login/doLogin?client_id=" + _clientId + "&redirectUrl=https://www.unrealengine.com/");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("DNT", "1");

            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    if (inputLine.contains("<input ") && inputLine.contains("name=\"")) {
                        String inputName = inputLine.split("name=\"")[1].split("\"")[0];
                        String inputValue = null;
                        if (inputLine.contains("value=\""))
                            inputValue = inputLine.split("value=\"")[1].split("\"")[0];
                        _inputParameters.put(inputName, inputValue);
                    }
                }
                _cookies.clear();
                parseCookies(conn.getHeaderFields().get("Set-Cookie"));
                Main.getInstance().getLoginForm().increaseProgressBarValue(4);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseCookies(List<String> strings)
    {
        for (String cookiesString : strings)
            for (String data : cookiesString.split(";"))
            {
                if (data.length() < 1)
                    continue;
                String splitted[] = data.split("=");
                if (splitted.length < 2)
                {
                    _cookies.put(splitted[0], "");
                    continue;
                }
                _cookies.put(splitted[0], splitted[1]);
            }
    }

    private void parseStartupCookies(String cookiesString)
    {
        for (String data : cookiesString.split(";"))
        {
            if (data.length() < 1)
                continue;
            String splitted[] = data.split("=");
            if (splitted.length < 2)
            {
                _cookies.put(splitted[0], "");
                continue;
            }
            _cookies.put(splitted[0].trim(), splitted[1]);
        }
    }

    public void doLogin(String username, String password)
    {
        getCookies();
        _inputParameters.put("epic_username", username);
        _inputParameters.put("password", password);
        _inputParameters.put("client_id", _clientId);
        _inputParameters.put("redirectUrl", "https://www.unrealengine.com/en-US/blog");

        try
        {
            String data = getQuery(_inputParameters);
            byte[] postData = data.getBytes( StandardCharsets.UTF_8 );
            int    postDataLength = postData.length;
            URL oracle = new URL("https://accounts.unrealengine.com/login/doLogin");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setDoOutput( true );
            conn.setDoInput( true );
            conn.setInstanceFollowRedirects( false );
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", postDataLength + "");
            String xsrf = _cookies.get("XSRF-TOKEN");
            conn.setRequestProperty("X-XSRF-TOKEN", xsrf);
            conn.setRequestMethod("POST");
            try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                wr.write(postData);
            }
            if (conn.getResponseCode() == 200) {
                _cookies.clear();
                _cookies.put("XSRF-TOKEN", xsrf);
                parseCookies(conn.getHeaderFields().get("Set-Cookie"));
                Main.getInstance().getLoginForm().increaseProgressBarValue(4);
                authorize(username, password);
            }
            else
                Main.getInstance().getLoginForm().badLogin();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    private void authorize(String username, String password)
    {
        try
        {
            URL oracle = new URL("https://accounts.unrealengine.com/authorize/index?client_id=" + _clientId + "&response_type=code&forWidget=true");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("host", "accounts.unrealengine.com");
            if (conn.getResponseCode() == 200)
            {
                parseCookies(conn.getHeaderFields().get("Set-Cookie"));
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    String code = inputLine.split("code=")[1].split("\"")[0];
                    Main.getInstance().getLoginForm().increaseProgressBarValue(15);
                    exchange(username, password, code);
                }
            }
            else
                Main.getInstance().getLoginForm().badLogin();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    private void exchange(String username, String password, String code)
    {
        try
        {
            URL oracle = new URL("https://www.unrealengine.com/exchange?code=" + code);
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("host", "accounts.unrealengine.com");
            conn.setInstanceFollowRedirects( false );
            conn.connect();
            if (conn.getResponseCode() == 302)
            {
                parseCookies(conn.getHeaderFields().get("Set-Cookie"));
                Main.getInstance().getLoginForm().increaseProgressBarValue(15);
                OAuth(username, password);
            }
            else
                Main.getInstance().getLoginForm().badLogin();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    private void OAuth(String username, String password)
    {
        HashMap<String, String> inputParameters = new HashMap<>();
        inputParameters.put("grant_type", "password");
        inputParameters.put("username", username);
        inputParameters.put("password", password);
        inputParameters.put("includePerms", "true");

        try
        {
            String data = getQuery(inputParameters);
            byte[] postData = data.getBytes( StandardCharsets.UTF_8 );
            int    postDataLength = postData.length;
            URL oracle = new URL("https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/token");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setDoOutput( true );
            conn.setDoInput( true );
            conn.setInstanceFollowRedirects( false );
            conn.setRequestProperty("Authorization", "basic MzRhMDJjZjhmNDQxNGUyOWIxNTkyMTg3NmRhMzZmOWE6ZGFhZmJjY2M3Mzc3NDUwMzlkZmZlNTNkOTRmYzc2Y2Y=");
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty( "charset", "utf-8");
            conn.setRequestProperty( "Content-Length", postDataLength + "");
            conn.setRequestMethod("POST");
            try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                wr.write(postData);
            }
            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                _epicOAuth = new JSONObject(content.toString());
                _accessToken = _epicOAuth.getString("access_token");
                Main.getInstance().getLoginForm().increaseProgressBarValue(15);
                OAuthExchange(username, password);
            }
            else
                Main.getInstance().getLoginForm().badLogin();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    private void OAuthExchange(String username, String password)
    {
        try
        {
            URL oracle = new URL("https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/exchange");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("host", "accounts.unrealengine.com");
            conn.setInstanceFollowRedirects( false );
            conn.setDoInput( true );
            conn.connect();
            if (conn.getResponseCode() == 200)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                JSONObject object = new JSONObject(content.toString());
                _epicOAuth.put("code", object.getString("code"));
                _exchangeCode = object.getString("code");
                Main.getInstance().getLoginForm().increaseProgressBarValue(15);
                SSOWithOAuthCode(username, password, false);
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    private boolean SSOWithOAuthCode(String username, String password, boolean auto)
    {
        try
        {
            URL oracle = new URL("https://accountportal-website-prod07.ol.epicgames.com/exchange?exchangeCode=" + _exchangeCode + "&state=/getSsoStatus");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("host", "accounts.unrealengine.com");
            conn.setInstanceFollowRedirects( false );
            conn.setDoInput( true );
            conn.connect();
            if (conn.getResponseCode() == 302)
            {
                Main.getInstance().getLoginForm().increaseProgressBarValue(15);
                _username = username;
                _password = password;
                writeToFile(username, password);
                Main.getInstance().successfulLogin(username);
                return true;
            }
            else {
                if (!auto)
                    Main.getInstance().getLoginForm().badLogin();
                return false;
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        return false;
    }

    public String getMarketplaceLocation()
    {
        try
        {
            URL oracle = new URL("https://www.unrealengine.com/marketplace");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("host", "accounts.unrealengine.com");
            conn.setInstanceFollowRedirects( false );
            conn.connect();
            if (conn.getResponseCode() == 302)
            {
                parseCookies(conn.getHeaderFields().get("Set-Cookie"));
                return conn.getHeaderFields().get("Location").get(0);
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        return null;
    }

    private void getStore()
    {
        try
        {
            URL oracle = new URL(getMarketplaceLocation());
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("host", "accounts.unrealengine.com");
            conn.setInstanceFollowRedirects( false );
            conn.connect();
            if (conn.getResponseCode() == 200) {
                parseCookies(conn.getHeaderFields().get("Set-Cookie"));
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    public boolean isLoggedIn()
    {
        getStore();
        try
        {
            URL oracle = new URL("https://www.unrealengine.com/marketplace/getAccountStatus");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("host", "accounts.unrealengine.com");
            conn.setInstanceFollowRedirects( false );
            conn.connect();
            if (conn.getResponseCode() == 200)
            {
                parseCookies(conn.getHeaderFields().get("Set-Cookie"));
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                JSONObject json = new JSONObject(content.toString());
                return json.getBoolean("isLoggedIn");
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        return false;
    }

    public void getAccountInfo()
    {
        try
        {
            URL oracle = new URL("https://www.unrealengine.com/marketplace/api/getAccountInfo");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setRequestProperty("Origin", "allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("host", "accounts.unrealengine.com");
            conn.setInstanceFollowRedirects( false );
            conn.connect();
            if (conn.getResponseCode() == 200)
            {
                parseCookies(conn.getHeaderFields().get("Set-Cookie"));
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                JSONObject json = new JSONObject(content.toString());
                _user = new User(json);
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    public boolean updateCategories(boolean startupCheck)
    {
        try
        {
            if (!startupCheck)
                Main.getInstance().getMainForm().setLoadingText("Loading categories data...");
            String data = "category=assets/recent&start=0";
            byte[] postData = data.getBytes( StandardCharsets.UTF_8 );
            int    postDataLength = postData.length;
            URL oracle = new URL("https://www.unrealengine.com/marketplace/assets/ajax-get-categories");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Host", "www.unrealengine.com");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0");
            conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            conn.setRequestProperty("Content-Length", postDataLength + "");
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setDoInput( true );
            conn.setDoOutput( true );
            conn.setRequestMethod("POST");
            try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                wr.write(postData);
            }
            conn.connect();
            if (startupCheck)
                return conn.getResponseCode() == 200;

            if (conn.getResponseCode() == 200)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                JSONObject json = new JSONObject(content.toString());

                JSONArray array = json.getJSONArray("categories");
                for (int i = 1; i < array.length(); i++)
                {
                    JSONObject element = array.getJSONObject(i);
                    Main.getInstance().createCategory(element.getString("path"), element.getString("name"));
                }
                Main.getInstance().updateCategoriesList();
                Main.getInstance().getMainForm().increaseLoadingBar(10);
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        return false;
    }

    public void getOwnedAssets()
    {
        Main.getInstance().clearOwnedAssets();
        try
        {
            Main.getInstance().getMainForm().setLoadingText("Loading owned assets...");
            URL oracle = new URL("https://launcher-public-service-prod06.ol.epicgames.com/launcher/api/public/assets/Windows?Label=live");
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Host", "www.unrealengine.com");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0");
            conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setDoInput( true );
            conn.connect();
            if (conn.getResponseCode() == 200)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                JSONArray json = new JSONArray(content.toString());

                for (int i = 0; i < json.length(); i++)
                {
                    JSONObject element = json.getJSONObject(i);

                    String catalogItemId = element.getString("catalogItemId");

                    EpicItem item = Main.getInstance().getItemByCatalogId(catalogItemId);

                    if (item == null)
                        continue;

                    EpicOwnedAsset ownedAsset;
                    if (Main.getInstance().containsOwnedAsset(catalogItemId))
                        ownedAsset = Main.getInstance().getOwnedAsset(catalogItemId);
                    else {
                        ownedAsset = new EpicOwnedAsset(catalogItemId, item);
                        Main.getInstance().addOwnedAsset(ownedAsset);
                    }

                    String appName = element.getString("appName");
                    String assetId = element.getString("assetId");
                    String buildVersion = element.getString("buildVersion");
                    EpicOwnedAssetRelease release = new EpicOwnedAssetRelease(appName, assetId, buildVersion);
                    ownedAsset.addRelease(release);
                }
                Main.getInstance().getMainForm().increaseLoadingBar(10);
                Main.getInstance().getMainForm().setLoadingText("Store loaded.");
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    private int _loadedPacksCount;
    public void generateItems()
    {
        int itemsCount = getItemsCount(false);
        int itemsPerLoad = 50;

        final int requestCount = itemsCount / itemsPerLoad + (itemsCount % itemsPerLoad > 0 ? 1 : 0);

        double perPart = 70.0 / requestCount;
        int threadsCount = 10;
        Thread threads[] = new Thread[threadsCount];
        _loadedPacksCount = 0;
        for (int t = 0; t < threadsCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                int perThread = requestCount / threadsCount;
                int max = (threadId + 1) * perThread;
                if (threadId + 1 == threadsCount)
                    max = requestCount;
                for (int i = threadId * perThread; i < max; i++) {
                    try {
                        String data = "count=" + itemsPerLoad + "&start=" + (i * itemsPerLoad);
                        URL oracle = new URL("https://catalog-public-service-prod06.ol.epicgames.com/catalog/api/shared/namespace/ue/offers?" + data);
                        HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
                        conn.setRequestProperty("Host", "www.unrealengine.com");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0");
                        conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
                        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                        conn.setRequestProperty("Authorization", "bearer " + _accessToken);
                        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
                        conn.setRequestProperty("Cookie", getCookiesString());
                        conn.setDoInput(true);
                        conn.connect();
                        if (conn.getResponseCode() == 200) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            String inputLine;
                            StringBuilder content = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) // Read File Line By Line
                            {
                                content.append(inputLine);
                            }
                            JSONObject json = new JSONObject(content.toString());

                            JSONArray pagingData = json.getJSONArray("elements");

                            for (int j = 0; j < pagingData.length(); j++) {
                                JSONObject element = pagingData.getJSONObject(j);

                                String name = element.has("title") ? element.getString("title") : "";
                                String description = element.has("description") ? element.getString("description") : "";
                                String longDescription = element.has("longDescription") ? element.getString("longDescription") : "";
                                String technicalDetails = element.has("technicalDetails") ? element.getString("technicalDetails") : "";

                                ArrayList<EpicCategory> categories = new ArrayList<>();
                                if (element.has("categories")) {
                                    JSONArray jsonCategories = element.getJSONArray("categories");
                                    for (int h = 0; h < jsonCategories.length(); h++) {
                                        EpicCategory category = Main.getInstance().getCategoryByPath(jsonCategories.getJSONObject(h).getString("path"));
                                        if (category == null)
                                            continue;
                                        categories.add(category);
                                    }
                                }

                                ArrayList<EpicImage> images = new ArrayList<>();
                                String thumbnail = "";
                                String urlPart = element.has("urlSlug") ? element.getString("urlSlug") : "";
                                String id = element.has("id") ? element.getString("id") : "";
                                String catalogId = "";
                                ArrayList<EpicItemReleaseInfo> releases = new ArrayList<>();
                                if (element.has("items"))
                                {
                                    JSONArray jsonItems = element.getJSONArray("items");
                                    JSONObject itemElement = jsonItems.getJSONObject(0);

                                    if (itemElement.has("keyImages")) {
                                        JSONArray jsonImages = itemElement.getJSONArray("keyImages");
                                        for (int h = 0; h < jsonImages.length(); h++) {
                                            JSONObject jsonImage = jsonImages.getJSONObject(h);
                                            String type = jsonImage.getString("type");
                                            if (type.equalsIgnoreCase("Thumbnail"))
                                                thumbnail = jsonImage.getString("url");
                                            if (!type.equalsIgnoreCase("Screenshot"))
                                                continue;
                                            String url = jsonImage.getString("url");
                                            int width = jsonImage.getInt("width");
                                            int height = jsonImage.getInt("height");
                                            images.add(new EpicImage(url, width, height));
                                        }
                                    }

                                    if (itemElement.has("title"))
                                        name = itemElement.getString("title");
                                    if (itemElement.has("description"))
                                        description = itemElement.getString("description");
                                    if (itemElement.has("longDescription"))
                                        longDescription = itemElement.getString("longDescription");
                                    if (itemElement.has("technicalDetails"))
                                        technicalDetails = itemElement.getString("technicalDetails");

                                    catalogId = itemElement.has("id") ? itemElement.getString("id") : "";
                                    if (itemElement.has("releaseInfo"))
                                    {
                                        JSONArray jsonReleases = itemElement.getJSONArray("releaseInfo");
                                        for (int h = 0; h < jsonReleases.length(); h++)
                                        {
                                            JSONObject releaseElement = jsonReleases.getJSONObject(h);
                                            String releaseId = releaseElement.has("id") ? releaseElement.getString("id") : "";
                                            String releaseAppId = releaseElement.has("appId") ? releaseElement.getString("appId") : "";
                                            ArrayList<String> compatibilities = new ArrayList<>();
                                            ArrayList<String> platforms = new ArrayList<>();
                                            if (releaseElement.has("compatibleApps"))
                                            {
                                                JSONArray compatibilityJSON = releaseElement.getJSONArray("compatibleApps");
                                                for (int k = 0; k < compatibilityJSON.length(); k++)
                                                    compatibilities.add(compatibilityJSON.getString(k));
                                            }
                                            if (releaseElement.has("platform"))
                                            {
                                                JSONArray platformJSON = releaseElement.getJSONArray("platform");
                                                for (int k = 0; k < platformJSON.length(); k++)
                                                    platforms.add(platformJSON.getString(k));
                                            }
                                            releases.add(new EpicItemReleaseInfo(releaseId, releaseAppId, compatibilities, platforms));
                                        }
                                    }
                                }
                                double price = element.has("price") ? element.getInt("price") / 100.0 : 0;
                                String sellerName = element.has("seller") ? element.getJSONObject("seller").getString("name") : "";
                                EpicItem epicItem = new EpicItem(id, catalogId, name, description, longDescription, technicalDetails, categories, images, urlPart, thumbnail, price, sellerName, releases);
                                for (EpicCategory category : categories)
                                    category.addItem(epicItem);
                                Main.getInstance().addItem(epicItem);
                            }
                            increaseLoadedPacksCount(perPart, requestCount);
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            });
            threads[t].start();
        }
        for (int i = 0; i < threadsCount; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void increaseLoadedPacksCount(double perPart, int requestCount)
    {
        _loadedPacksCount++;
        Main.getInstance().getMainForm().increaseLoadingBar(perPart);
        Main.getInstance().getMainForm().setLoadingText("Loading items... [" + _loadedPacksCount + " / " + requestCount + "]");
    }

    private int getItemsCount(boolean autoLogin)
    {
        try
        {
            if (!autoLogin)
                Main.getInstance().getMainForm().setLoadingText("Loading items count...");
            String data = "count=1";
            URL oracle = new URL("https://catalog-public-service-prod06.ol.epicgames.com/catalog/api/shared/namespace/ue/offers?" + data);
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Host", "www.unrealengine.com");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0");
            conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setDoInput( true );
            conn.connect();
            if (autoLogin)
                return conn.getResponseCode() == 200 ? 1 : 0;
            if (conn.getResponseCode() == 200)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                JSONObject json = new JSONObject(content.toString());

                JSONObject pagingData = json.getJSONObject("paging");

                Main.getInstance().getMainForm().increaseLoadingBar(10);
                return pagingData.getInt("total");
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        return -1;
    }

    public void downloadItem(EpicItem item)
    {
        try
        {
            Main.getInstance().getDownloadForm().setMainInfoText("Downloading item info...");
            String data = item.getCatalogItemId() + "/" + item.getAppNameByRev(Main.getInstance().getEngineVersion());
            URL oracle = new URL("https://launcher-public-service-prod06.ol.epicgames.com/launcher/api/public/assets/Windows/" + data);
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Host", "www.unrealengine.com");
            conn.setRequestProperty("User-Agent", "game=UELauncher, engine=UE4, build=allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setDoInput( true );
            conn.connect();
            if (conn.getResponseCode() == 200)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                if (isStopDownload())
                    return;
                JSONObject itemBuildInfoJSON = new JSONObject(content.toString());
                String distribution = itemBuildInfoJSON.getJSONObject("items").getJSONObject("MANIFEST").getString("distribution");
                String path = itemBuildInfoJSON.getJSONObject("items").getJSONObject("MANIFEST").getString("path");
                Main.getInstance().getDownloadForm().increase2Progress(5);
                getItemManifest(distribution, path);
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    public void getItemManifest(String distribution, String path)
    {
        try
        {
            Main.getInstance().getDownloadForm().setMainInfoText("Downloading item manifest...");
            URL oracle = new URL(distribution + path);
            HttpURLConnection conn = (HttpURLConnection) oracle.openConnection();
            conn.setRequestProperty("Host", "www.unrealengine.com");
            conn.setRequestProperty("User-Agent", "game=UELauncher, engine=UE4, build=allar_ue4_marketplace_commandline");
            conn.setRequestProperty("Accept", "*/*");
            //conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Authorization", "bearer " + _accessToken);
            conn.setRequestProperty("Cookie", getCookiesString());
            conn.setDoInput( true );
            conn.setInstanceFollowRedirects( false );
            conn.connect();
            if (conn.getResponseCode() == 200)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) // Read File Line By Line
                {
                    content.append(inputLine);
                }
                if (isStopDownload())
                    return;
                JSONObject manifestJSON = new JSONObject(content.toString());
                String appName = manifestJSON.getString("AppNameString");
                Main.getInstance().getDownloadForm().increase2Progress(5);
                if (isStopDownload())
                    return;
                downloadItemChunks(appName, manifestJSON.getJSONObject("ChunkHashList"), manifestJSON.getJSONObject("DataGroupList"));
                if (isStopDownload())
                    return;
                extractFiles(appName, manifestJSON.getJSONArray("FileManifestList"));
            }
            else {
                Main.getInstance().getMainForm().finishDownload(null, false);
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    String _hexChars[] = new String[] {"0", "1", "2", "3", "4", "5", "6", "7","8", "9", "A", "B", "C", "D", "E", "F"};
    private boolean _stopDownload = false;

    public boolean isStopDownload() {
        return _stopDownload;
    }

    public void setStopDownload(boolean stopDownload) {
        _stopDownload = stopDownload;
    }

    private String byteToHex(int b)
    {
        return _hexChars[(b >> 4) & 0x0f] + _hexChars[b & 0x0f];
    }

    private String chunkHashToReverseHexEncoding(String chunkHash)
    {
        String result = "";
        for (int i = 0; i < (chunkHash.length() / 3 < 8 ? chunkHash.length() / 3 : 8); ++i)
        {
            try {
                result = byteToHex(Integer.parseInt(chunkHash.substring(i * 3, i * 3 + 3))) + result;
            }
            catch (StringIndexOutOfBoundsException ss)
            {
                ss.printStackTrace();
            }
        }
        return result;
    }

    private int _chunksDownloaded;
    private int _chunksCount;


    public void downloadItemChunks(String appName, JSONObject chunkHashList, JSONObject dataGroupList)
    {
        try {
            _chunksDownloaded = 0;
            ArrayList<String[]> data = new ArrayList<>();
            JSONArray chunkHashNames = chunkHashList.names();
            String chunkBaseURL = "http://download.epicgames.com/Builds/Rocket/Automated/" + appName + "/CloudDir/ChunksV3/";
            _chunksCount = chunkHashNames.length();
            Main.getInstance().getDownloadForm().setMainInfoText("Downloaded chunks [" + _chunksDownloaded + " / " + _chunksCount + "]...");

            for (int i = 0; i < chunkHashNames.length(); i++) {
                String name = chunkHashNames.getString(i);
                String result = chunkHashList.getString(name);


                String hash = chunkHashToReverseHexEncoding(result);
                String group = String.format("%02d", Integer.parseInt(dataGroupList.getString(name)));
                String fileName = name + ".chunk";
                data.add(new String[] {fileName, chunkBaseURL + group + "/" + hash + "_" + name + ".chunk"});
                if (isStopDownload())
                    return;
            }

            // Make folders if required
            File testFile = new File ("." + appName + "/chunks/");
            if (!testFile.exists())
                testFile.mkdirs();

            int threadsCount = 10;
            Thread threads[] = new Thread[threadsCount];
            for (int i = 0; i < threadsCount; i++)
            {
                int threadId = i;
                threads[i] = new Thread(() -> {
                    int perThread = data.size() / threadsCount;
                    int max = (threadId + 1) * perThread;
                    if (threadId + 1 == threadsCount)
                        max = data.size();
                    for (int h = threadId * perThread; h < max; h++) {
                        if (isStopDownload())
                            break;
                        try {
                            File file = new File("." + appName + "/chunks/" + data.get(h)[0]);
                            if (file.exists())
                                file.delete();
                            saveUrl("." + appName + "/chunks/" + data.get(h)[0], data.get(h)[1]);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                threads[i].start();
            }
            for (int i = 0; i < threadsCount; i++)
            {
                if (isStopDownload()) {
                    threads[i].stop();
                    continue;
                }
                threads[i].join();
            }
            Main.getInstance().getDownloadForm().clear1Progress();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void extractFiles(String appName, JSONArray fileManifestList)
    {
        if (isStopDownload())
            return;
        try {
            int chunksCount = 0;
            int chunksReaded = 0;
            Main.getInstance().getDownloadForm().setMainInfoText("Reading chunks...");
            File dir = new File("." + appName + "/chunks/");
            if (!dir.exists() || !dir.isDirectory())
                return;
            if (dir.listFiles() == null)
                return;
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isDirectory())
                    continue;
                if (isStopDownload())
                    return;
                double chunkPercent = 100.0 / dir.listFiles().length;
                double wholePercent = 30.0 / dir.listFiles().length;
                String fileName = file.getName();
                File newFile = new File("." + appName + "/reworked_chunks/" + fileName);
                if (newFile.exists())
                    newFile.delete();
                else
                    newFile.getParentFile().mkdirs();
                Path path = Paths.get(file.getAbsolutePath());
                byte[] data = Files.readAllBytes(path);


                byte[] headerData = new byte[41];

                for (int i = 0; i < 41; i++)
                    headerData[i] = data[i];

                int headerSize = headerData[8];
                boolean compressed = headerData[40] == 1;

                byte[] realFileData = new byte[data.length - headerSize];

                int h = 0;
                for (int i = headerSize; i < data.length; i++) {
                    realFileData[h] = data[i];
                    h++;
                }

                if (compressed)
                {
                    try (FileOutputStream fos = new FileOutputStream("." + appName + "/reworked_chunks/" + fileName)) {
                        fos.write(decompress(realFileData));
                    }
                }
                else {
                    try (FileOutputStream fos = new FileOutputStream("." + appName + "/reworked_chunks/" + fileName)) {
                        fos.write(realFileData);
                    }
                }
                Main.getInstance().getDownloadForm().setMainInfoText("Readed chunks [" + chunksCount + " / " + dir.listFiles().length + "]...");
                chunksCount++;
                Main.getInstance().getDownloadForm().increase1Progress(chunkPercent);
                Main.getInstance().getDownloadForm().increase2Progress(wholePercent);
                file.deleteOnExit();
            }
            deleteFolder(dir);
            Main.getInstance().getDownloadForm().clear1Progress();

            for (int i = 0; i < fileManifestList.length(); i++)
            {
                double chunkPercent = 100.0 / fileManifestList.length();
                double wholePercent = 30.0 / fileManifestList.length();
                if (isStopDownload())
                    return;
                JSONObject fileManifest = fileManifestList.getJSONObject(i);
                int fileSize = 0;
                String fileName = Main.getInstance().getProjects().get(Main.getInstance().getCurrentProject()) + fileManifest.getString("Filename");
                File file = new File(fileName);
                if (!file.exists())
                    file.getParentFile().mkdirs();
                else
                    file.delete();

                JSONArray chunkParts = fileManifest.getJSONArray("FileChunkParts");

                for (int h = 0; h < chunkParts.length(); h++)
                {
                    JSONObject chunk = chunkParts.getJSONObject(h);
                    fileSize += Integer.decode("0x" + chunkHashToReverseHexEncoding(chunk.getString("Size")));
                }

                byte buffer[] = new byte[fileSize];
                int loc = 0;
                for (int h = 0; h < chunkParts.length(); h++) {
                    if (isStopDownload())
                        return;
                    JSONObject chunk = chunkParts.getJSONObject(h);
                    String chunkGUID = chunk.getString("Guid");
                    int chunkOffset = Integer.decode("0x" + chunkHashToReverseHexEncoding(chunk.getString("Offset")));
                    int chunkSize = Integer.decode("0x" + chunkHashToReverseHexEncoding(chunk.getString("Size")));
                    Path path = Paths.get("." + appName + "/reworked_chunks/" + chunkGUID + ".chunk");
                    byte[] data = Files.readAllBytes(path);

                    for (int c = chunkOffset; c < chunkOffset + chunkSize; c++)
                    {
                        buffer[loc] = data[c];
                        loc++;
                    }
                }
                try (FileOutputStream fos = new FileOutputStream(file.getAbsolutePath())) {
                    fos.write(buffer);
                }
                Main.getInstance().getDownloadForm().setMainInfoText("Extracted data [" + (i + 1) + " / " + fileManifestList.length() + "]...");
                Main.getInstance().getDownloadForm().increase1Progress(chunkPercent);
                Main.getInstance().getDownloadForm().increase2Progress(wholePercent);
            }
            deleteFolder(new File("." + appName));
            Main.getInstance().getMainForm().finishDownload(appName, true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void saveUrl(final String filename, final String urlString)
            throws MalformedURLException, IOException {
        if (isStopDownload())
            return;
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(new URL(urlString).openStream());
            fout = new FileOutputStream(filename);

            final byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
            _chunksDownloaded++;
            Main.getInstance().getDownloadForm().setMainInfoText("Downloaded chunks [" + _chunksDownloaded + " / " + _chunksCount + "]...");
            double chunkPercent = 100.0 / _chunksCount;
            double wholePercent = 30.0 / _chunksCount;
            Main.getInstance().getDownloadForm().increase1Progress(chunkPercent);
            Main.getInstance().getDownloadForm().increase2Progress(wholePercent);
        } finally {
            if (in != null) {
                in.close();
            }
            if (fout != null) {
                fout.close();
            }
        }
    }

    public byte[] decompress(byte[] bytesToDecompress)
    {
        byte[] returnValues = null;

        Inflater inflater = new Inflater();

        int numberOfBytesToDecompress = bytesToDecompress.length;

        inflater.setInput
                (
                        bytesToDecompress,
                        0,
                        numberOfBytesToDecompress
                );

        int bufferSizeInBytes = numberOfBytesToDecompress;

        int numberOfBytesDecompressedSoFar = 0;
        List<Byte> bytesDecompressedSoFar = new ArrayList<Byte>();

        try
        {
            while (inflater.needsInput() == false)
            {
                byte[] bytesDecompressedBuffer = new byte[bufferSizeInBytes];

                int numberOfBytesDecompressedThisTime = inflater.inflate
                        (
                                bytesDecompressedBuffer
                        );

                numberOfBytesDecompressedSoFar += numberOfBytesDecompressedThisTime;

                for (int b = 0; b < numberOfBytesDecompressedThisTime; b++)
                {
                    bytesDecompressedSoFar.add(bytesDecompressedBuffer[b]);
                }
            }

            returnValues = new byte[bytesDecompressedSoFar.size()];
            for (int b = 0; b < returnValues.length; b++)
            {
                returnValues[b] = (byte)(bytesDecompressedSoFar.get(b));
            }

        }
        catch (DataFormatException dfe)
        {
            dfe.printStackTrace();
        }

        inflater.end();

        return returnValues;
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    private String getQuery(Map<String, String> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> pair : params.entrySet())
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private String getCookiesString()
    {
        StringBuilder cookies = new StringBuilder();
        for (Map.Entry<String, String> entry : _cookies.entrySet())
        {
            cookies.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
        }
        return cookies.toString();
    }

    public String getUsername() {
        return _username;
    }

    public String getPassword() {
        return _password;
    }
}
