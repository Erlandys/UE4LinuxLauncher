package launcher;

import launcher.objects.EpicItem;

import javax.swing.*;

public class DownloadForm extends JFrame {
    private JTextPane _mainInfo;
    private JPanel _panel1;
    private JProgressBar _progressBar1;
    private JButton _stopDownloadButton;
    private JProgressBar _progressBar2;
    private JLabel _progressLabel1;
    private JLabel _progressLabel2;

    private double _progress1;
    private double _progress2;
    private final EpicItem _item;

    public DownloadForm(EpicItem item) {
        super("Download");
        _item = item;
        setContentPane(_panel1);
        setVisible(true);
        setResizable(false);
        pack();
        _stopDownloadButton.addActionListener(actionEvent -> {
            Main.getInstance().getEpicAPI().setStopDownload(true);
            Main.getInstance().getMainForm().finishDownload(_item.getReleases().get(0).getAppId(), false);
        });
    }

    public void increase1Progress(double progress) {
        _progress1 += progress;
        _progressBar1.setValue((int) _progress1);
        _progressLabel1.setText((int) _progress1 + "%");
    }

    public void clear1Progress() {
        _progress1 = 0;
        _progressBar1.setValue((int) _progress1);
        _progressLabel1.setText((int) _progress1 + "%");
    }

    public void increase2Progress(double progress) {
        _progress2 += progress;
        _progressBar2.setValue((int) _progress2);
        _progressLabel2.setText((int) _progress2 + "%");
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

}
