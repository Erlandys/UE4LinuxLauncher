package launcher.objects;

public class EpicOwnedAssetRelease {
    private String _name;
    private String _assetId;
    private String _version;

    public EpicOwnedAssetRelease(String name, String assetId, String buildVersion)
    {
        _name = name;
        _assetId = assetId;
        String data[] = buildVersion.split("\\.");
        _version = data[0] + "." + data[1];
    }

    public String getName() {
        return _name;
    }

    public String getAssetId() {
        return _assetId;
    }

    public String getUEVersion()
    {
        return _version;
    }
}
