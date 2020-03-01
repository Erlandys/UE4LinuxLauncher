package launcher.managers;

import launcher.DownloadForm;
import launcher.MainForm;
import launcher.objects.EpicCategory;
import launcher.objects.EpicItem;
import launcher.objects.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketplaceManager {
	private static String UNREAL_ENGINE_ASSETS = "https://www.unrealengine.com/marketplace/api/assets";
	static String UNREAL_ENGINE_OWNED_ASSETS = "https://www.unrealengine.com/marketplace/api/assets/vault";

	final int ITEMS_PER_DOWNLOAD = 20;

	private int _requestsCount;
	private int _itemsCount;
	private AtomicInteger _requestNumber;

	private Map<String, EpicItem> _marketplaceItems;
	private Map<String, EpicCategory> _marketplaceCategories;

	private MarketplaceManager() {
		_marketplaceItems = new HashMap<>();
		_marketplaceCategories = new HashMap<>();
		//noinspection ResultOfMethodCallIgnored
		DownloadForm.getInstance();
	}

	public void createMarketplace() {
		LocalMarketplaceManager.getInstance().loadMarketplace();
	}

	private void downloadItems(int start, int count) {
		try {
			Request request = new Request(UNREAL_ENGINE_ASSETS);
			request.assignParameter("start", start);
			request.assignParameter("count", count);
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			if (!request.execute(200)) {
				// TODO: Handle error
				System.out.println(request);
				return;
			}
			JSONObject root = new JSONObject(request.getContent());
			JSONObject data = root.getJSONObject("data");
			JSONArray elements = data.getJSONArray("elements");
			for (int i = 0; i < elements.length(); i++) {
				JSONObject itemObject = elements.getJSONObject(i);
				if (!itemObject.has("status"))
					continue;
				if (!itemObject.getString("status").equalsIgnoreCase("ACTIVE"))
					continue;
				EpicItem item = new EpicItem(itemObject);
				_marketplaceItems.put(item.getCatalogItemId(), item);
			}
//			increaseLoadedPacksCount();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadNewMarketplace() {
		int itemsCount = getItemsCount();
		final int itemsPerDownload = 20;
		_requestsCount = (int) Math.ceil(itemsCount / (double) itemsPerDownload);
		ExecutorService _service = Executors.newFixedThreadPool(10);
		for (int i = 0; i < _requestsCount; i++) {
			final int start = i * itemsPerDownload;
			final int count = Math.min(start + itemsPerDownload, itemsCount) - start;
			_service.execute(() -> downloadItems(start, count));
		}
		_service.shutdown();
		while (!_service.isTerminated()) {
		}
	}

	public int getItemsCount() {
		int result = -1;
		try {
			Request request = new Request(UNREAL_ENGINE_ASSETS);
			request.assignParameter("count", 1);
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignCookies(SessionManager.getInstance().getSession());
			if (!request.execute(200)) {
				// TODO: Handle...
				return result;
			}
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			JSONObject json = new JSONObject(request.getContent());
			JSONObject data = json.getJSONObject("data");
			JSONObject pagingData = data.getJSONObject("paging");
			MainForm.getInstance().increaseLoadingBar(10);
			result = pagingData.getInt("total");
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	public void loadOwnedAssets() {
		_requestNumber = new AtomicInteger(0);
		MainForm.getInstance().setLoadingText("Loading owned assets...");
		try {
			getOwnedAssetsCount();
		}
		catch (RuntimeException re) {
			MainForm.getInstance().setLoadingText(re.getMessage(), true);
		}
		ConcurrentLinkedQueue<String> items = new ConcurrentLinkedQueue<>();
		getOwnedAssetsList(items);
		getDetailedInformation(items);
	}

	private void getOwnedAssetsList(ConcurrentLinkedQueue<String> items) {
		int requestsCount = (int) Math.ceil(_itemsCount / (double) ITEMS_PER_DOWNLOAD);
		ExecutorService threadPool = Executors.newWorkStealingPool();
		for (int i = 0; i < requestsCount; i++) {
			int start = i * ITEMS_PER_DOWNLOAD;
			int end = Math.min((i + 1) * ITEMS_PER_DOWNLOAD, _itemsCount);
			threadPool.execute(() -> downloadOwnedItemsList(start, end, items));
		}
		threadPool.shutdown();
		while (!threadPool.isTerminated()) {
		}
	}

	private void getDetailedInformation(ConcurrentLinkedQueue<String> items) {
		ExecutorService threadPool = Executors.newWorkStealingPool();
		for (String catalogItemId : items) {
			threadPool.execute(() -> downloadItem(catalogItemId));
		}
		threadPool.shutdown();
		while (!threadPool.isTerminated()) {
		}
	}

	private void downloadItem(String catalogItemId) {
		EpicItem item = getItem(catalogItemId);
		if (item != null) {
			item.removeFromDatabase();
			_marketplaceItems.remove(catalogItemId);
		}

		try {
			Request request = new Request("https://www.unrealengine.com/marketplace/api/assets/item/" + catalogItemId);
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignBearer();
			if (!request.execute(200)) {
				System.out.println(request);
				throw new RuntimeException("Failed to load owned assets. #1");
			}
			updateLoadingBar();
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("status"))
				throw new RuntimeException("Failed to load owned assets. #2");
			if (!root.getString("status").equalsIgnoreCase("ok"))
				throw new RuntimeException("Failed to load owned assets. #3");
			if (!root.has("data"))
				throw new RuntimeException("Failed to load owned assets. #4");
			JSONObject data = root.getJSONObject("data");
			if (!data.has("data"))
				throw new RuntimeException("Failed to load owned assets. #4");
			JSONObject innerData = data.getJSONObject("data");
			item = new EpicItem(innerData);
			item.save();
			_marketplaceItems.put(catalogItemId, item);
			SessionManager.getInstance().getUser().addOwnedItem(item);
		}
		catch (IOException exception) {
			throw new RuntimeException("Failed to load owned assets. #7");
		}
	}

	private void downloadOwnedItemsList(int start, int end, ConcurrentLinkedQueue<String> items) {
		try {
			Request request = new Request(UNREAL_ENGINE_OWNED_ASSETS);
			request.assignParameter("start", start);
			request.assignParameter("count", end);
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignBearer();
			if (!request.execute(200)) {
				System.out.println(request);
				throw new RuntimeException("Failed to load owned assets. #1");
			}
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			updateLoadingBar();
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("status"))
				throw new RuntimeException("Failed to load owned assets. #2");
			if (!root.getString("status").equalsIgnoreCase("ok"))
				throw new RuntimeException("Failed to load owned assets. #3");
			if (!root.has("data"))
				throw new RuntimeException("Failed to load owned assets. #4");
			JSONObject data = root.getJSONObject("data");
			if (!data.has("elements"))
				throw new RuntimeException("Failed to load owned assets. #5");
			JSONArray elements = data.getJSONArray("elements");
			for (int i = 0; i < elements.length(); i++) {
				JSONObject itemObject = elements.getJSONObject(i);
				if (!itemObject.has("catalogItemId"))
					continue;
				items.add(itemObject.getString("catalogItemId"));
			}
		}
		catch (IOException exception) {
			throw new RuntimeException("Failed to load owned assets. #7");
		}
	}

	private void updateLoadingBar() {
		MainForm.getInstance().increaseLoadingBar(_requestNumber.incrementAndGet() * 100.0 / _requestsCount);
	}

	void getOwnedAssetsCount() {
		try {
			Request request = new Request(UNREAL_ENGINE_OWNED_ASSETS);
			request.assignParameter("start", 0);
			request.assignParameter("count", 0);
			request.assignXSRF(SessionManager.getInstance().getSession().getXSRF());
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignBearer();
			if (!request.execute(200)) {
				System.out.println(request);
				throw new RuntimeException("Failed to load owned assets. #1");
			}
			SessionManager.getInstance().getSession().setCookies(request.getCookies());
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("status"))
				throw new RuntimeException("Failed to load owned assets. #2");
			if (!root.getString("status").equalsIgnoreCase("ok"))
				throw new RuntimeException("Failed to load owned assets. #3");
			if (!root.has("data"))
				throw new RuntimeException("Failed to load owned assets. #4");
			JSONObject data = root.getJSONObject("data");
			if (!data.has("paging"))
				throw new RuntimeException("Failed to load owned assets. #5");
			JSONObject paging = data.getJSONObject("paging");
			if (!paging.has("total"))
				throw new RuntimeException("Failed to load owned assets. #6");
			_itemsCount = paging.getInt("total");
			_requestsCount = (int) Math.ceil(_itemsCount / (double) ITEMS_PER_DOWNLOAD) + 1 + _itemsCount;
			updateLoadingBar();
		}
		catch (IOException exception) {
			throw new RuntimeException("Failed to load owned assets. #7");
		}
	}

	public EpicCategory getCategory(String name) {
		return _marketplaceCategories.getOrDefault(name, null);
	}

	public EpicCategory createCategory(String name, String path) {
		return createCategory(0, name, path);
	}

	public EpicCategory createCategory(int id, String name, String path) {
		EpicCategory category = new EpicCategory(id, path, name);
		_marketplaceCategories.put(name, category);
		MainForm.getInstance().updateCategoriesList(_marketplaceCategories.keySet());
		return category;
	}

	public EpicItem getItem(String catalogItemId) {
		return _marketplaceItems.getOrDefault(catalogItemId, null);
	}

	public Collection<EpicItem> getItems() {
		return _marketplaceItems.values();
	}

	public void addItem(EpicItem item) {
		_marketplaceItems.put(item.getCatalogItemId(), item);
	}

	public static MarketplaceManager getInstance() {
		return SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final MarketplaceManager _instance = new MarketplaceManager();
	}
}
