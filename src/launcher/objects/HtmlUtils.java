package launcher.objects;

import launcher.utils.Utils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

public class HtmlUtils {
	private static String HTML_BASE = "<html><head>%head%</head><body>%body%</body></html>";

	private static String MARKET_HEAD = Utils.getResource("launcher/html/marketHead.html");

	private static String ASSET_INFO_HEAD = Utils.getResource("launcher/html/assetInfoHead.html");

	private static String ASSET_DIV = Utils.getResource("launcher/html/assetDiv.html");

	private static String ASSET_INFO = Utils.getResource("launcher/html/assetInfo.html");

	private static String ASSET_DIV_OWNER = "<span class=\"asset-price owned\">Owned</span>";
	private static FontRenderContext _frc;
	public static Font FONT_TITLE;

	public static void initData() {
		_frc = new FontRenderContext(new AffineTransform(), true, true);
		FONT_TITLE = new Font("FreeSerif", Font.BOLD, 18);
	}

	public static String getBaseHtml() {
		return HTML_BASE;
	}

	public static String getMarketHead() {
		return MARKET_HEAD;
	}

	public static String getAssetDiv() {
		return ASSET_DIV;
	}

	public static String getAssetDivOwner() {
		return ASSET_DIV_OWNER;
	}

	public static String getAssetInfo() {
		return ASSET_INFO;
	}

	public static String findText(String text, int maxSize, Font font) {
		if ((int) (font.getStringBounds(text, _frc).getWidth()) > maxSize) {
			for (int i = text.length() - 1; i > 11; i--) {
				String shortened = text.substring(0, i) + "..";
				if ((int) (font.getStringBounds(shortened, _frc).getWidth()) <= maxSize)
					return shortened;
			}
		}
		return text;
	}

	public static String toHTML(String head, String body){
		return getBaseHtml().replace("%head%", head).replace("%body%", body);
	}

	public static String getAssetInfoHead() {
		return ASSET_INFO_HEAD;
	}
}
