package launcher.objects;

import java.util.HashMap;

public class EpicOwnedAsset {
	private String _catalogItemId;
	private EpicItem _item;
	private HashMap<String, EpicOwnedAssetRelease> _releases;

	public EpicOwnedAsset(String catalogItemId, EpicItem item) {
		_catalogItemId = catalogItemId;
		_item = item;
		_releases = new HashMap<>();
	}

	public void addRelease(EpicOwnedAssetRelease release) {
		_releases.put(release.getUEVersion(), release);
	}

	public String getCatalogItemId() {
		return _catalogItemId;
	}

	public EpicItem getItem() {
		return _item;
	}

	public HashMap<String, EpicOwnedAssetRelease> getReleases() {
		return _releases;
	}
}
