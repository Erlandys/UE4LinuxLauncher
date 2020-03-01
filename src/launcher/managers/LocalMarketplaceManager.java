package launcher.managers;

import launcher.MainForm;
import launcher.objects.EpicItem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LocalMarketplaceManager {
	private static String MARKETPLACE_FILE_LOCATION = "market";

	private int _loadedPacksCount;
	private int _packsCount;

	public void loadMarketplace() {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			Statement statement = connection.createStatement();
			ResultSet rset = statement.executeQuery("SELECT * FROM items");
			while (rset.next()) {
				MarketplaceManager.getInstance().addItem(new EpicItem(rset));
			}
		}
		catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private void increaseLoadedPacksCount() {
		_loadedPacksCount++;
		MainForm.getInstance().increaseLoadingBar(100.0 / _packsCount);
		MainForm.getInstance().setLoadingText("Loading items... [" + _loadedPacksCount + " / " + _packsCount + "]");
	}

	public static LocalMarketplaceManager getInstance() {
		return SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final LocalMarketplaceManager _instance = new LocalMarketplaceManager();
	}
}
