package model;

import javax.swing.*;

public class DisabledItemSelectionModel extends DefaultListSelectionModel {
    @Override
    public void setSelectionInterval(int i0, int i1) {
        super.setSelectionInterval(-1, -1);
    }
}
