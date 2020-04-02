package launcher.managers;

import java.sql.*;

public class DatabaseManager {

	private static String URL = "jdbc:sqlite:data.db";
	private Connection _connection;

	DatabaseManager() {
		prepareDatabase();
	}

	public Connection getConnection() throws SQLException {
		return _connection;
	}

	private void prepareDatabase() {
		try {
			_connection = DriverManager.getConnection(URL);
			prepareCookiesTable();
			prepareUserDataTable();
			prepareItemsTable();
			prepateItemReleasesTable();
			prepareCategoriesTable();
			prepareCategoriesItemsTable();
			prepareImagesTable();
			prepareGlobalVariablesTable();
			prepateItemDownloadsTable();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void prepareCookiesTable() throws SQLException {
		if (tableExists("cookies"))
			return;
		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE \"cookies\" (\n" +
				"  \"name\" TEXT NOT NULL,\n" +
				"  \"value\" TEXT,\n" +
				"  \"expire_at\" integer NOT NULL,\n" +
				"  PRIMARY KEY (\"name\")\n" +
				");");
	}

	private void prepareUserDataTable() throws SQLException {
		if (tableExists("user_data"))
			return;
		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE \"user_data\" (\n" +
				"  \"email\" TEXT NOT NULL,\n" +
				"  \"password\" TEXT NOT NULL,\n" +
				"  \"engine_install_dir\" TEXT,\n" +
				"  \"exchange_code\" TEXT,\n" +
				"  \"oauth_access_key\" TEXT,\n" +
				"  \"oauth_access_key_expiration\" integer(10),\n" +
				"  \"oauth_refresh_token\" TEXT,\n" +
				"  \"oauth_refresh_token_expiration\" integer(10),\n" +
				"  \"account_id\" TEXT,\n" +
				"  \"client_id\" TEXT,\n" +
				"  \"app_id\" TEXT,\n" +
				"  \"device_id\" TEXT,\n" +
				"  \"name\" TEXT,\n" +
				"  \"oauth_exchange_code\" TEXT,\n" +
				"  PRIMARY KEY (\"email\")\n" +
				");");
	}




	private void prepareItemsTable() throws SQLException {
		if (tableExists("items")) {
			// check if field exists,
			if(!fieldExists("items", "totalSize")){
				Statement statement = _connection.createStatement();
				statement.executeUpdate("ALTER TABLE items ADD COLUMN totalSize INTEGER");
			}
			return;
		}

		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE \"items\" (\n" +
				"  \"id\" integer NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
				"  \"item_id\" TEXT,\n" +
				"  \"catalog_item_id\" TEXT NOT NULL,\n" +
				"  \"name\" TEXT,\n" +
				"  \"description\" TEXT,\n" +
				"  \"long_description\" TEXT,\n" +
				"  \"technical_details\" TEXT,\n" +
				"  \"url_part\" TEXT,\n" +
				"  \"price\" real(10,2),\n" +
				"  \"discounted_price\" real(10,2),\n" +
				"  \"discount_percent\" integer(10),\n" +
				"  \"seller_name\" TEXT,\n" +
				"  \"is_owned\" integer(1),\n" +
				"  \"effective_date\" integer(10),\n" +
				"  \"featured_image\" integer(10),\n" +
				"  \"thumbnail_image\" integer(10),\n" +
				"  \"learn_thumbnail_image\" integer(10),\n" +
				"  \"header_image\" integer(10),\n" +
				"  \"asset_id\" TEXT\n" +
				"  \"totalSize\" integer\n" +
				");");
	}

	private void prepateItemReleasesTable() throws SQLException {
		if (tableExists("item_releases"))
			return;
		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE \"item_releases\" (\n" +
				"  \"id\" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
				"  \"release_id\" TEXT,\n" +
				"  \"app_id\" TEXT,\n" +
				"  \"lowest_version\" real(10,2),\n" +
				"  \"highest_version\" real(10,2),\n" +
				"  \"version_title\" TEXT,\n" +
				"  \"compatibility\" TEXT,\n" +
				"  \"platforms\" TEXT,\n" +
				"  \"item_id\" integer(10) NOT NULL,\n" +
				"  CONSTRAINT \"item_releases_item_id_foreign_key\" FOREIGN KEY (\"item_id\") REFERENCES \"items\" (\"id\") ON DELETE CASCADE ON UPDATE NO ACTION\n" +
				");");
	}

	private void prepareCategoriesTable() throws SQLException {
		if (tableExists("categories"))
			return;
		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE \"categories\" (\n" +
				"  \"id\" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
				"  \"path\" TEXT NOT NULL,\n" +
				"  \"name\" TEXT\n" +
				");");
	}

	private void prepareCategoriesItemsTable() throws SQLException {
		if (tableExists("categories_items"))
			return;
		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE \"categories_items\" (\n" +
				"  \"id\" integer NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
				"  \"item_id\" integer(10) NOT NULL,\n" +
				"  \"category_id\" integer(10) NOT NULL,\n" +
				"  CONSTRAINT \"categories_items_item_id_foreign_key\" FOREIGN KEY (\"item_id\") REFERENCES \"items\" (\"id\") ON DELETE CASCADE ON UPDATE NO ACTION,\n" +
				"  CONSTRAINT \"categories_items_category_id_foreign_key\" FOREIGN KEY (\"category_id\") REFERENCES \"categories\" (\"id\") ON DELETE CASCADE ON UPDATE NO ACTION\n" +
				");");
	}

	private void prepareImagesTable() throws SQLException {
		if (tableExists("images"))
			return;
		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE \"images\" (\n" +
				"  \"id\" integer NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
				"  \"item_id\" integer(10) NOT NULL,\n" +
				"  \"url\" TEXT NOT NULL,\n" +
				"  \"width\" integer(10) NOT NULL,\n" +
				"  \"height\" integer(10) NOT NULL,\n" +
				"  CONSTRAINT \"images_item_id_foreign_key\" FOREIGN KEY (\"item_id\") REFERENCES \"items\" (\"id\") ON DELETE CASCADE ON UPDATE NO ACTION\n" +
				");");
	}

	private void prepareGlobalVariablesTable() throws SQLException {
		if (tableExists("global_variables"))
			return;
		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE \"global_variables\" (\n" +
				"  \"name\" TEXT NOT NULL,\n" +
				"  \"value\" TEXT,\n" +
				"  PRIMARY KEY (\"name\")\n" +
				");");
	}

	private boolean tableExists(String tableName) throws SQLException {
		PreparedStatement statement = _connection.prepareStatement("SELECT `name` FROM `sqlite_master` WHERE `type` = 'table' AND `name` = ?");
		statement.setString(1, tableName);
		ResultSet rset = statement.executeQuery();
		return rset.next();
	}

	private boolean fieldExists(String tableName, String fieldName) throws SQLException {
		PreparedStatement statement = _connection.prepareStatement("SELECT * FROM pragma_table_info(?) WHERE name = ?");
		statement.setString(1, tableName);
		statement.setString(2, fieldName);
		ResultSet rset = statement.executeQuery();
		return rset.next();
	}


	private void prepateItemDownloadsTable() throws SQLException {
		if (tableExists("item_downloads"))
			return;
		Statement statement = _connection.createStatement();
		statement.executeUpdate("CREATE TABLE 'item_downloads' ('id' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, 'item_id' TEXT, 'catalog_item_id' TEXT, 'projectName' TEXT, 'download_time' INTEGER);");
	}

	public static DatabaseManager getInstance() {
		return DatabaseManager.SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final DatabaseManager _instance = new DatabaseManager();
	}
}
