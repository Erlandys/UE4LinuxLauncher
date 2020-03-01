package launcher.objects;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

public class HtmlUtils {
	private static String HTML_BASE = "<html><head>%head%</head><body>%body%</body></html>";
	private static String MARKET_HEAD = "    <style type=\"text/css\">\n" +
			"    \t.asset {\n" +
			"        \tborder: 1px solid #cccccc;\n" +
			"            width: 187px;\n" +
			"        }\n" +
			"        .info .title {\n" +
			"        \tpadding-top: 0px;\n" +
			"        \tmargin-top: 0px;\n" +
			"        \tfont-size: 18px;\n" +
			"        \tpadding-left: 3px;\n" +
			"        \tfont-family: FreeSerif;\n" +
			"        }" +
			"        a {\n" +
			"        \ttext-decoration:none;\n" +
			"        \tcolor: #0aaff1;\n" +
			"        }\n" +
			"        .info .title a {\n" +
			"        \ttext-decoration:none;\n" +
			"        \tcolor: #000000;\n" +
			"        \tfont-family: FreeSerif;\n" +
			"        }" +
			"        .asset .creator {\n" +
			"        \tfont-size: 12px;\n" +
			"        \tfont-weight: bold;\n" +
			"        \tpadding-left: 3px;\n" +
			"        \tpadding-bottom: 4px;\n" +
			"        \tfont-family: FreeSerif;\n" +
			"        }\n" +
			"        .asset .price-container {\n" +
			"        \tfont-size: 12px;\n" +
			"        \ttext-align: right;\n" +
			"        \tfont-family: FreeSerif;\n" +
			"        \tpadding-bottom: 5px;\n" +
			"        }\n" +
			"        .asset .asset-category {\n" +
			"        \tborder-top: 1px solid #cccccc;\n" +
			"        \tpadding-top: 5px;\n" +
			"        \tpadding-bottom: 5px;\n" +
			"        \tfont-size: 12px;\n" +
			"        \tpadding-left: 3px;\n" +
			"        \tfont-weight: bold;\n" +
			"        \ttext-align: center;\n" +
			"        \tfont-family: FreeSerif;\n" +
			"        }\n" +
			"        .asset-container {\n" +
			"   \t\t\ttext-align:center; \n" +
			"        }\n" +
			"        .asset-container td {\n" +
			"        \tpadding-left: 15px;\n" +
			"        }\n\n" +
			"        .more-button {\n" +
			"        \ttext-align: center;\n" +
			"        \tfont-size: 20px;\n" +
			"        \tfont-weight: bold;\n" +
			"        \tborder-top: 2px solid #cccccc;\n" +
			"        \tborder-bottom: 2px solid #cccccc;\n" +
			"        }\n" +
			"        .more-button a {\n" +
			"        \tcolor: #999999;\n" +
			"        }" +
			".selected {\n" +
			"        \tcolor: #7a7a7a;\n" +
			"        }\n" +
			"        .filter {\n" +
			"        \ttext-align: right;\n" +
			"        \tmargin-right: 10px;\n" +
			"        \tmargin-top: 10px;\n" +
			"        \tfont-weight: 700;\n" +
			"    \t}" +
			"\t</style>";
	private static String ASSET_INFO_HEAD = "<style type=\"text/css\">body {\n" +
			"            font-family: FreeSerif;\n" +
			"        }\n" +
			"        a {\n" +
			"            text-decoration:none;\n" +
			"            color: #0aaff1;\n" +
			"        }\n" +
			"        .top {\n" +
			"            border-bottom: 1px solid #cccccc;\n" +
			"        }\n" +
			"    \t.top .images {\n" +
			"            text-align:center;\n" +
			"        }\n" +
			"        .top .data td {\n" +
			"            height: 100%;\n" +
			"            vertical-align: bottom;\n" +
			"        }\n" +
			"        .top .data .title {\n" +
			"            padding-top: 0px;\n" +
			"            margin-top: 5px;\n" +
			"            padding-bottom: 15px;\n" +
			"            margin-bottom: 15px;\n" +
			"            font-size: 20px;\n" +
			"            padding-left: 3px;\n" +
			"            font-family: FreeSerif;\n" +
			"            color: #000000;\n" +
			"            font-weight: bold;\n" +
			"        }\n" +
			"        .top .data .detail-text {\n" +
			"            font-size: 13px;\n" +
			"            padding-left: 3px;\n" +
			"            padding-bottom: 5px;\n" +
			"            margin-bottom: 5px;\n" +
			"            font-family: FreeSerif;\n" +
			"            color: #000000;\n" +
			"        }\n" +
			"        .top .data .data-table {\n" +
			"            background-color: #EEEEEE;\n" +
			"            border: 1px solid #cccccc;\n" +
			"            width: 100%;\n" +
			"        }\n" +
			"        .top .data .data-table .first-column {\n" +
			"            border-right: 1px solid #cccccc;\n" +
			"            width: 50%;\n" +
			"            text-align: center;\n" +
			"            height: 100%;\n" +
			"        }\n" +
			"        .top .data .data-table .label {\n" +
			"            font-size: 9px;\n" +
			"            font-family: FreeSerif;\n" +
			"            font-weight: bold;\n" +
			"        }\n" +
			"        .top .data .data-table .text {\n" +
			"            font-size: 9px;\n" +
			"            font-family: FreeSerif;\n" +
			"        }\n" +
			"        .top .data .data-table .supported-platforms {\n" +
			"            padding-bottom: 5px;\n" +
			"        }\n" +
			"        .download-button {\n" +
			"            background-color: #0aaff1;\n" +
			"            cursor: pointer;\n" +
			"            display: block;\n" +
			"            height: 35px;\n" +
			"            border: none;\n" +
			"            outline: none;\n" +
			"            border-radius: 2px;\n" +
			"            white-space: nowrap;\n" +
			"            vertical-align: middle;\n" +
			"            box-sizing: border-box;\n" +
			"            margin: 0 auto;\n" +
			"            margin-top: 1em;\n" +
			"            max-width: 6em;\n" +
			"            min-width: 4em;\n" +
			"            text-align: center;\n" +
			"            padding-top: 8px;\n" +
			"        }\n" +
			"        .download-button .btn {\n" +
			"            color: #ffffff;\n" +
			"            font-weight: 700;\n" +
			"            font-family: FreeSerif;\n" +
			"            text-decoration: none;\n" +
			"            font-size: 13px;\n" +
			"        }\n" +
			"        .top-line {\n" +
			"            border-bottom: 1px solid #cccccc;\n" +
			"            text-align: right;\n" +
			"            font-size: 16px;\n" +
			"            font-weight: 700;\n" +
			"            padding-right: 10px;\n" +
			"            padding-bottom: 5px;\n" +
			"            padding-top: 5px;\n" +
			"        }</style>";

	private static String ASSET_DIV = "    \t<div class=\"asset\">\n" +
			"        \t<div class=\"image-box\">\n" +
			"                <a href=\"item %id%\">\n" +
			"                    <img src=\"%image%\" height=\"240\" width=\"240\" border=\"0\">\n" +
			"                </a>\n" +
			"        \t</div>\n" +
			"\t        <div class=\"info\">\n" +
			"\t            <div class=\"title\">\n" +
			"\t                <a class=\"mock-ellipsis-item mock-ellipsis-item-helper ellipsis-text\" href=\"item %id%\">%title%</a>\n" +
			"\t            </div>\n" +
			"\t            <div class=\"creator\">\n" +
			"\t                <a class=\"\" href=\"\">%creator%</a>&nbsp;\n" +
			"\t            </div>\n" +
			"\t            <div class=\"price-container\">\n" +
			"\t                %owned%" +
			"\t                <span class=\"asset-price\">\n" +
			"\t                    <span class=\"\">\n" +
			"\t                        %price%\n" +
			"\t                    </span> \n" +
			"\t                </span>\n" +
			"\t            </div>\n" +
			"\t            <div class=\"asset-category\">\n" +
			"                \t<a class=\"mock-ellipsis-item-cat\" href=\"category %category%\">%category%</a>\n" +
			"                </div>\n" +
			"\t        </div>\n" +
			"    \t</div>";

	private static String ASSET_INFO = "    <div class=\"top-line\"><a href=\"%backBypass%\">Back</a></div>\n" +
			"    <table class=\"top\">\n" +
			"        <tr>\n" +
			"            <td class=\"images\">\n" +
			"                %firstImage%<br>\n" +
			"                %navigation%<br>\n" +
			"            </td>\n" +
			"            <td class=\"data\" valign=\"top\">\n" +
			"                <div class=\"title\">%title%</div>\n" +
			"                <div class=\"detail-text\">%description%</div>\n" +
			"                <table class=\"data-table\">\n" +
			"                    <tr>\n" +
			"                        <td valign=\"middle\" class=\"first-column\">\n" +
			"                            %download%\n" +
			"                        </td>\n" +
			"                        <td>\n" +
			"                            <div class=\"supported-platforms\">\n" +
			"                                <div class=\"label\">Supported Platforms</div>\n" +
			"                                %platforms%\n" +
			"                            </div>\n" +
			"                            <div class=\"supported-versions\">\n" +
			"                                <div class=\"label\">Supported Engine Versions</div>\n" +
			"                                <div class=\"text\">%versions%</div>\n" +
			"                            </div>\n" +
			"                        </td>\n" +
			"                    </tr>\n" +
			"                </table>\n" +
			"            </td>\n" +
			"        </tr>\n" +
			"    </table>\n" +
			"    <div id=\"descPanel\" class=\"col-xs-12\">\n" +
			"        <h1>\n" +
			"            Description\n" +
			"        </h1>\n" +
			"        %longDescription%" +
			"    </div>\n" +
			"\n" +
			"    <div id=\"technicalDetailsPanel\" class=\"col-xs-12 technical-details\">\n" +
			"        <h1>\n" +
			"            Technical Details\n" +
			"        </h1>\n" +
			"        %techDescription%\n" +
			"    </div>";

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

	public static String getAssetInfoHead() {
		return ASSET_INFO_HEAD;
	}
}
