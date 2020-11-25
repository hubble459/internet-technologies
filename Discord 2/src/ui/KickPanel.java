package ui;

import model.Command;
import model.Message;
import util.ServerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class KickPanel extends JDialog implements ServerUtil.OnReceive {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JPanel tablePanel;
    private final HashMap<String, Integer> users;


    public KickPanel() {
        users = new HashMap<>();
        setContentPane(contentPane);
//        setModal(true);
        ServerUtil.onReceive(this);

        tablePanel.setLayout(new FlowLayout());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        ServerUtil.send(Command.ROOM);
    }

    private void onCancel() {
        stop();
    }

    private void stop() {
        ServerUtil.removeOnReceive(this);

        dispose();
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
                updateTable();
                break;
            case KICKED:
                stop();
                break;
            case VOTE_KICK:
                String[] data = message.getPayload().split(" ", 2);
                String username = data[0];
                int votes = Integer.parseInt(data[1]);
                users.put(username, votes);
                updateTable();
                break;
        }
    }

    private void getUsers(String payload) {
        for (String s : payload.split(";")) {
            users.putIfAbsent(s, 0);
        }
        updateTable();
    }

    private void updateTable() {
        SwingUtilities.invokeLater(() -> {
            tablePanel.removeAll();

            String[][] data = new String[users.size()][2];
            users.forEach(new BiConsumer<>() {
                private int count = 0;

                @Override
                public void accept(String s, Integer integer) {
                    data[count][0] = s;
                    data[count++][1] = String.valueOf(integer);
                }
            });
            System.out.println(Arrays.deepToString(data));

            String[] columnNames = new String[]{"Username", "Votes"};
            JTable table = new JTable(data, columnNames);
            JScrollPane scrollPane = new JScrollPane(table);

            tablePanel.add(scrollPane);
        });
    }
}
