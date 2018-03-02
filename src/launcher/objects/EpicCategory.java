package launcher.objects;

import java.util.HashMap;

public class EpicCategory {
    private String _path;
    private String _name;

    private HashMap<String, EpicItem> _items;

    public EpicCategory(String path, String name)
    {
        _path = path;
        _name = name;
        _items = new HashMap<>();
    }

    public void addItem(EpicItem item)
    {
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

}
