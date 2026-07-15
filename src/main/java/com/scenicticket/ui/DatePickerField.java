package com.scenicticket.ui;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** A compact, non-editable date field that opens a visual date selector when clicked. */
final class DatePickerField extends JPanel {
    private final JFormattedTextField textField = new JFormattedTextField();
    private LocalDate date;

    DatePickerField(LocalDate initialDate) {
        super(new BorderLayout(2, 0));
        textField.setColumns(10);
        textField.setEditable(false);
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton button = UiComponents.secondaryButton("选择");
        button.setToolTipText("点击选择日期");
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                openPicker();
            }
        });
        button.addActionListener(event -> openPicker());
        add(textField, BorderLayout.CENTER);
        add(button, BorderLayout.EAST);
        setDate(initialDate);
    }

    LocalDate getDate() {
        return date;
    }

    void setDate(LocalDate value) {
        date = value == null ? LocalDate.now() : value;
        textField.setText(date.toString());
    }

    private void openPicker() {
        Date initial = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner spinner = new JSpinner(new SpinnerDateModel(initial, null, null, java.util.Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        int result = JOptionPane.showConfirmDialog(this, spinner, "选择日期",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            Date selected = (Date) spinner.getValue();
            setDate(selected.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
    }
}
