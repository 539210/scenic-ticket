package com.scenicticket.ui;

import com.scenicticket.dto.TicketAvailabilityDTO;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;

public final class TicketAvailabilityDialogPanel extends JPanel {
    private static final String[] COLUMNS = {
            "票种ID", "票种名称", "游玩日期", "原价", "优惠", "折后价", "可售库存"
    };
    private static final int[] COLUMN_WIDTHS = {75, 120, 110, 90, 80, 90, 90};

    private final LocalDate today;
    private final DatePickerField startDateField;
    private final DatePickerField endDateField;

    public TicketAvailabilityDialogPanel(String itemTitle, LocalDate today) {
        super(new BorderLayout());
        if (itemTitle == null || itemTitle.isBlank()) {
            throw new IllegalArgumentException("景点名称不能为空");
        }
        this.today = java.util.Objects.requireNonNull(today, "today");
        startDateField = new DatePickerField(today.plusDays(1));
        endDateField = new DatePickerField(today.plusDays(14));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "开始日期", startDateField);
        addField(fields, 1, "结束日期", endDateField);
        add(UiComponents.card("查询“" + itemTitle + "”可售票种与日期", fields), BorderLayout.CENTER);
    }

    private void addField(JPanel target, int row, String label, java.awt.Component field) {
        GridBagConstraints labelConstraints = constraints(row, 0);
        target.add(new JLabel(label), labelConstraints);
        GridBagConstraints fieldConstraints = constraints(row, 1);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        target.add(field, fieldConstraints);
    }

    private GridBagConstraints constraints(int row, int column) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    public DateRange request() {
        LocalDate startDate = startDateField.getDate();
        LocalDate endDate = endDateField.getDate();
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("开始日期不能早于今天");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        return new DateRange(startDate, endDate);
    }

    public static Component results(List<TicketAvailabilityDTO> options) {
        if (options == null || options.isEmpty()) {
            return new JLabel("所选日期范围暂无可售票种或库存");
        }
        JTable table = UiComponents.table(resultModel(options));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int index = 0; index < COLUMN_WIDTHS.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(COLUMN_WIDTHS[index]);
        }
        return new JScrollPane(table);
    }

    static DefaultTableModel resultModel(List<TicketAvailabilityDTO> options) {
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (TicketAvailabilityDTO option : options) {
            model.addRow(new Object[]{option.ticketType().getTicketTypeId(), option.ticketType().getName(),
                    option.inventory().getVisitDate(), UiFormatters.money(option.ticketType().getOriginalPrice()),
                    UiFormatters.discount(option.ticketType().getDiscountRate()),
                    UiFormatters.money(option.discountedPrice()), option.inventory().getAvailableStock()});
        }
        return model;
    }

    void setDates(String startDate, String endDate) {
        startDateField.setDate(UiInputParsers.requiredDate(startDate, "开始日期"));
        endDateField.setDate(UiInputParsers.requiredDate(endDate, "结束日期"));
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
