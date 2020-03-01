package launcher;

import launcher.managers.EngineManager;
import launcher.managers.MarketplaceManager;
import launcher.managers.SessionManager;
import launcher.objects.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

public class MainForm extends JFrame {

	private enum ViewType {
		Library,
		Marketplace,
		OwnedAssets
	}

	private enum Filter {
		All(Double.MAX_VALUE),
		Below10(10),
		Below25(25),
		Below50(50),
		Free(0);

		double _price;

		Filter(double price) {
			_price = price;
		}

		public double getPrice() {
			return _price;
		}
	}

	private JPanel mainPanel;
	private JTextPane usernamePane;
	private JList list1;
	private JButton marketplaceButton;
	private JButton libraryButton;
	private JButton ownedAssetsButton;
	private JProgressBar _loadingProgressBar;
	private double _loadingPercent;
	private JLabel _loadingLabel;
	private JPanel _mainPanel;
	private JButton _logoutButton;
	private JProgressBar _progressBar1;
	private JPanel _libraryPanel;
	private JTextField _engineInstallDir;
	private JButton _selectButton;
	private JButton _updateButton;
	private JLabel _engineVersion;
	private JTextPane _textPane1;
	private EpicCategory _currentCategory;
	private JScrollPane _scrollPane;
	private ViewType _viewType;
	private JTextPane _projectsList;
	private JLabel _selectedProject;
	private JButton _launchUE4Button;
	private JButton _reloadOwnedAssetsButton;

	private int _itemsPerPage;
	private boolean _viewingItem;
	private Thread _downloadThread;
	private int _caretPosition;

	private boolean _reloadingOwnedAssets = false;

	private Filter _filter;

	private String _mainLoadingBarText;
	private String _additionalLoadingBarText;

	MainForm() {
		super("Main");
		_caretPosition = 0;
		_viewingItem = false;
		_loadingPercent = 0;
		_currentCategory = null;
		_itemsPerPage = 8;
		_viewType = ViewType.Library;
		_downloadThread = null;
		_filter = Filter.All;
		setContentPane(mainPanel);
		setVisible(true);
		setResizable(false);
		pack();
		_textPane1 = new JTextPane();
		_textPane1.setEditable(false);
		_textPane1.setContentType("text/html");
		DefaultCaret caret = (DefaultCaret) _textPane1.getCaret();
		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		_scrollPane = new JScrollPane();
		_scrollPane.setEnabled(true);
		_scrollPane.setVerticalScrollBarPolicy(20);
		_scrollPane.setViewportView(_textPane1);
		_mainPanel.add(_scrollPane);
		((HTMLEditorKit) _textPane1.getEditorKitForContentType("text/html")).setAutoFormSubmission(false);
		list1.addListSelectionListener(listSelectionEvent -> {
			if (_currentCategory == MarketplaceManager.getInstance().getCategory(list1.getSelectedValue().toString()) && !_viewingItem)
				return;
			_currentCategory = MarketplaceManager.getInstance().getCategory(list1.getSelectedValue().toString());
			_itemsPerPage = 8;
			_viewingItem = false;
			updateMarketplaceList();
		});
		marketplaceButton.addActionListener(actionEvent -> {
			if (_viewType.equals(ViewType.Marketplace) && !_viewingItem)
				return;
			marketplaceButton.setEnabled(false);
			switch (_viewType) {
				case OwnedAssets:
					ownedAssetsButton.setEnabled(true);
					break;
				case Library:
					libraryButton.setEnabled(true);
					break;
			}
			_libraryPanel.setVisible(false);
			_mainPanel.setVisible(true);
			_viewType = ViewType.Marketplace;
			list1.setVisible(true);
			_viewingItem = false;
			if (_currentCategory == null) {
				list1.setSelectedIndex(0);
				return;
			}
			updateMarketplaceList();

		});
		libraryButton.addActionListener(actionEvent -> {
			if (_viewType.equals(ViewType.Library) && !_viewingItem)
				return;
			libraryButton.setEnabled(false);
			switch (_viewType) {
				case OwnedAssets:
					ownedAssetsButton.setEnabled(true);
					break;
				case Marketplace:
					marketplaceButton.setEnabled(false); // TODO:
					break;
			}
			_mainPanel.setVisible(false);
			_libraryPanel.setVisible(true);
			_viewType = ViewType.Library;
			list1.setVisible(false);
			_currentCategory = null;
			_viewingItem = false;
		});
		ownedAssetsButton.addActionListener(actionEvent -> {
			if (_viewType.equals(ViewType.OwnedAssets) && !_viewingItem)
				return;
			ownedAssetsButton.setEnabled(false);
			switch (_viewType) {
				case Library:
					libraryButton.setEnabled(true);
					break;
				case Marketplace:
					marketplaceButton.setEnabled(false); // TODO:
					break;
			}
			_libraryPanel.setVisible(false);
			_mainPanel.setVisible(true);
			_viewingItem = false;
			list1.setVisible(false);
			_viewType = ViewType.OwnedAssets;
			_itemsPerPage = 8;
			updateOwnedAssetsList();
		});
		_textPane1.addHyperlinkListener(e -> {
			if (!e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
				return;
			if (e.getDescription().startsWith("item")) {
				_caretPosition = _textPane1.getCaretPosition();
				String data[] = e.getDescription().split((" "));
				if (data.length == 3)
					showItemInfo(data[1], Integer.parseInt(data[2]));
				else
					showItemInfo(data[1], 0);
				_viewingItem = true;
			} else if (e.getDescription().startsWith("download_item")) {
				startDownload(e.getDescription().split(" ")[1]);
			} else if (e.getDescription().equalsIgnoreCase("back")) {
				switch (_viewType) {
					case OwnedAssets:
						_viewingItem = false;
						updateOwnedAssetsList();
						_textPane1.setCaretPosition(_caretPosition);
						break;
					case Marketplace:
						_viewingItem = false;
						updateMarketplaceList();
						_textPane1.setCaretPosition(_caretPosition);
						break;
				}
			} else if (e.getDescription().startsWith("filter")) {
				_filter = Filter.valueOf(e.getDescription().split(" ")[1]);
				_itemsPerPage = 8;
				updateMarketplaceList();
			} else if (e.getDescription() != null && e.getDescription().length() > 0) {
				if (Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (URISyntaxException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		_projectsList.addHyperlinkListener(e -> {
            if (!e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                return;
            if (e.getDescription().startsWith("project")) {
                String[] data = e.getDescription().split((" "));
                _selectedProject.setText(data[1]);
                SessionManager.getInstance().getUser().setCurrentProject(data[1]);
                updateProjectsList();
            }
		});
		_scrollPane.getVerticalScrollBar().addAdjustmentListener(event -> {
			if (_viewingItem)
				return;
			if (_viewType.equals(ViewType.Marketplace)) {
				if (_currentCategory == null)
					return;
				JScrollBar scrollBar = (JScrollBar) event.getAdjustable();
				int extent = scrollBar.getModel().getExtent();
				if (scrollBar.getMaximum() - (scrollBar.getValue() + extent) > 6)
					return;
				long size = _currentCategory.getItems().values().stream().filter(item -> item.getPrice() <= _filter.getPrice()).count();
				if (_itemsPerPage >= size)
					return;
				_itemsPerPage += 8;
				updateMarketplaceList();
			} else if (_viewType.equals(ViewType.OwnedAssets)) {
                if (_reloadingOwnedAssets)
                    return;
                JScrollBar scrollBar = (JScrollBar) event.getAdjustable();
                int extent = scrollBar.getModel().getExtent();
                if (scrollBar.getMaximum() - (scrollBar.getValue() + extent) > 6)
                    return;
                if (_itemsPerPage >= SessionManager.getInstance().getUser().getOwnedItems().size())
                    return;
                _itemsPerPage += 8;
                updateOwnedAssetsList();
            }
		});
		_selectButton.addActionListener(actionEvent -> {
			JFileChooser chooser;
			chooser = new JFileChooser();
			String location = SessionManager.getInstance().getUser().getUe4InstallLocation() == null || SessionManager.getInstance().getUser().getUe4InstallLocation().length() < 1 ? "." : SessionManager.getInstance().getUser().getUe4InstallLocation();
			chooser.setCurrentDirectory(new File(location));
			chooser.setDialogTitle("Select UE4 install directory");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				SessionManager.getInstance().getUser().setUe4InstallLocation(chooser.getSelectedFile().toString());
				SessionManager.getInstance().getUser().save();
				setEngineInstallDir(chooser.getSelectedFile().toString());
				EngineManager.getInstance().readEngineData();
			}
		});
		_updateButton.addActionListener(actionEvent -> EngineManager.getInstance().readEngineData());
		_launchUE4Button.addActionListener(actionEvent -> {
			new Thread(() -> {
				String launch = SessionManager.getInstance().getUser().getUe4InstallLocation() + "/Engine/Binaries/Linux/UE4Editor";
				String project = "";
				if (SessionManager.getInstance().getUser().getCurrentProject() != null && SessionManager.getInstance().getUser().getCurrentProject().length() > 0)
					project = SessionManager.getInstance().getUser().getProjects().get(SessionManager.getInstance().getUser().getCurrentProject()) + SessionManager.getInstance().getUser().getCurrentProject() + ".uproject";
				File file = new File(launch);
				file.setExecutable(true);
				try {
					Process p;
					if (project.isEmpty())
						p = new ProcessBuilder(launch, "&").start();
					else
						p = new ProcessBuilder(launch, project, "&").start();
					p.waitFor();
				} catch (Exception ignored) {
					ignored.printStackTrace();
				}
			}).start();
		});
		_reloadOwnedAssetsButton.addActionListener(actionEvent -> {
			_reloadingOwnedAssets = true;
			_loadingProgressBar.setVisible(true);
			new Thread(() ->
			{
				MarketplaceManager.getInstance().loadOwnedAssets();
				if (_viewType.equals(ViewType.OwnedAssets)) {
					_itemsPerPage = 8;
					updateOwnedAssetsList();
				}
				_reloadingOwnedAssets = false;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				_loadingProgressBar.setVisible(false);
			}).start();
		});
		_logoutButton.addActionListener(actionEvent -> doLogout());
	}

	private void doLogout() {
		setVisible(false);
		LoginForm.getInstance().setLoginData(SessionManager.getInstance().getUser().getEmail(), "Password:");
		LoginForm.getInstance().clearError();
		LoginForm.getInstance().clearProgress();
		LoginForm.getInstance().allowActions();
		LoginForm.getInstance().setVisible(true);
	}

	public void updateCategoriesList(Collection<String> categories) {
		String[] data = new String[categories.size()];
		categories.toArray(data);
		for (int i = 0; i < data.length - 1; i++) {
			for (int j = i + 1; j < data.length; j++) {
				if (data[i].compareTo(data[j]) > 0) {
					String dd = data[i];
					data[i] = data[j];
					data[j] = dd;
				}
			}
		}
		list1.setListData(data);
	}

	public void setMainLoadingText(String text) {
	}

	public void setLoadingText(String text) {
		setLoadingText(text, false);
	}

	public void setLoadingText(String text, boolean onlyText) {
		_additionalLoadingBarText = text;
		_loadingProgressBar.setString(_additionalLoadingBarText + (onlyText ? "" : " [" + String.format("%.2f", _loadingPercent) + "%]"));
	}

	public void increaseLoadingBar(int percent) {
		_loadingPercent = percent;
		_loadingProgressBar.setValue((int) _loadingPercent);
		_loadingProgressBar.setString(_additionalLoadingBarText + " [" + String.format("%.2f", _loadingPercent) + "%]");
	}

	public void increaseLoadingBar(double percent) {
		_loadingPercent = percent;
		_loadingProgressBar.setValue((int) _loadingPercent);
		_loadingProgressBar.setString(_additionalLoadingBarText + " [" + String.format("%.2f", _loadingPercent) + "%]");
	}

	public void hideLoading() {
		Thread t1 = new Thread(() -> {
			try {
				Thread.sleep(2000);
				_loadingLabel.setVisible(false);
				_loadingProgressBar.setVisible(false);
			} catch (InterruptedException e) {
				// handle: log or throw in a wrapped RuntimeException
				throw new RuntimeException("InterruptedException caught in lambda", e);
			}
		});
		t1.start();
	}

	public void disableActions() {
		marketplaceButton.setEnabled(false);
		libraryButton.setEnabled(false);
		ownedAssetsButton.setEnabled(false);
		_launchUE4Button.setEnabled(false);
		_selectButton.setEnabled(false);
		_updateButton.setEnabled(false);
		_progressBar1.setVisible(false);
		_reloadOwnedAssetsButton.setEnabled(false);
	}

	public void enableActions() {
		if (!_viewType.equals(ViewType.Marketplace))
			marketplaceButton.setEnabled(false); // TODO:
		if (!_viewType.equals(ViewType.Library))
			libraryButton.setEnabled(true);
		if (!_viewType.equals(ViewType.OwnedAssets))
			ownedAssetsButton.setEnabled(true);
		_selectButton.setEnabled(true);
		_updateButton.setEnabled(true);
		_reloadOwnedAssetsButton.setEnabled(true);
	}

	private void updateMarketplaceList() {
		String html = HtmlUtils.getBaseHtml();
		html = html.replace("%head%", HtmlUtils.getMarketHead());
		StringBuilder data = new StringBuilder();

		data.append("<div class=\"filter\">");
		data.append("<a class=\"mock-ellipsis-item-cat\"").append(_filter.equals(Filter.All) ? " class=\"selected\" href=\"\"" : " href=\"filter All\"").append(">All</a>");
		data.append(" | ");
		data.append("<a class=\"mock-ellipsis-item-cat\"").append(_filter.equals(Filter.Below50) ? " class=\"selected\" href=\"\"" : " href=\"filter Below50\"").append(">Below 50$</a>");
		data.append(" | ");
		data.append("<a class=\"mock-ellipsis-item-cat\"").append(_filter.equals(Filter.Below25) ? " class=\"selected\" href=\"\"" : " href=\"filter Below25\"").append(">Below 25$</a>");
		data.append(" | ");
		data.append("<a class=\"mock-ellipsis-item-cat\"").append(_filter.equals(Filter.Below10) ? " class=\"selected\" href=\"\"" : " href=\"filter Below10\"").append(">Below 10$</a>");
		data.append(" | ");
		data.append("<a class=\"mock-ellipsis-item-cat\"").append(_filter.equals(Filter.Free) ? " class=\"selected\" href=\"\"" : " href=\"filter Free\"").append(">Only free</a>");
		data.append("</div>");
		data.append("<p align=\"center\" style=\"margin-top: 10\" style=\"font-family: Lato, Helvetica, Arial, sans-serif\">\n");
		data.append("<span style=\"font-size: 16px; color: #808080; text-shadow: 2px 2px #ff0000;\">");
		data.append(_currentCategory.getName());
		data.append("</span><br></p><br>");
		if (_currentCategory != null) {
			int itemsInLine = 4;
			final int i[] = {0};

			data.append("<table class=\"asset-container\">");
			_currentCategory.getItems().values().stream().filter(item -> item.getPrice() <= _filter.getPrice()).forEach(item -> {
				if (i[0] >= _itemsPerPage)
					return;
				if (i[0] % itemsInLine == 0 && i[0] > 0)
					data.append("</tr>");
				if (i[0] % itemsInLine == 0) {
					data.append("<tr>");
				}
				data.append("<td>");
				String asset = HtmlUtils.getAssetDiv();
				asset = asset.replaceAll("%category%", _currentCategory.getName());
				asset = asset.replaceAll("%id%", item.getCatalogItemId());
				String name = HtmlUtils.findText(item.getName(), 195, HtmlUtils.FONT_TITLE);
				asset = asset.replaceAll("%title%", name);
				asset = asset.replaceAll("%creator%", item.getSellerName());
				asset = asset.replaceAll("%image%", item.getThumbnail().getUrl());
				/*if (SessionManager.getInstance().getUser().getOwnedAsset(item.getCatalogItemId()) != null) {
					asset = asset.replaceAll("%owned%", HtmlUtils.getAssetDivOwner());
					asset = asset.replaceAll("%price%", item.isCompatible(SessionManager.getInstance().getUser().getEngineVersion()) ? "" : "Not compatible");
				} else {
					asset = asset.replaceAll("%owned%", item.isCompatible(SessionManager.getInstance().getUser().getEngineVersion()) ? "" : "Not compatible");
					asset = asset.replaceAll("%price%", item.getPrice() == 0 ? "Free" : (item.getPrice() + " USD"));
				}*/
				data.append(asset);
				data.append("</td>");
				i[0]++;
			});
			if (i[0] % itemsInLine != 0)
				data.append("</tr>");
			data.append("</table>");
		}

		html = html.replace("%body%", data.toString());
		_textPane1.setText(html);
		if (_itemsPerPage == 8) {
			_textPane1.setCaretPosition(0);
			_scrollPane.getVerticalScrollBar().setValue(0);
		}
	}

	private void updateOwnedAssetsList() {
		String html = HtmlUtils.getBaseHtml();
		html = html.replace("%head%", HtmlUtils.getMarketHead());
		StringBuilder data = new StringBuilder("<p align=\"center\" style=\"margin-top: 10\" style=\"font-family: Lato, Helvetica, Arial, sans-serif\">\n" +
				"      <span style=\"font-size: 16px; color: #808080; text-shadow: 2px 2px #ff0000;\">Owned Assets</span><br>\n" +
				"    </p><br>");
		int itemsInLine = 4;
		int i = 0;
		if (SessionManager.getInstance().getUser().getOwnedItems() != null) {
			data.append("<table class=\"asset-container\">");
			for (EpicItem item : SessionManager.getInstance().getUser().getOwnedItems()) {
				if (i >= _itemsPerPage)
					break;
				if (i % itemsInLine == 0) {
					data.append("<tr>");
				}
				data.append("<td>");
				String asset = HtmlUtils.getAssetDiv();
				asset = asset.replaceAll("%category%", item.getCategories().size() == 0 ? "Unknown" : item.getCategories().get(0).getName());
				asset = asset.replaceAll("%id%", item.getCatalogItemId());
				String name = HtmlUtils.findText(item.getName(), 195, HtmlUtils.FONT_TITLE);
				asset = asset.replaceAll("%title%", name);
				asset = asset.replaceAll("%creator%", item.getSellerName());
				asset = asset.replaceAll("%image%", item.getThumbnail().getUrl());
				asset = asset.replaceAll("%owned%", HtmlUtils.getAssetDivOwner());
				asset = asset.replaceAll("%price%", item.isCompatible(SessionManager.getInstance().getUser().getUnrealEngineVersion()) ? "" : "Not compatible");
				data.append(asset);
				data.append("</td>");
				i++;
				if (i % itemsInLine == 0)
					data.append("</tr>");
			}
			data.append("</table>");
		}

		html = html.replace("%body%", data.toString());
		_textPane1.setText(html);
		if (_itemsPerPage == 8) {
			_textPane1.setCaretPosition(0);
			_scrollPane.getVerticalScrollBar().setValue(0);
		}
	}

	private void showItemInfo(String catalogItemId, int startAt) {
		String html = HtmlUtils.getBaseHtml();
		html = html.replace("%head%", HtmlUtils.getAssetInfoHead());

		String data = "";

		EpicItem item = MarketplaceManager.getInstance().getItem(catalogItemId);

		if (item != null) {
			data = HtmlUtils.getAssetInfo();
			data = data.replaceAll("%title%", item.getName());
			data = data.replaceAll("%description%", item.getDescription());
			data = data.replaceAll("%longDescription%", item.getLongDescription());
			data = data.replaceAll("%techDescription%", item.getTechnicalDetails());

			String firstImage = "";
			String images = "";
			String navigation = "";
			int photosPerPage = 3;
			System.out.println(item.getImages().size());
			if (item.getImages().size() > 0) {
				String images1 = "";
				int firstPhoto = Math.max(0, startAt + 1 == item.getImages().size() ? startAt - 2 : startAt - 1);
				int lastPhoto = Math.min(item.getImages().size(), startAt == 0 ? startAt + 3 : startAt + 2);
				for (int i = firstPhoto; i < lastPhoto; i++) {
					if (i == startAt) {
						firstImage = "<img src=\"" + item.getImages().get(i).getUrl() + "\" class=\"\" width=\"640\" height=\"360\">";
//						continue;
					}
					if (item.getImages().size() <= i)
						break;

					EpicImage image = item.getImages().get(i);
					images1 += "<td " + (i == startAt ? "style=\"background-color: #cccccc\"" : "") + ">";
					if (i == startAt)
						images1 += "<img  src=\"" + image.getUrl() + "\" class=\"\" width=\"192\" height=\"108\"></a>";
					else
						images1 += "<a href=\"item " + catalogItemId + " " + i + "\"><img src=\"" + image.getUrl() + "\" class=\"\" width=\"192\" height=\"108\"></a>";
					images1 += "</td>";
				}
				images += images1;
				if (startAt == 0)
					navigation = "<table><tr><td style=\"width: 50px\"> </td>%images%<td style=\"width: 50px\"><a href=\"%nextBypass%\">Next</a></td></tr></table>";
				else if (startAt + 1 == item.getImages().size())
					navigation = "<table><tr><td style=\"width: 50px\"><a href=\"%prevBypass%\">Prev</a></td>%images%<td style=\"width: 50px\"> </td></tr></table>";
				else
					navigation = "<table><tr><td style=\"width: 50px\"><a href=\"%prevBypass%\">Prev</a></td>%images%<td style=\"width: 50px\"><a href=\"%nextBypass%\">Next</a></td></tr></table>";
			}
			data = data.replaceAll("%firstImage%", firstImage);
			data = data.replaceAll("%navigation%", navigation);
			String versions;
			double lowestVersion = Double.MAX_VALUE;
			double highestVersion = Double.MIN_VALUE;
			String lowVersion = "";
			String highVersion = "";
			if (item.getReleases() != null) {
				for (int i = 0; i < item.getReleases().size(); i++) {
					if (lowestVersion > item.getReleases().get(i).getLowestVersion()) {
						lowestVersion = item.getReleases().get(i).getLowestVersion();
						lowVersion = item.getReleases().get(i).getCompatibility().get(lowestVersion);
					}
					if (highestVersion < item.getReleases().get(i).getHighestVersion()) {
						highestVersion = item.getReleases().get(i).getHighestVersion();
						highVersion = item.getReleases().get(i).getCompatibility().get(highestVersion);
					}
				}
			}
			if (lowVersion.equalsIgnoreCase(highVersion))
				versions = lowVersion + "";
			else
				versions = lowVersion + " - " + highVersion;
			data = data.replaceAll("%versions%", versions);

			StringBuilder platforms = new StringBuilder();

			if (item.getReleases() != null) {
				if (!item.getReleases().isEmpty()) {
					for (String platform : item.getReleases().get(0).getPlatforms())
						platforms.append("<div class=\"text\">").append(platform).append("</div>");
				}
			}
			data = data.replaceAll("%platforms%", platforms.toString());

			data = data.replaceAll("%images%", images);

			String downloadButton;

			if (item.isOwned())
				downloadButton = "<div class=\"download-button\"><a href=\"download_item " + catalogItemId + "\" class=\"btn\">Download</a></div>";
			else
				downloadButton = "<div class=\"download-button\"><a href=\"https://www.unrealengine.com/marketplace/" + item.getUrlPart() + "\" class=\"btn\">Go to Website</a></div><br>Price: " + (item.getPrice() == 0 ? "Free" : (item.getPrice() + " USD"));
			data = data.replaceAll("%download%", downloadButton);

			String prevBypass = "item " + catalogItemId + " " + (startAt - 1);
			String nextBypass = "item " + catalogItemId + " " + (startAt + 1);

			if (data.contains("%prevBypass%"))
				data = data.replaceAll("%prevBypass%", prevBypass);
			if (data.contains("%nextBypass%"))
				data = data.replaceAll("%nextBypass%", nextBypass);

			data = data.replaceAll("%backBypass%", "back");
		}

		html = html.replace("%body%", data);
		_textPane1.setText(html);
		_textPane1.setCaretPosition(0);
		_scrollPane.getVerticalScrollBar().setValue(0);
	}

	private void startDownload(String catalogItemId) {
		if (SessionManager.getInstance().getUser().getCurrentProject() == null || SessionManager.getInstance().getUser().getCurrentProject().length() < 1) {
			JOptionPane.showMessageDialog(this, "You have to select project in library!", "No selected project!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		EpicItem item = MarketplaceManager.getInstance().getItem(catalogItemId);
		if (item == null)
			return;

		if (!item.isCompatible(SessionManager.getInstance().getUser().getUnrealEngineVersion())) {
			JOptionPane.showMessageDialog(this, "This asset pack is not compatible with your [" + SessionManager.getInstance().getUser().getUnrealEngineVersion() + "] engine version!", "Not compatible!", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (!item.isOwned())
			return;

		marketplaceButton.setEnabled(false);
		libraryButton.setEnabled(false);
		ownedAssetsButton.setEnabled(false);
		_scrollPane.setEnabled(false);
		_textPane1.setEnabled(false);
		DownloadForm.getInstance().startDownloading(item);
		_downloadThread = new Thread(item::startDownloading);
		_downloadThread.start();
	}

	public void finishDownload() {
        marketplaceButton.setEnabled(false); // TODO:
        libraryButton.setEnabled(true);
        ownedAssetsButton.setEnabled(true);
        _scrollPane.setEnabled(true);
        _textPane1.setEnabled(true);
	}

	public void updateProjectsList() {
		String html = "<html><head><style>a {" +
				"font-family: FreeSerif;" +
				"color: #0aaff1;" +
				"font-weight: 700;" +
				"font-size: 12px;" +
				"}</style></head><body>%s</body></html>";
		StringBuilder data = new StringBuilder("<p align=\"center\" style=\"margin-top: 10\" style=\"font-family: Lato, Helvetica, Arial, sans-serif\">\n" +
				"      <span style=\"font-size: 16px; color: #808080; text-shadow: 2px 2px #ff0000;\">Select available project</span><br>\n" +
				"    </p><br>");
		if (SessionManager.getInstance().getUser().getProjects().size() > 0) {
			data.append("<table style=\"text-align: center;\">");
			int i = 0;
			int elementsInRow = 4;
			for (String projectName : SessionManager.getInstance().getUser().getProjects().keySet()) {
				if (i % elementsInRow == 0)
					data.append("<tr>");
				data.append("<td width=235 style=\"border: 1px solid #999999; text-align: center;");
				data.append("\"><a href=\"project ").append(projectName).append("\"");
				if (projectName.equals(SessionManager.getInstance().getUser().getCurrentProject()))
					data.append(" style=\"color: #ffa64d !important;\"");
				data.append(">").append(projectName).append("</a>").append("</td>");
				i++;
				if (i % elementsInRow == 0)
					data.append("</tr>");
			}
			if (i % elementsInRow != 0)
				data.append("</tr>");
			data.append("</table>");
		}
		html = html.replace("%s", data.toString());
		_projectsList.setText(html);
	}

	public void setEngineVersion(double version) {
		if (version == 0) {
			_engineVersion.setText("None");
			_launchUE4Button.setEnabled(false);
			return;
		}
		_engineVersion.setText(version + "");
		_launchUE4Button.setEnabled(false); // TODO:
	}

	public void setEngineInstallDir(String dir) {
		_engineInstallDir.setText(dir);
	}

	public MainForm setUsernamePane(String username) {
		usernamePane.setText(username);
		return this;
	}

	public void initialize() {
		setEngineInstallDir(SessionManager.getInstance().getUser().getUe4InstallLocation());
		disableActions();
		EngineManager.getInstance().readEngineData();
		MarketplaceManager.getInstance().createMarketplace();
//		SessionManager.getInstance().getUser().loadOwnedAssets();
		hideLoading();
		enableActions();
	}

	private static MainForm _instance = null;

	public synchronized static MainForm getInstance() {
		if (_instance == null)
			_instance = new MainForm();
		return _instance;
	}

	public synchronized static MainForm getInstance(boolean reinitialize) {
		if (reinitialize || _instance == null)
			_instance = new MainForm();
		return _instance;
	}
}
