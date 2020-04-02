package launcher.objects;

import launcher.DownloadForm;
import launcher.managers.SessionManager;
import org.json.JSONObject;

import java.io.IOException;

public class EpicItemMeta {
    private EpicItem item;
    private String unrealVersion;

    private long totalSize = 0;

    private JSONObject itemInfo_root, itemInfo_items, itemInfo_manifest;
    private JSONObject manifest_root;
    private String manifest_appName, signature, distribution, path;

    EpicItemMeta(EpicItem epicItem, String ueVersion) throws EpicItem.EpicItemException {
        item = epicItem;
        unrealVersion = ueVersion;

        fetchItemInfo();
        fetchItemManifest();
    }

    public String get_manifest_appName(){
        return manifest_appName;
    }

    public String getSignature(){
        return signature;
    }

    public String getPath(){
        return path;
    }

    public String getDistribution(){
        return distribution;
    }

    public JSONObject getManifest_root(){
        return manifest_root;
    }

    public long getTotalSize(){
        return totalSize;
    }


    private void fetchItemManifest() throws EpicItem.EpicItemException {
        try {
            manifest_root = new JSONObject(downloadItemManifest(distribution, path, signature));
        } catch(EpicItem.EpicItemException ex){
            throw new EpicItem.EpicItemException(ex.getMessage());
        }

        // sanity checks
        if (!manifest_root.has("AppNameString")) {
            throw new EpicItem.EpicItemException("Failed to parse item manifest...");
        }

        if (!manifest_root.has("ChunkHashList") || !manifest_root.has("DataGroupList") || !manifest_root.has("ChunkFilesizeList")) {
            throw new EpicItem.EpicItemException("Failed to parse item manifest...");
        }

        if (!manifest_root.has("FileManifestList")) {
            throw new EpicItem.EpicItemException("Failed to parse item manifest...");
        }

        // parse data
        manifest_appName = manifest_root.getString("AppNameString");

        totalSize = 0;
        JSONObject filesizeList = manifest_root.getJSONObject("ChunkFilesizeList");
        for (Object entry : filesizeList.toMap().values()) {
            if (!(entry instanceof String))
                continue;
            String value = (String) entry;
            totalSize += Integer.parseInt(EpicItem.reverseHexEncoding(value), 16);
        }
    }

    private void fetchItemInfo() throws EpicItem.EpicItemException {
        try {
            itemInfo_root = new JSONObject(downloadItemInfo());
        } catch(EpicItem.EpicItemException ex){
            throw new EpicItem.EpicItemException("Failed to download item info...");
        }

        // sanity checks
        if (!itemInfo_root.has("items")) {
            throw new EpicItem.EpicItemException("Failed to download item info, missing items node...");
        }

        itemInfo_items = itemInfo_root.getJSONObject("items");
        if (!itemInfo_items.has("MANIFEST")) {
            throw new EpicItem.EpicItemException("Failed to download item info, missing MANIFEST node...");
        }

        itemInfo_manifest = itemInfo_items.getJSONObject("MANIFEST");
        if (!itemInfo_manifest.has("distribution") || !itemInfo_manifest.has("path") || !itemInfo_manifest.has("signature")) {
            throw new EpicItem.EpicItemException("Failed to download item info...");
        }

        // parse data
        distribution = itemInfo_manifest.getString("distribution");
        path = itemInfo_manifest.getString("path");
        signature = itemInfo_manifest.getString("signature");
    }

    private String downloadItemManifest(String distribution, String path, String signature) throws EpicItem.EpicItemException {
        try {
            DownloadForm.getInstance().setMainProgressText("Downloading item manifest...");
            Request request = new Request(distribution + path + "?" + signature);
            if (!request.execute(200)) {
                System.out.println(request);
                throw new EpicItem.EpicItemException("Failed to download item manifest...");
            }

            return request.getContent();

        } catch (IOException ex){
            ex.printStackTrace();
            throw new EpicItem.EpicItemException("Failed to download item manifest...");
        }
    }


    private String downloadItemInfo() throws EpicItem.EpicItemException {
        try {
            Request request = new Request("https://launcher-public-service-prod06.ol.epicgames.com/launcher/api/public/assets/Windows/" + item.getCatalogItemId() + "/" + unrealVersion);
            request.assignParameter("label", "Live");
            request.assignCookies(SessionManager.getInstance().getSession());
            request.assignBearer();
            if (!request.execute(200)) {
                System.out.println(request);
                throw new EpicItem.EpicItemException("request execution failed");
            }

            return request.getContent();

        } catch(Exception ex){
            throw new EpicItem.EpicItemException("request failed");
        }
    }
}
