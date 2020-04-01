package launcher.objects;

import launcher.DownloadForm;
import launcher.managers.DatabaseManager;
import launcher.managers.MarketplaceManager;
import launcher.managers.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class EpicItem {
	private static ReentrantLock LOCK = new ReentrantLock();
	private int _id;
	private String _itemId;
	private String _catalogItemId;
	private String _name;
	private String _description;
	private String _longDescription;
	private String _technicalDetails;
	private ArrayList<EpicCategory> _categories = new ArrayList<>();
	private ArrayList<EpicImage> _images = new ArrayList<>();
	private HashMap<String, ArrayList<Long>> _downloads = new HashMap<>();
	private String _urlPart;
	private double _price;
	private double _discountPrice;
	private int _discountPercent;
	private String _sellerName;
	private List<EpicItemReleaseInfo> _releases = new LinkedList<>();
	private HashMap<Double, String> _compatibility = new HashMap<>();
	private EpicImage _featured;
	private EpicImage _thumbnail;
	private EpicImage _learnThumbnail;
	private EpicImage _headerImage;

	private boolean _isOwned;

	public EpicItem(JSONObject object) {
		_itemId = object.has("id") ? object.getString("id") : "";
		_catalogItemId = object.has("catalogItemId") ? object.getString("catalogItemId") : "";
		_name = object.has("title") ? object.getString("title") : "Unknown";
		_price = object.has("price") && !object.isNull("price") ? object.getInt("price") / 100.0 : 0;
		_discountPrice = object.has("discountPrice") && !object.isNull("discountPrice") ? object.getInt("discountPrice") / 100.0 : 0;
		_discountPercent = object.has("discount") && !object.isNull("discount") ? 100 - object.getInt("discount") : 0;
		_description = object.has("description") ? object.getString("description") : "";
		_longDescription = object.has("longDescription") && !object.isNull("longDescription") ? object.getString("longDescription") : "";
		_technicalDetails = object.has("technicalDetails") && !object.isNull("technicalDetails") ? object.getString("technicalDetails") : "";
		_urlPart = object.has("urlSlug") ? object.getString("urlSlug") : "";
		if (object.has("thumbnail") && !object.isNull("thumbnail"))
			_thumbnail = new EpicImage(object.getString("thumbnail"), 284, 284);
		if (object.has("featured") && !object.isNull("featured"))
			_featured = new EpicImage(object.getString("featured"), 476, 246);
		if (object.has("learnThumbnail") && !object.isNull("learnThumbnail"))
			_learnThumbnail = new EpicImage(object.getString("learnThumbnail"), 894, 488);
		if (object.has("headerImage") && !object.isNull("headerImage"))
			_headerImage = new EpicImage(object.getString("headerImage"), 1920, 1080);
		if (object.has("keyImages")) {
			JSONArray images = object.getJSONArray("keyImages");
			for (int i = 0; i < images.length(); i++) {
				JSONObject image = images.getJSONObject(i);
				if (!image.has("type"))
					continue;
				if (!image.has("url") || !image.has("width") || !image.has("height"))
					continue;
				String type = image.getString("type");
				if (!type.equalsIgnoreCase("Screenshot"))
					continue;
				_images.add(new EpicImage(image.getString("url"), image.getInt("width"), image.getInt("height")));
			}
		}
		if (object.has("releaseInfo")) {
			JSONArray releaseInfo = object.getJSONArray("releaseInfo");
			for (int i = 0; i < releaseInfo.length(); i++) {
				JSONObject release = releaseInfo.getJSONObject(i);
				if (!release.has("id") || !release.has("appId") || !release.has("compatibleApps") || !release.has("platform") || !release.has("versionTitle"))
					continue;
				_releases.add(new EpicItemReleaseInfo(release));
			}
		}
		for (EpicItemReleaseInfo release : _releases) {
			if (release == null)
				continue;
			String appName = release.getAppId();
			for (double compatibility : release.getCompatibility().keySet()) {
				_compatibility.put(compatibility, appName);
			}
		}
		if (object.has("seller")) {
			JSONObject seller = object.getJSONObject("seller");
			if (seller.has("name"))
				_sellerName = seller.getString("name");
		}

		if (object.has("categories")) {
			JSONArray categories = object.getJSONArray("categories");
			for (int i = 0; i < categories.length(); i++) {
				JSONObject category = categories.getJSONObject(i);
				if (!category.has("name") || !category.has("path"))
					continue;
				String name = category.getString("name");
				String path = category.getString("path");
				EpicCategory categoryObject = MarketplaceManager.getInstance().getCategory(name);
				if (categoryObject == null)
					categoryObject = MarketplaceManager.getInstance().createCategory(name, path);
				_categories.add(categoryObject);
			}
		}
		_isOwned = object.has("owned") && object.getBoolean("owned");
	}

	public EpicItem(ResultSet rset) throws SQLException {
		_id = rset.getInt("id");
		_itemId = rset.getString("item_id");
		_catalogItemId = rset.getString("catalog_item_id");
		_name = rset.getString("name");
		_description = rset.getString("description");
		_longDescription = rset.getString("long_description");
		_technicalDetails = rset.getString("technical_details");
		_urlPart = rset.getString("url_part");
		_price = rset.getDouble("price");
		_discountPrice = rset.getDouble("discounted_price");
		_discountPercent = rset.getInt("discount_percent");
		_sellerName = rset.getString("seller_name");
		_isOwned = rset.getBoolean("is_owned");
		_featured = EpicImage.loadImage(rset.getInt("featured_image"));
		_thumbnail = EpicImage.loadImage(rset.getInt("thumbnail_image"));
		_learnThumbnail = EpicImage.loadImage(rset.getInt("learn_thumbnail_image"));
		_headerImage = EpicImage.loadImage(rset.getInt("header_image"));
		_images = EpicImage.loadImagesByItem(_id);
		_categories = EpicCategory.loadCategoriesByItem(this);
		if (_isOwned)
			SessionManager.getInstance().getUser().addOwnedItem(this);
		_releases = EpicItemReleaseInfo.loadReleaseInfosByItem(_id);
		_compatibility = new HashMap<>();
		for (EpicItemReleaseInfo release : _releases) {
			if (release == null)
				continue;
			String appName = release.getAppId();
			for (double compatibility : release.getCompatibility().keySet()) {
				_compatibility.put(compatibility, appName);
			}
		}

		Connection connection = DatabaseManager.getInstance().getConnection();

		PreparedStatement statement = connection.prepareStatement("SELECT * FROM item_downloads WHERE item_id = ? AND catalog_item_id = ?");
		statement.setString(1, getItemId());
		statement.setString(2, getCatalogItemId());
		ResultSet rsetDownloads = statement.executeQuery();
		while (rsetDownloads.next()) {
			String projectName = rsetDownloads.getString("projectName");
			if(!_downloads.containsKey(projectName)) {
				_downloads.put(projectName, new ArrayList<>());
			}

			_downloads.get(projectName).add((long) rsetDownloads.getInt("download_time"));
		}
	}

	public int getId() {
		return _id;
	}

	public String getItemId() {
		return _itemId;
	}

	public String getName() {
		return _name;
	}

	public String getDescription() {
		return _description;
	}

	public String getLongDescription() {
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

	public EpicImage getThumbnail() {
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

	public List<EpicItemReleaseInfo> getReleases() {
		return _releases;
	}

	public String getAppNameByRev(double version) {
		return _compatibility.get(version);
	}

	public HashMap<String, ArrayList<Long>> getDownloads(){
		return _downloads;
	}

	public long getLastDownloadTime(String projectName){
		long lastDownload = -1;

		// return the last download time for any project when the projectName is empty
		if(projectName.length() == 0){
			for(String pName : _downloads.keySet())
			lastDownload = Math.max(lastDownload, getLastDownloadTime(pName));
		}
		// return the last download time for the requested project (if it exists)
		else if(_downloads.containsKey(projectName)) {
			for (long time : _downloads.get(projectName)) {
				lastDownload = Math.max(lastDownload, time);
			}
		}
		return lastDownload;
	}

	public boolean isCompatible(double version) {
		return _compatibility.containsKey(version);
	}

	public boolean isOwned() {
		return _isOwned;
	}

	public void removeFromDatabase() {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("DELETE FROM items WHERE catalog_item_id = ?");
			statement.setString(1, _catalogItemId);
			statement.executeUpdate();
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	public synchronized void save() {
		LOCK.lock();
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("INSERT INTO items(item_id, catalog_item_id, name, description, long_description, technical_details, url_part, price, discounted_price, discount_percent, seller_name, is_owned, featured_image, thumbnail_image, learn_thumbnail_image, header_image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, _itemId);
			statement.setString(2, _catalogItemId);
			statement.setString(3, _name);
			statement.setString(4, _description);
			statement.setString(5, _longDescription);
			statement.setString(6, _technicalDetails);
			statement.setString(7, _urlPart);
			statement.setDouble(8, _price);
			statement.setDouble(9, _discountPrice);
			statement.setInt(10, _discountPercent);
			statement.setString(11, _sellerName);
			statement.setBoolean(12, _isOwned);
			statement.setInt(13, 0);
			statement.setInt(14, 0);
			statement.setInt(15, 0);
			statement.setInt(16, 0);
			statement.executeUpdate();
			ResultSet rset = statement.getGeneratedKeys();
			if (rset.next())
				_id = rset.getInt(1);
		} catch (SQLException se) {
			se.printStackTrace();
		}
		for (EpicImage image : _images) {
			image.save(_id);
		}
		for (EpicItemReleaseInfo release : _releases) {
			release.save(_id);
		}
		int featuredId = 0;
		int thumbnailId = 0;
		int learnThumbnailId = 0;
		int headerId = 0;
		if (_featured != null)
			featuredId = _featured.save(_id);
		if (_thumbnail != null)
			thumbnailId = _thumbnail.save(_id);
		if (_learnThumbnail != null)
			learnThumbnailId = _learnThumbnail.save(_id);
		if (_headerImage != null)
			headerId = _headerImage.save(_id);

		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("UPDATE items SET featured_image = ?, thumbnail_image = ?, learn_thumbnail_image = ?, header_image = ? WHERE id = ?");
			statement.setInt(1, featuredId);
			statement.setInt(2, thumbnailId);
			statement.setInt(3, learnThumbnailId);
			statement.setInt(4, headerId);
			statement.setInt(5, _id);
			statement.executeUpdate();
		} catch (SQLException se) {
			se.printStackTrace();
		}

		for (EpicCategory category : _categories) {
			category.saveOrUpdate(this);
		}
		LOCK.unlock();
	}

	public void startDownloading() {
		try {
			DownloadForm.getInstance().setMainInfoText("Downloading [" + _name + "]");
			DownloadForm.getInstance().setMainProgressText("Downloading item info...");
			Request request = new Request("https://launcher-public-service-prod06.ol.epicgames.com/launcher/api/public/assets/Windows/" + _catalogItemId + "/" + _compatibility.get(SessionManager.getInstance().getUser().getUnrealEngineVersion()));
			request.assignParameter("label", "Live");
			request.assignCookies(SessionManager.getInstance().getSession());
			request.assignBearer();
			if (!request.execute(200)) {
				System.out.println(request);
				DownloadForm.getInstance().setMainInfoText("Failed to download item info...");
				return;
			}
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("items")) {
				System.out.println(request);
				DownloadForm.getInstance().setMainInfoText("Failed to download item info...");
				return;
			}
			JSONObject items = root.getJSONObject("items");
			if (!items.has("MANIFEST")) {
				System.out.println(request);
				DownloadForm.getInstance().setMainInfoText("Failed to download item info...");
				return;
			}
			JSONObject manifest = items.getJSONObject("MANIFEST");
			if (!manifest.has("distribution") || !manifest.has("path") || !manifest.has("signature")) {
				System.out.println(request);
				DownloadForm.getInstance().setMainInfoText("Failed to download item info...");
				return;
			}
			String distribution = manifest.getString("distribution");
			String path = manifest.getString("path");
			String signature = manifest.getString("signature");
			DownloadForm.getInstance().increase2Progress(5);
			getItemManfiest(distribution, path, signature);
			DownloadForm.getInstance().finishDownload(true, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getItemManfiest(String distribution, String path, String signature) {
		try {
			DownloadForm.getInstance().setMainProgressText("Downloading item manifest...");
			Request request = new Request(distribution + path + "?" + signature);
			if (!request.execute(200)) {
				System.out.println(request);
				DownloadForm.getInstance().setMainInfoText("Failed to download item manifest...");
				return;
			}
			JSONObject root = new JSONObject(request.getContent());
			if (!root.has("AppNameString")) {
				System.out.println(request);
				DownloadForm.getInstance().setMainInfoText("Failed to parse item manifest...");
				return;
			}
			if (!root.has("ChunkHashList") || !root.has("DataGroupList") || !root.has("ChunkFilesizeList")) {
				System.out.println(request);
				DownloadForm.getInstance().setMainInfoText("Failed to parse item manifest...");
				return;
			}
			if (!root.has("FileManifestList")) {
				System.out.println(request);
				DownloadForm.getInstance().setMainInfoText("Failed to parse item manifest...");
				return;
			}
			long totalSize = 0;
			JSONObject filesizeList = root.getJSONObject("ChunkFilesizeList");
			for (Object entry : filesizeList.toMap().values()) {
				if (!(entry instanceof String))
					continue;
				String value = (String) entry;
				totalSize += Integer.parseInt(reverseHexEncoding(value), 16);
			}
			DownloadForm.getInstance().increase2Progress(5);
			String appName = root.getString("AppNameString");
			downloadItemChunks(appName, distribution, path, root.getJSONObject("ChunkHashList"), root.getJSONObject("DataGroupList"), totalSize);
			decompressChunks(appName);
			extractChunks(appName, root.getJSONArray("FileManifestList"));
			deleteDirectory(new File("." + appName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void downloadItemChunks(String appName, String distribution, String path, JSONObject chunkHashList, JSONObject dataGroupList, long totalSize) {
		DownloadForm.getInstance().clear1Progress();
		String chunkBaseURL = distribution + path.substring(0, path.lastIndexOf("/")) + "/ChunksV3/";
		AtomicLong downloadedSize = new AtomicLong(0);
		int chunksCount = chunkHashList.length();

		DownloadForm.getInstance().setMainProgressText("Downloading item chunks...");
		DownloadForm.getInstance().setMinorProgressText(toBytes(downloadedSize.get()) + " of " + toBytes(totalSize));

		List<AbstractMap.SimpleEntry<String, String>> data = new LinkedList<>();

		JSONArray chunkHashNames = chunkHashList.names();
		for (int i = 0; i < chunkHashNames.length(); i++) {
			String name = chunkHashNames.getString(i);
			String result = chunkHashList.getString(name);

			String hash = reverseHexEncoding(result);
			String group = String.format("%02d", Integer.parseInt(dataGroupList.getString(name)));
			data.add(new AbstractMap.SimpleEntry<>(name, chunkBaseURL + group + "/" + hash + "_" + name + ".chunk"));
		}

		File directory = new File("." + appName + "/chunks/");
		if (!directory.exists())
			//noinspection ResultOfMethodCallIgnored
			directory.mkdirs();


		ExecutorService _service = Executors.newWorkStealingPool();
		DownloadForm.getInstance().setMainProgressText("Downloading chunks");
		for (AbstractMap.SimpleEntry<String, String> entry : data) {
			_service.execute(() -> {
				File file = new File("." + appName + "/chunks/" + entry.getKey() + ".chunk");
				if (file.exists())
					//noinspection ResultOfMethodCallIgnored
					file.delete();
				try (BufferedInputStream in = new BufferedInputStream(new URL(entry.getValue()).openStream());
					 FileOutputStream fout = new FileOutputStream("." + appName + "/chunks/" + entry.getKey() + ".chunk")) {
					final byte[] content = new byte[1024];
					long totalCount = 0;
					int count;
					while ((count = in.read(content, 0, 1024)) != -1) {
						fout.write(content, 0, count);
						totalCount += count;
					}
					double chunkPercent = 100.0 / chunksCount;
					double wholePercent = 30.0 / chunksCount;
					DownloadForm.getInstance().setMinorProgressText(toBytes(downloadedSize.addAndGet(totalCount)) + " of " + toBytes(totalSize));
					DownloadForm.getInstance().increase1Progress(chunkPercent);
					DownloadForm.getInstance().increase2Progress(wholePercent);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		_service.shutdown();
		while (!_service.isTerminated()) {
		}
	}

	private void decompressChunks(String appName) {
		DownloadForm.getInstance().clear1Progress();
		final AtomicInteger chunksCount = new AtomicInteger(0);
		File directory = new File("." + appName + "/chunks/");
		if (!directory.exists() || !directory.isDirectory()) {
			throw new RuntimeException("Directory [" + appName + "/chunks] does not exist!");
		}
		File[] filesList = directory.listFiles();
		if (filesList == null) {
			throw new RuntimeException("No files in directory [" + appName + "/chunks]!");
		}

		int filesCount = filesList.length;
		DownloadForm.getInstance().setMainProgressText("Decompressing chunks...");
		DownloadForm.getInstance().setMinorProgressText(chunksCount + " of " + filesCount);
		ExecutorService _service = Executors.newWorkStealingPool();
		for (File file : filesList) {
			_service.execute(() -> {
				try {
					if (file.isDirectory())
						return;
					double chunkPercent = 100.0 / filesCount;
					double wholePercent = 30.0 / filesCount;

					String fileName = file.getName();
					File newFile = new File("." + appName + "/reworked_chunks/" + fileName);
					if (newFile.exists())
						//noinspection ResultOfMethodCallIgnored
						newFile.delete();
					else
						//noinspection ResultOfMethodCallIgnored
						newFile.getParentFile().mkdirs();

					Path path = Paths.get(file.getAbsolutePath());
					byte[] data = Files.readAllBytes(path);

					byte[] headerData = Arrays.copyOfRange(data, 0, 41);
					int headerSize = headerData[8];
					boolean isCompressed = headerData[40] == 1;

					byte[] fileData = Arrays.copyOfRange(data, headerSize, data.length);

					try (FileOutputStream fos = new FileOutputStream("." + appName + "/reworked_chunks/" + fileName)) {
						byte[] resultData = isCompressed ? decompress(fileData) : fileData;
						fos.write(resultData);
					}
					DownloadForm.getInstance().setMinorProgressText(chunksCount.incrementAndGet() + " of " + filesCount);
					DownloadForm.getInstance().increase1Progress(chunkPercent);
					DownloadForm.getInstance().increase2Progress(wholePercent);
					file.deleteOnExit();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		_service.shutdown();
		while (!_service.isTerminated()) {
		}
		deleteDirectory(directory);
	}

	private void extractChunks(String appName, JSONArray fileManifestList) {
		DownloadForm.getInstance().clear1Progress();
		int filesCount = fileManifestList.length();
		double chunkPercent = 100.0 / filesCount;
		double wholePercent = 30.0 / filesCount;
		String projectPath = SessionManager.getInstance().getUser().getProjects().get(SessionManager.getInstance().getUser().getCurrentProject());
		File f = new File(projectPath);
		if (!f.exists()) {
			f.mkdirs();
		}

		DownloadForm.getInstance().setMainProgressText("Extracting files...");
		DownloadForm.getInstance().setMinorProgressText("0 of " + filesCount);

		for (int i = 0; i < filesCount; i++) {
			JSONObject manifest = fileManifestList.getJSONObject(i);
			if (!manifest.has("Filename") || !manifest.has("FileChunkParts"))
				continue;
			String fullPath = projectPath + manifest.getString("Filename");
			File file = new File(fullPath);
			if (!file.exists())
				//noinspection ResultOfMethodCallIgnored
				file.getParentFile().mkdirs();
			else
				//noinspection ResultOfMethodCallIgnored
				file.delete();

			JSONArray chunkParts = manifest.getJSONArray("FileChunkParts");
			try (FileOutputStream fos = new FileOutputStream(file.getAbsolutePath())) {
				for (int h = 0; h < chunkParts.length(); h++) {
					JSONObject chunk = chunkParts.getJSONObject(h);
					if (!chunk.has("Size"))
						continue;
					String chunkId = chunk.getString("Guid");
					int chunkOffset = Integer.decode("0x" + reverseHexEncoding(chunk.getString("Offset")));
					int chunkSize = Integer.decode("0x" + reverseHexEncoding(chunk.getString("Size")));
					try (FileInputStream fis = new FileInputStream("." + appName + "/reworked_chunks/" + chunkId + ".chunk")) {
						FileChannel channel = fis.getChannel();
						channel.position(chunkOffset);
						ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
						channel.read(buffer);
						fos.write(buffer.array());
					}
				}
				DownloadForm.getInstance().setMinorProgressText((i + 1) + " of " + filesCount);
				DownloadForm.getInstance().increase1Progress(chunkPercent);
				DownloadForm.getInstance().increase2Progress(wholePercent);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void deleteDirectory(File directory) {
		File[] files = directory.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteDirectory(f);
				} else {
					//noinspection ResultOfMethodCallIgnored
					f.delete();
				}
			}
		}
		//noinspection ResultOfMethodCallIgnored
		directory.delete();
	}

	private static byte[] decompress(byte[] rawData) {
		byte[] result = null;

		Inflater inflater = new Inflater();

		int rawDataLength = rawData.length;
		inflater.setInput(rawData, 0, rawDataLength);
		int decompressedDataLength = 0;

		try {
			while (!inflater.needsInput()) {
				byte[] decompressed = new byte[rawDataLength];
				int decompressedLength = inflater.inflate(decompressed);
				decompressedDataLength += decompressedLength;
				if (result == null) {
					result = decompressed;
				} else {
					byte[] currentData = result;
					int currentLength = decompressedDataLength - decompressedLength;
					result = new byte[decompressedDataLength];
					System.arraycopy(currentData, 0, result, 0, currentLength);
					System.arraycopy(decompressed, 0, result, currentLength, decompressedLength);
				}
			}
		} catch (DataFormatException dfe) {
			dfe.printStackTrace();
		}
		inflater.end();
		return result;
	}

	private static String reverseHexEncoding(String chunkHash) {
		String result = "";
		int count = Math.min(chunkHash.length() / 3, 8);
		for (int i = 0; i < count; ++i) {
			try {
				result = byteToHex(Integer.parseInt(chunkHash.substring(i * 3, i * 3 + 3))) + result;
			} catch (StringIndexOutOfBoundsException ss) {
				ss.printStackTrace();
			}
		}
		return result;
	}

	private static String toBytes(long size) {
		String result;
		if (size < 1024)
			result = Long.toString(size);
		else if (size < 1048576)
			result = String.format("%.2f", size / 1024.0) + "k";
		else if (size < 1073741824)
			result = String.format("%.2f", size / 1048576.0) + "M";
		else if (size < 1099511627776L)
			result = String.format("%.2f", size / 1073741824.0) + "G";
		else
			result = String.format("%.2f", size / 1099511627776.0) + "T";
		result += "B";
		return result;
	}

	static final String[] HEX_CHARS = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

	private static String byteToHex(int b) {
		return HEX_CHARS[(b >> 4) & 0x0f] + HEX_CHARS[b & 0x0f];
	}
}
