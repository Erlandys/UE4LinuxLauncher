package launcher;

import launcher.managers.*;
import launcher.objects.HtmlUtils;

public class Main {
	public static void main(String[] args) {
		//noinspection ResultOfMethodCallIgnored
		DatabaseManager.getInstance();

		HtmlUtils.initData();
		//noinspection ResultOfMethodCallIgnored
		EngineManager.getInstance();
		//noinspection ResultOfMethodCallIgnored
		AuthenticationManager.getInstance();
		//noinspection ResultOfMethodCallIgnored
		MarketplaceManager.getInstance();

		AuthenticationManager.getInstance().initialize();
	}
}