package launcher;

import launcher.managers.EngineManager;
import launcher.managers.SessionManager;
import launcher.objects.EpicItem;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class DownloadForm extends JFrame {
	private JTextPane _mainInfo;
	private JPanel _panel1;
	private JProgressBar _progressBar1;
	private JButton _stopDownloadButton;
	private JProgressBar _progressBar2;

	private double _progress1;
	private double _progress2;
	private EpicItem _item;

	private String _minorProgressText;
	private String _mainProgressText;

	public DownloadForm() {
		super("Download");
		_item = null;
		setContentPane(_panel1);
		setVisible(false);
		setResizable(false);
		pack();
		_stopDownloadButton.addActionListener(actionEvent -> {
			/*Main.getInstance().getEpicAPI().setStopDownload(true);
			Main.getInstance().getMainForm().finishDownload(_item.getReleases().get(0).getAppId(), false);*/
		});
	}

	public void startDownloading(EpicItem item) {
		_item = item;
		_progress1 = 0;
		_progress2 = 0;
		_minorProgressText = "";
		_mainProgressText = "";
		_progressBar1.setValue(0);
		_progressBar1.setString(_minorProgressText + " [" + String.format("%.2f", _progress1) + "%]");
		_progressBar2.setValue(0);
		_progressBar2.setString(_mainProgressText + " [" + String.format("%.2f", _progress2) + "%]");
		setMainInfoText("");
		setVisible(true);
	}

	public void setMinorProgressText(String text) {
		_minorProgressText = text;
		_progressBar1.setString(_minorProgressText + " [" + String.format("%.2f", _progress1) + "%]");
	}

	public void setMainProgressText(String mainProgressText) {
		_mainProgressText = mainProgressText;
		_progressBar2.setString(_mainProgressText + " [" + String.format("%.2f", _progress2) + "%]");
	}

	public void increase1Progress(double progress) {
		_progress1 += progress;
		_progressBar1.setValue((int) _progress1);
		_progressBar1.setString(_minorProgressText + " [" + String.format("%.2f", _progress1) + "%]");
	}

	public void clear1Progress() {
		_progress1 = 0;
		_progressBar1.setValue((int) _progress1);
	}

	public void increase2Progress(double progress) {
		_progress2 += progress;
		_progressBar2.setValue((int) _progress2);
		_progressBar2.setString(_mainProgressText + " [" + String.format("%.2f", _progress2) + "%]");
	}

	public synchronized void setMainInfoText(String text) {
		try {
			_mainInfo.setText("<html>\n" +
					"  <head>\n" +
					"    \n" +
					"  </head>\n" +
					"  <body>\n" +
					"    <p align=\"center\" style=\"margin-top: 10\" style=\"font-family: FreeSerif; font-size: 12px;color: #444444; font-weight: bold;\">\n" +
					"    \t" + text + "\n" +
					"    </p>\n" +
					"  </body>\n" +
					"</html>");
		} catch (NullPointerException npe) {
			System.out.println(npe.toString());
		}
	}


	public static DownloadForm getInstance() {
		return DownloadForm.SingletonHolder._instance;
	}

	public void finishDownload(boolean openFolder) {
		if (openFolder) {
			try {
				Desktop.getDesktop().open(new File(SessionManager.getInstance().getUser().getProjects().get(SessionManager.getInstance().getUser().getCurrentProject())));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		setVisible(false);
		MainForm.getInstance().finishDownload();
	}

	private static class SingletonHolder {
		protected static final DownloadForm _instance = new DownloadForm();
	}
}
