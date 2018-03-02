package launcher;

import launcher.objects.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        Filter(double price)
        {
            _price = price;
        }

        public double getPrice()
        {
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

    public static void main(String[] args) {
        new MainForm("erlandys56@gmail.com");
    }

    MainForm(String username) {
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
        usernamePane.setText(username);
        ((HTMLEditorKit) _textPane1.getEditorKitForContentType("text/html")).setAutoFormSubmission(false);
        list1.addListSelectionListener(listSelectionEvent -> {
            if (_currentCategory == Main.getInstance().getCategory(list1.getSelectedValue().toString()) && !_viewingItem)
                return;
            _currentCategory = Main.getInstance().getCategory(list1.getSelectedValue().toString());
            _itemsPerPage = 8;
            _viewingItem = false;
            updateMarketplaceList();
        });
        marketplaceButton.addActionListener(actionEvent -> {
            if (_viewType.equals(ViewType.Marketplace) && !_viewingItem)
                return;
            marketplaceButton.setEnabled(false);
            switch (_viewType)
            {
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
            if (_currentCategory == null)
            {
                list1.setSelectedIndex(0);
                return;
            }
            updateMarketplaceList();

        });
        libraryButton.addActionListener(actionEvent -> {
            if (_viewType.equals(ViewType.Library) && !_viewingItem)
                return;
            libraryButton.setEnabled(false);
            switch (_viewType)
            {
                case OwnedAssets:
                    ownedAssetsButton.setEnabled(true);
                    break;
                case Marketplace:
                    marketplaceButton.setEnabled(true);
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
            switch (_viewType)
            {
                case Library:
                    libraryButton.setEnabled(true);
                    break;
                case Marketplace:
                    marketplaceButton.setEnabled(true);
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
            }
            else if (e.getDescription().startsWith("download_item")) {
                startDownload(e.getDescription().split(" ")[1]);
            }
            else if (e.getDescription().equalsIgnoreCase("back")) {
                switch (_viewType)
                {
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
            }
            else if (e.getDescription().startsWith("filter"))
            {
                _filter = Filter.valueOf(e.getDescription().split(" ")[1]);
                _itemsPerPage = 8;
                updateMarketplaceList();
            }
            else if (e.getDescription() != null && e.getDescription().length() > 0)
            {
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
                String data[] = e.getDescription().split((" "));
                _selectedProject.setText(data[1]);
                Main.getInstance().setCurrentProject(data[1]);
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
                if (_itemsPerPage >= Main.getInstance().getOwnedAssets().size())
                    return;
                _itemsPerPage += 8;
                updateOwnedAssetsList();
            }
        });
        _selectButton.addActionListener(actionEvent -> {
            JFileChooser chooser;
            chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(Main.getInstance().getUe4InstallDir().length() < 1 ? "." : Main.getInstance().getUe4InstallDir()));
            chooser.setDialogTitle("Select UE4 install directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                Main.getInstance().getEpicAPI().updateInstallDir(chooser.getSelectedFile().toString());
        });
        _updateButton.addActionListener(actionEvent -> Main.getInstance().getEpicAPI().readEngineData());
        _launchUE4Button.addActionListener(actionEvent -> {
            new Thread(() -> {
                String launch = Main.getInstance().getUe4InstallDir() + "/Engine/Binaries/Linux/UE4Editor";
                String project = "";
                if (Main.getInstance().getCurrentProject() != null && Main.getInstance().getCurrentProject().length() > 0)
                    project = Main.getInstance().getProjects().get(Main.getInstance().getCurrentProject()) + Main.getInstance().getCurrentProject() + ".uproject";
                File file = new File(launch);
                file.setExecutable(true);
                try {
                    Process p;
                    p = new ProcessBuilder(launch, project, "&").start();
                    p.waitFor();
                } catch (Exception ignored) {
                }
            }).start();
        });
        _reloadOwnedAssetsButton.addActionListener(actionEvent -> {
            _reloadingOwnedAssets = true;
            _loadingLabel.setVisible(true);
            new Thread(() ->
            {
                Main.getInstance().getEpicAPI().getOwnedAssets();
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
                _loadingLabel.setVisible(false);
            }).start();
        });
        _logoutButton.addActionListener(actionEvent -> Main.getInstance().doLogout());
    }

    public void updateCategoriesList(Collection<String> categories) {
        String data[] = new String[categories.size()];
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

    public void setLoadingText(String text) {
        _loadingLabel.setText(text);
    }

    public void increaseLoadingBar(int percent) {
        _loadingPercent += (double) percent;
        _loadingProgressBar.setValue(_loadingProgressBar.getValue() + percent);
    }

    public void increaseLoadingBar(double percent) {
        _loadingPercent += percent;
        _loadingProgressBar.setValue((int) _loadingPercent);
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
            marketplaceButton.setEnabled(true);
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
            _currentCategory.getItems().values().stream().filter(item -> item.getPrice() <= _filter.getPrice()).forEach(item ->  {
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
                asset = asset.replaceAll("%image%", item.getThumbnail());
                if (Main.getInstance().getOwnedAsset(item.getCatalogItemId()) != null) {
                    asset = asset.replaceAll("%owned%", HtmlUtils.getAssetDivOwner());
                    asset = asset.replaceAll("%price%", item.isCompatible(Main.getInstance().getEngineVersion()) ? "" : "Not compatible");
                } else {
                    asset = asset.replaceAll("%owned%", item.isCompatible(Main.getInstance().getEngineVersion()) ? "" : "Not compatible");
                    asset = asset.replaceAll("%price%", item.getPrice() == 0 ? "Free" : (item.getPrice() + " USD"));
                }
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
        if (Main.getInstance() != null && Main.getInstance().getOwnedAssets() != null) {
            data.append("<table class=\"asset-container\">");
            for (EpicOwnedAsset ownedAsset : Main.getInstance().getOwnedAssets()) {
                if (i >= _itemsPerPage)
                    break;
                EpicItem item = ownedAsset.getItem();
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
                asset = asset.replaceAll("%image%", item.getThumbnail());
                if (Main.getInstance().getOwnedAsset(item.getCatalogItemId()) != null) {
                    asset = asset.replaceAll("%owned%", HtmlUtils.getAssetDivOwner());
                    asset = asset.replaceAll("%price%", item.isCompatible(Main.getInstance().getEngineVersion()) ? "" : "Not compatible");
                } else {
                    asset = asset.replaceAll("%owned%", item.isCompatible(Main.getInstance().getEngineVersion()) ? "" : "Not compatible");
                    asset = asset.replaceAll("%price%", item.getPrice() == 0 ? "Free" : (item.getPrice() + " USD"));
                }
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

        EpicItem item = Main.getInstance().getItemByCatalogId(catalogItemId);

        if (item != null) {
            data = HtmlUtils.getAssetInfo();
            data = data.replaceAll("%title%", item.getName());
            data = data.replaceAll("%description%", item.getDescription());
            data = data.replaceAll("%longDescription%", item.getLongDescription());
            data = data.replaceAll("%techDescription%", item.getTechnicalDetails());

            String firstImage = "";
            String images = "";
            int photosPerPage = 4;
            if (item.getImages().size() > 0) {
                String images1 = "";
                for (int i = startAt; i < startAt + photosPerPage; i++) {
                    if (i == startAt) {
                        firstImage = "<img src=\"" + item.getImages().get(i).getUrl() + "\" class=\"\" width=\"640\" height=\"360\">";
                        continue;
                    }
                    if (item.getImages().size() <= i)
                        break;

                    EpicImage image = item.getImages().get(i);
                    images1 += "<td>";
                    images1 += "<a href=\"item " + catalogItemId + " " + i + "\"><img src=\"" + image.getUrl() + "\" class=\"\" width=\"192\" height=\"108\"></a>";
                    images1 += "</td>";
                }
                images += images1;
            }
            data = data.replaceAll("%firstImage%", firstImage);
            String versions;
            double lowestVersion = Double.MAX_VALUE;
            double highestVersion = Double.MIN_VALUE;
            String lowVersion = "";
            String highVersion = "";
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
            if (lowestVersion == highestVersion)
                versions = lowVersion + "";
            else
                versions = lowVersion + " - " + highVersion;
            data = data.replaceAll("%versions%", versions);

            StringBuilder platforms = new StringBuilder();

            for (String platform : item.getReleases().get(0).getPlatforms())
                platforms.append("<div class=\"text\">").append(platform).append("</div>");
            data = data.replaceAll("%platforms%", platforms.toString());

            data = data.replaceAll("%images%", images);

            String downloadButton;

            if (Main.getInstance().containsOwnedAsset(catalogItemId))
                downloadButton = "<div class=\"download-button\"><a href=\"download_item " + catalogItemId + "\" class=\"btn\">Download</a></div>";
            else
                downloadButton = "<div class=\"download-button\"><a href=\"https://www.unrealengine.com/marketplace/" + item.getUrlPart() + "\" class=\"btn\">Go to Website</a></div><br>Price: " + (item.getPrice() == 0 ? "Free" : (item.getPrice() + " USD"));
            data = data.replaceAll("%download%", downloadButton);

            String prevBypass = "";
            String nextBypass = "";

            if (startAt > 0)
                prevBypass = "item " + catalogItemId + " " + (startAt - 1);
            if (startAt + photosPerPage < item.getImages().size() - 1)
                nextBypass = "item " + catalogItemId + " " + (startAt + 1);
            data = data.replaceAll("%prevBypass%", prevBypass);
            data = data.replaceAll("%nextBypass%", nextBypass);

            data = data.replaceAll("%backBypass%", "back");
        }

        html = html.replace("%body%", data);
        _textPane1.setText(html);
        _textPane1.setCaretPosition(0);
        _scrollPane.getVerticalScrollBar().setValue(0);
    }

    private void startDownload(String catalogItemId) {
        if (Main.getInstance().getCurrentProject() == null || Main.getInstance().getCurrentProject().length() < 1) {
            JOptionPane.showMessageDialog(this, "You have to select project in library!", "No selected project!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        EpicItem item = Main.getInstance().getItemByCatalogId(catalogItemId);
        if (item == null)
            return;

        if (!item.isCompatible(Main.getInstance().getEngineVersion())) {
            JOptionPane.showMessageDialog(this, "This asset pack is not compatible with your [" + Main.getInstance().getEngineVersion() + "] engine version!", "No compatible!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean hasItem = Main.getInstance().containsOwnedAsset(catalogItemId);
        if (!hasItem)
            return;

        marketplaceButton.setEnabled(false);
        libraryButton.setEnabled(false);
        ownedAssetsButton.setEnabled(false);
        _scrollPane.setEnabled(false);
        _textPane1.setEnabled(false);
        Main.getInstance().showDownloadForm(item);
        _downloadThread = new Thread(() -> Main.getInstance().getEpicAPI().downloadItem(item));
        _downloadThread.start();
    }

    public void finishDownload(String appName, boolean openFolder) {

        if (openFolder) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(new File(Main.getInstance().getProjects().get(Main.getInstance().getCurrentProject())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Main.getInstance().getEpicAPI().setStopDownload(false);
            if (_downloadThread != null) {
                _downloadThread.interrupt();
                _downloadThread.stop();
                _downloadThread = null;
            }
            File dir = new File("." + appName);
            if (dir.exists())
                EpicAPI.deleteFolder(dir);
        }
        Main.getInstance().getDownloadForm().dispose();
        marketplaceButton.setEnabled(true);
        libraryButton.setEnabled(true);
        ownedAssetsButton.setEnabled(true);
        _scrollPane.setEnabled(true);
        _textPane1.setEnabled(true);
    }

    public void updateProjectsList()
    {
        String html = "<html><head><style>a {" +
                "font-family: FreeSerif;" +
                "color: #0aaff1;" +
                "font-weight: 700;" +
                "font-size: 12px;" +
                "}</style></head><body>%s</body></html>";
        StringBuilder data = new StringBuilder("<p align=\"center\" style=\"margin-top: 10\" style=\"font-family: Lato, Helvetica, Arial, sans-serif\">\n" +
                "      <span style=\"font-size: 16px; color: #808080; text-shadow: 2px 2px #ff0000;\">Select available project</span><br>\n" +
                "    </p><br>");
        if (Main.getInstance().getProjects().size() > 0) {
            data.append("<table style=\"text-align: center;\">");
            int i = 0;
            int elementsInRow = 4;
            for (String projectName : Main.getInstance().getProjects().keySet()) {
                if (i % elementsInRow == 0)
                    data.append("<tr>");
                data.append("<td width=235 style=\"border: 1px solid #999999; text-align: center;");
                data.append("\"><a href=\"project ").append(projectName).append("\"");
                if (projectName.equals(Main.getInstance().getCurrentProject()))
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

    public void setEngineVersion(double version)
    {
        if (version == 0) {
            _engineVersion.setText("None");
            _launchUE4Button.setEnabled(false);
            return;
        }
        _engineVersion.setText(version + "");
        _launchUE4Button.setEnabled(true);
    }

    public void setEngineInstallDir(String dir)
    {
        _engineInstallDir.setText(dir);
    }
}
