package nl.jq.itech.client.ui;

import nl.jq.itech.client.helper.SocketHelper;
import nl.jq.itech.client.helper.model.Command;
import nl.jq.itech.client.helper.model.Message;
import nl.jq.itech.client.helper.model.Request;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

/**
 * KickPanel is basically a dialog for an emergency meeting
 */
public class KickPanel extends JDialog implements SocketHelper.Interfaces.OnReceivedListener {
    private final DefaultListModel<String> votes;
    private final HashMap<String, Integer> users;
    private SocketHelper helper;
    private JPanel contentPane;
    private JButton buttonCancel;
    private JButton buttonVote;
    private JList<String> voteList;
    private boolean voted;

    public KickPanel(SocketHelper helper) {
        setContentPane(contentPane);
        setTitle("Emergency Meeting");
        this.helper = helper;
        helper.addOnReceivedListener(this);
        votes = new DefaultListModel<>();
        voteList.setModel(votes);
        users = new HashMap<>();

        buttonCancel.addActionListener(e -> skip());
        buttonVote.addActionListener(e -> vote());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        // Skip and exit on close
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

    /**
     * If not already voted, vote skip
     */
    private void skip() {
        if (!voted) {
            voted = true;
            Request.build(helper)
                    .setCommand(Command.VOTE_SKIP)
                    .setOnResponse((success, message) -> !success)
                    .setMaxRetries(5)
                    .send();
        }
    }

    /**
     * Dispose of this dialog
     */
    private void exit() {
        helper.removeOnReceivedListener(this);
        dispose();
    }

    /**
     * If not already voted, vote [username]
     */
    private void vote() {
        if (!voted) {
            voted = true;
            String selected = voteList.getSelectedValue();
            if (selected != null) {
                selected = selected.split(" ", 2)[0];
                selected = selected.substring("<html>".length());

                Request.build(helper)
                        .setMessage(Command.VOTE_KICK_USER, selected)
                        .setOnResponse((success, message) -> !success)
                        .setMaxRetries(5)
                        .send();
            }
        }
    }

    /**
     * Get users from USERS response
     *
     * @param payload 200 name;name;name
     */
    private void getUsers(String payload) {
        for (String s : payload.split(";")) {
            users.putIfAbsent(s, 0);
        }
    }

    /**
     * Update the dialog with new votes and such
     */
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

    /**
     * Handle messages when they come in
     *
     * @param message Message
     */
    @Override
    public void onReceive(Message message) {
        switch (message.getCommand()) {
            case JOINED_ROOM:
                // If someone joined the room, add them to the dialog options
                String username = message.getPayload().split(" ", 2)[0];
                users.putIfAbsent(username, 0);
                break;
            case VOTES:
                // If someone voted, we will receive a VOTED message containing the new data
                String[] names = message.getPayload().split(";");
                for (String name : names) {
                    String[] data = name.split(" ", 2);
                    String username2 = data[0];
                    int votes = Integer.parseInt(data[1]);
                    users.put(username2, votes);
                }
                break;
            case KICK_RESULT:
                // If there has been a kick result, the meeting has ended
                exit();
                break;
        }

        // Update the dialog if it's visible
        if (isVisible()) {
            updateList();
        }
    }
}
