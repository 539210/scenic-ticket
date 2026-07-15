package com.scenicticket.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A compact, non-editable date field that opens a month calendar when clicked. */
final class DatePickerField extends JPanel {
    private final JFormattedTextField textField = new JFormattedTextField();
    private LocalDate date;

    DatePickerField(LocalDate initialDate) {
        super(new BorderLayout());
        setOpaque(false);
        textField.setColumns(10);
        textField.setEditable(false);
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        textField.setToolTipText("点击日期框选择年、月、日");
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                openPicker();
            }
        });
        add(textField, BorderLayout.CENTER);
        setDate(initialDate);
    }

    LocalDate getDate() {
        return date;
    }

    void setDate(LocalDate value) {
        date = value;
        textField.setText(value == null ? "" : value.toString());
    }

    void clearDate() {
        setDate(null);
    }

    private void openPicker() {
        CalendarDatePickerPanel calendar = new CalendarDatePickerPanel(date == null ? LocalDate.now() : date);
        int result = JOptionPane.showConfirmDialog(this, calendar, "选择日期",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            setDate(calendar.getSelectedDate());
        }
    }
}

/** Month calendar used by {@link DatePickerField}; package-visible for focused UI tests. */
final class CalendarDatePickerPanel extends JPanel {
    private static final String[] WEEKDAYS = {"一", "二", "三", "四", "五", "六", "日"};

    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel daysPanel = new JPanel(new GridLayout(6, 7, 3, 3));
    private final List<JButton> dayButtons = new ArrayList<>(42);
    private YearMonth displayedMonth;
    private LocalDate selectedDate;

    CalendarDatePickerPanel(LocalDate initialDate) {
        super(new BorderLayout(6, 8));
        selectedDate = java.util.Objects.requireNonNull(initialDate, "initialDate");
        displayedMonth = YearMonth.from(initialDate);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(330, 270));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        JButton previous = UiComponents.secondaryButton("‹");
        JButton next = UiComponents.secondaryButton("›");
        previous.setToolTipText("上一个月");
        next.setToolTipText("下一个月");
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 16f));
        previous.addActionListener(event -> previousMonth());
        next.addActionListener(event -> nextMonth());
        header.add(previous, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);

        JPanel calendar = new JPanel(new BorderLayout(0, 4));
        JPanel weekdays = new JPanel(new GridLayout(1, 7, 3, 0));
        for (String weekday : WEEKDAYS) {
            JLabel label = new JLabel(weekday, SwingConstants.CENTER);
            label.setForeground(UiTheme.MUTED);
            weekdays.add(label);
        }
        for (int index = 0; index < 42; index++) {
            JButton day = new JButton();
            day.setFocusPainted(false);
            day.setMargin(new java.awt.Insets(1, 1, 1, 1));
            day.addActionListener(event -> selectDay(Integer.parseInt(day.getActionCommand())));
            dayButtons.add(day);
            daysPanel.add(day);
        }
        calendar.add(weekdays, BorderLayout.NORTH);
        calendar.add(daysPanel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        add(calendar, BorderLayout.CENTER);
        refreshCalendar();
    }

    LocalDate getSelectedDate() {
        return selectedDate;
    }

    YearMonth getDisplayedMonth() {
        return displayedMonth;
    }

    void previousMonth() {
        displayedMonth = displayedMonth.minusMonths(1);
        refreshCalendar();
    }

    void nextMonth() {
        displayedMonth = displayedMonth.plusMonths(1);
        refreshCalendar();
    }

    void selectDay(int dayOfMonth) {
        selectedDate = displayedMonth.atDay(dayOfMonth);
        refreshCalendar();
    }

    private void refreshCalendar() {
        monthLabel.setText(displayedMonth.getYear() + "年 " + displayedMonth.getMonthValue() + "月");
        int offset = displayedMonth.atDay(1).getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int index = 0; index < dayButtons.size(); index++) {
            JButton button = dayButtons.get(index);
            int day = index - offset + 1;
            boolean inMonth = day >= 1 && day <= displayedMonth.lengthOfMonth();
            button.setVisible(inMonth);
            button.setEnabled(inMonth);
            if (!inMonth) {
                button.setText("");
                continue;
            }
            button.setText(Integer.toString(day));
            button.setActionCommand(Integer.toString(day));
            LocalDate candidate = displayedMonth.atDay(day);
            boolean selected = candidate.equals(selectedDate);
            button.setOpaque(selected);
            button.setBorderPainted(selected);
            button.setBackground(selected ? UiTheme.PRIMARY : UiTheme.SURFACE);
            button.setForeground(selected ? Color.WHITE : UiTheme.TEXT);
        }
        revalidate();
        repaint();
    }
}
