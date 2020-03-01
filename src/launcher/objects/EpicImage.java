package launcher.objects;

import launcher.managers.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;

public class EpicImage {
	private int _id;
	private String _url;
	private int _width;
	private int _height;

	private EpicImage() {}

	public EpicImage(String url, int width, int height) {
		_id = 0;
		_url = url;
		_width = width;
		_height = height;
	}

	public String getUrl() {
		return _url;
	}

	public int getWidth() {
		return _width;
	}

	public int getHeight() {
		return _height;
	}

	void setId(int id) {
		_id = id;
	}

	void setUrl(String url) {
		_url = url;
	}

	void setWidth(int width) {
		_width = width;
	}

	void setHeight(int height) {
		_height = height;
	}

	public int save(int itemId) {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("INSERT INTO images(item_id, url, width, height) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, itemId);
			statement.setString(2, _url);
			statement.setInt(3, _width);
			statement.setInt(4, _height);
			statement.executeUpdate();
			ResultSet rset = statement.getGeneratedKeys();
			if (rset.next())
				_id = rset.getInt(1);
		}
		catch (SQLException se) {
			se.printStackTrace();
		}
		return _id;
	}

	public static EpicImage loadImage(int imageId) {
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM images WHERE id = ?");
			statement.setInt(1, imageId);
			ResultSet rset = statement.executeQuery();
			if (rset.next()) {
				EpicImage image = new EpicImage();
				image.setId(imageId);
				image.setUrl(rset.getString("url"));
				image.setWidth(rset.getInt("width"));
				image.setHeight(rset.getInt("height"));
				return image;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ArrayList<EpicImage> loadImagesByItem(int id) {
		ArrayList<EpicImage> result = new ArrayList<>();
		try {
			Connection connection = DatabaseManager.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM images WHERE item_id = ?");
			statement.setInt(1, id);
			ResultSet rset = statement.executeQuery();
			while (rset.next()) {
				EpicImage image = new EpicImage();
				image.setId(rset.getInt("id"));
				image.setUrl(rset.getString("url"));
				image.setWidth(rset.getInt("width"));
				image.setHeight(rset.getInt("height"));
				result.add(image);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
}
