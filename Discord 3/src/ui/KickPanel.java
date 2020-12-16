package ui;

import helper.SocketHelper;
import helper.model.Command;
import helper.model.Message;
import helper.model.Request;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

public class KickPanel extends JDialog implements SocketHelper.Interfaces.OnReceivedListener {
    private final DefaultListModel<String> votes;
    private final HashMap<String, Integer> users;
    private SocketHelper helper;
    private JPanel contentPane;
    private JButton buttonCancel;
    private JButton buttonVote;
    private JList<String> voteList;

    public KickPanel(SocketHelper helper) {
        setContentPane(contentPane);
        this.helper = helper;
        helper.addOnReceivedListener(this);
        setTitle("Emergency Meeting");
        votes = new DefaultListModel<>();
        voteList.setModel(votes);
        users = new HashMap<>();

        buttonCancel.addActionListener(e -> skip());
        buttonVote.addActionListener(e -> vote());

        // call skip() and exit() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                skip();
                exit();
            }
        });

        // call skip() and exit() on ESCAPE
        contentPane.registerKeyboardAction(e -> {
            skip();
            exit();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        Request.build(helper)
                .setCommand(Command.ROOM)
                .setOnResponse((success, message) -> {
                    getUsers(message.getPayload());
                    updateList();
                    return false;
                })
                .send();
    }

    private void skip() {
        Request.build(helper)
                .setCommand(Command.VOTE_SKIP)
                .send();
    }

    private void exit() {
        helper.removeOnReceivedListener(this);
        dispose();
    }

    private void vote() {
        String selected = voteList.getSelectedValue();
        if (selected != null) {
            selected = selected.split(" ", 2)[0];
            selected = selected.substring("<html>".length());

            Request.build(helper)
                    .setMessage(Command.VOTE_KICK, selected)
                    .send();
        }
    }

    private void getUsers(String payload) {
        for (String s : payload.split(";")) {
            users.putIfAbsent(s, 0);
        }
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

    public void setSocketHelper(SocketHelper helper) {
        this.helper = helper;
    }

    @Override
    public void onReceive(Message message) {
        switch (message.getCommand()) {
            case JOINED_ROOM:
                String username = message.getPayload().split(" ", 2)[0];
                users.putIfAbsent(username, 0);
                break;
            case VOTES:
                String[] names = message.getPayload().split(";");
                for (String name : names) {
                    String[] data = name.split(" ", 2);
                    String username2 = data[0];
                    int votes = Integer.parseInt(data[1]);
                    users.put(username2, votes);
                }
                break;
            case KICK_RESULT:
                exit();
                break;
        }

        if (isVisible()) {
            updateList();
        }
    }
}
