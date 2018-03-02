package launcher.objects;

public class EpicImage {
    private String _url;
    private int _width;
    private int _height;

    public EpicImage(String url, int width, int height)
    {
        _url = url;
        _width = width;
        _height = height;
    }

    public String getUrl()
    {
        return _url;
    }

    public int getWidth()
    {
        return _width;
    }

    public int getHeight()
    {
        return _height;
    }
}
