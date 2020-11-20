package ui;

import util.SocketUtil;

import javax.swing.*;
import java.awt.*;

public class MainScreen extends JFrame {
    private JPanel mainPanel;
    private ChannelPanel channelPanel;
    private ChatPanel chatPanel;

    public MainScreen(String title) throws HeadlessException {
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        setupMenu();
        pack();
        setLocationRelativeTo(null);
        setResizable(false);

        setVisible(true);

        channelPanel.setChatPanel(chatPanel);

        SocketUtil.connect();
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu options = new JMenu("Options");

        options.add("Close");
        options.add("Logout");
        options.add("Help");

        menuBar.add(options);

        setJMenuBar(menuBar);
    }
}
