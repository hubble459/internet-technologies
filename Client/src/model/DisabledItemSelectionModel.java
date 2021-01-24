package model;

import javax.swing.*;

/**
 * In our chat we use a JList but the chat's don't need to be selectable
 * So we use this SelectionModel
 */
public class DisabledItemSelectionModel extends DefaultListSelectionModel {
    @Override
    public void setSelectionInterval(int i0, int i1) {
        super.setSelectionInterval(-1, -1);
    }
}
