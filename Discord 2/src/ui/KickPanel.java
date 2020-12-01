package ui;

import model.Command;
import model.Message;
import util.ServerUtil;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

public class KickPanel extends JDialog implements ServerUtil.OnReceive {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JButton buttonVote;
    private JList<String> voteList;
    private final DefaultListModel<String> votes;
    private final HashMap<String, Integer> users;

    public KickPanel() {
        setContentPane(contentPane);
//        setModal(true);
        ServerUtil.onReceive(this);
        votes = new DefaultListModel<>();
        voteList.setModel(votes);
        users = new HashMap<>();

        buttonCancel.addActionListener(e -> exit());
        buttonVote.addActionListener(e -> vote());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> exit(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        ServerUtil.send(Command.ROOM);
    }

    private void exit() {
        ServerUtil.removeOnReceive(this);

        dispose();
    }

    private void vote() {
        String selected = voteList.getSelectedValue();
        if (selected != null) {
            selected = selected.split(" ", 2)[0];
            selected = selected.substring("<html>".length());

            ServerUtil.send(Command.VOTE_KICK_USER, selected);
        }
        exit();
    }

    @Override
    public void received(Message message) {
        System.out.println(message.getPayload());
        switch (message.getCommand()) {
            case ROOM:
                getUsers(message.getPayload());
                break;
            case JOINED_ROOM:
                users.putIfAbsent(message.getPayload(), 0);
                updateList();
                break;
            case KICKED:
                exit();
                break;
            case VOTED:
                String[] data = message.getPayload().split(" ", 2);
                String username = data[0];
                int votes = Integer.parseInt(data[1]);
                users.put(username, votes);
                updateList();
                break;
        }
    }

    private void getUsers(String payload) {
        for (String s : payload.split(";")) {
            users.putIfAbsent(s, 0);
        }
        updateList();
    }

    private void updateList() {
        SwingUtilities.invokeLater(() -> {
            votes.clear();
            for (String s : users.keySet()) {
                int v = users.get(s);
                String line = "<html>" + s + " &nbsp<strong>" + v + "</strong></html>";
                votes.addElement(line);
            }
        });
    }
}
