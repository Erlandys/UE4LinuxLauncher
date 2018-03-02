package launcher.objects;

import java.util.ArrayList;
import java.util.HashMap;

public class EpicItem {
    private String _itemId;
    private String _catalogItemId;
    private String _name;
    private String _description;
    private String _longDescription;
    private String _technicalDetails;
    private ArrayList<EpicCategory> _categories;
    private ArrayList<EpicImage> _images;
    private String _urlPart;
    private String _thumbnail;
    private double _price;
    private String _sellerName;
    private ArrayList<EpicItemReleaseInfo> _releases;
    private HashMap<Double, String> _compatibility;

    public EpicItem(String itemId, String catalogItemId, String name, String description, String longDescription, String technicalDetails, ArrayList<EpicCategory> categories, ArrayList<EpicImage> images, String urlPart, String thumbnail, double price, String sellerName, ArrayList<EpicItemReleaseInfo> releases)
    {
        _itemId = itemId;
        _catalogItemId = catalogItemId;
        _name = name;
        _description = description;
        _longDescription = longDescription;
        _technicalDetails = technicalDetails;
        _categories = categories;
        _images = images;
        _urlPart = urlPart;
        _thumbnail = thumbnail;
        _price = price;
        _sellerName = sellerName;
        _releases = releases;
        _compatibility = new HashMap<>();
        for (EpicItemReleaseInfo release : _releases)
        {
            if (release == null)
                continue;
            String appName = release.getAppId();
            for (double compatibility : release.getCompatibility().keySet())
            {
                _compatibility.put(compatibility, appName);
            }
        }
    }

    public String getItemId()
    {
        return _itemId;
    }

    public String getName()
    {
        return _name;
    }

    public String getDescription()
    {
        return _description;
    }

    public String getLongDescription()
    {
        return _longDescription;
    }

    public String getTechnicalDetails() {
        return _technicalDetails;
    }

    public ArrayList<EpicCategory> getCategories() {
        return _categories;
    }

    public ArrayList<EpicImage> getImages() {
        return _images;
    }

    public String getUrlPart() {
        return _urlPart;
    }

    public String getThumbnail() {
        return _thumbnail;
    }

    public double getPrice() {
        return _price;
    }

    public String getSellerName() {
        return _sellerName;
    }

    public String getCatalogItemId() {
        return _catalogItemId;
    }

    public ArrayList<EpicItemReleaseInfo> getReleases() {
        return _releases;
    }

    public String getAppNameByRev(double version)
    {
        return _compatibility.get(version);
    }

    public boolean isCompatible(double version)
    {
        return _compatibility.containsKey(version);
    }
}
