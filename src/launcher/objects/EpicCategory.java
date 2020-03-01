package launcher.objects;

import launcher.managers.DatabaseManager;
import launcher.managers.MarketplaceManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class EpicCategory {
	private int _id;
	private String _path;
	private String _name;

	private HashMap<String, EpicItem> _items;

	public EpicCategory(int id, String path, String name) {
		_id = id;
		_path = path;
		_name = name;
		_items = new HashMap<>();
	}

	public void addItem(EpicItem item) {
		_items.put(item.getItemId(), item);
	}

	public String getPath() {
		return _path;
	}

	public String getName() {
		return _name;
	}

	public HashMap<String, EpicItem> getItems() {
		return _items;
	}

	public void saveOrUpdate(EpicItem epicItem) {
		_items.put(epicItem.getItemId(), epicItem);
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("REPLACE INTO categories(path, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, _path);
			statement.setString(2, _name);
			statement.executeUpdate();
			ResultSet rset = statement.getGeneratedKeys();
			if (rset != null && rset.next())
				_id = rset.getInt(1);

			statement = connection.prepareStatement("INSERT INTO categories_items(item_id, category_id) VALUES(?, ?)");
			statement.setInt(1, epicItem.getId());
			statement.setInt(2, _id);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<EpicCategory> loadCategoriesByItem(EpicItem item) {
		ArrayList<EpicCategory> result = new ArrayList<>();
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT categories.id, categories.name, categories.path FROM categories_items INNER JOIN categories ON categories_items.category_id = categories.id WHERE item_id = ?");
			statement.setInt(1, item.getId());
			ResultSet rset = statement.executeQuery();
			while (rset.next()) {
				EpicCategory category = MarketplaceManager.getInstance().getCategory(rset.getString("name"));
				if (category == null) {
					category = MarketplaceManager.getInstance().createCategory(rset.getInt("id"), rset.getString("name"), rset.getString("path"));
				}
				category.addItem(item);
				result.add(category);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
}
