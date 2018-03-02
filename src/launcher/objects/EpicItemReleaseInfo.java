package launcher.objects;

import java.util.ArrayList;
import java.util.HashMap;

public class EpicItemReleaseInfo {
    private String _id;
    private String _appId;
    private HashMap<Double, String> _compatibility;
    private ArrayList<String> _platforms;
    private double _lowestVersion;
    private double _highestVersion;

    public EpicItemReleaseInfo(String id, String appId, ArrayList<String> compatibility, ArrayList<String> platforms)
    {
        _id = id;
        _appId = appId;
        _compatibility = new HashMap<>();
        int i = 0;
        for (String comp : compatibility)
        {
            String ver = comp.substring(3);
            String origVersion = comp.substring(3);
            if (ver.length() == 3)
                ver = ver.split("\\.")[0] + ".0" + ver.split("\\.")[1];
            double version = Double.parseDouble(ver);
            if (i == 0) {
                _lowestVersion = version;
                _highestVersion = version;
            }
            else {
                if (_lowestVersion > version)
                    _lowestVersion = version;
                if (_highestVersion < version)
                    _highestVersion = version;
            }
            _compatibility.put(version, origVersion);
            i++;
        }
        _platforms = platforms;
    }

    public String getId() {
        return _id;
    }

    public String getAppId() {
        return _appId;
    }

    public HashMap<Double, String> getCompatibility() {
        return _compatibility;
    }

    public double getLowestVersion() {
        return _lowestVersion;
    }

    public double getHighestVersion() {
        return _highestVersion;
    }

    public ArrayList<String> getPlatforms() {
        return _platforms;
    }
}
