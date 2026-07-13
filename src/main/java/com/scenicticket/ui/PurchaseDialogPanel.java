package com.scenicticket.ui;

import com.scenicticket.dto.PendingOrderResult;
import com.scenicticket.dto.TicketAvailabilityDTO;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class PurchaseDialogPanel extends JPanel {
    private static final int MAX_QUANTITY = 99;

    private final List<TicketAvailabilityDTO> options;
    private final JComboBox<String> optionBox = new JComboBox<>();
    private final JSpinner quantitySpinner = new JSpinner();
    private final JComboBox<String> paymentBox = new JComboBox<>(new String[]{"微信", "支付宝", "银行卡"});
    private final JLabel amountLabel = new JLabel();

    public PurchaseDialogPanel(String itemTitle, List<TicketAvailabilityDTO> options) {
        super(new BorderLayout());
        if (itemTitle == null || itemTitle.isBlank()) {
            throw new IllegalArgumentException("景点名称不能为空");
        }
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("可售票种不能为空");
        }
        this.options = List.copyOf(options);
        buildContent(itemTitle);
    }

    private void buildContent(String itemTitle) {
        for (TicketAvailabilityDTO option : options) {
            validateOption(option);
            optionBox.addItem(option.ticketType().getName() + " | " + option.inventory().getVisitDate()
                    + " | 折后 " + UiFormatters.money(option.discountedPrice())
                    + " | 可售 " + option.inventory().getAvailableStock());
        }
        quantitySpinner.setModel(new SpinnerNumberModel(1, 1, maximumFor(selectedOption()), 1));
        amountLabel.setFont(UiTheme.SECTION_FONT);
        amountLabel.setToolTipText("仅供下单前确认，最终金额由服务端按最新票价重新计算");

        optionBox.addActionListener(event -> updateSelection());
        quantitySpinner.addChangeListener(event -> updateEstimatedAmount());

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "景点", new JLabel(itemTitle));
        addField(fields, 1, "票种与日期", optionBox);
        addField(fields, 2, "购买票数", quantitySpinner);
        addField(fields, 3, "付款方式", paymentBox);
        addField(fields, 4, "下单前估算金额", amountLabel);
        addField(fields, 5, "支付提示", new JLabel("创建后请到“我的订单”主动确认支付，15 分钟过期"));
        add(UiComponents.card("创建待支付订单", fields), BorderLayout.CENTER);
        updateEstimatedAmount();
    }

    private void updateSelection() {
        SpinnerNumberModel model = (SpinnerNumberModel) quantitySpinner.getModel();
        int maximum = maximumFor(selectedOption());
        model.setMaximum(maximum);
        if ((Integer) model.getValue() > maximum) {
            model.setValue(maximum);
        }
        updateEstimatedAmount();
    }

    private void updateEstimatedAmount() {
        BigDecimal total = selectedOption().discountedPrice()
                .multiply(BigDecimal.valueOf((Integer) quantitySpinner.getValue()));
        amountLabel.setText(UiFormatters.money(total));
    }

    private TicketAvailabilityDTO selectedOption() {
        int index = optionBox.getSelectedIndex();
        if (index < 0 || index >= options.size()) {
            throw new IllegalStateException("请选择票种与游玩日期");
        }
        return options.get(index);
    }

    private int maximumFor(TicketAvailabilityDTO option) {
        return Math.min(MAX_QUANTITY, option.inventory().getAvailableStock());
    }

    private void validateOption(TicketAvailabilityDTO option) {
        Objects.requireNonNull(option, "option");
        Objects.requireNonNull(option.ticketType(), "ticketType");
        Objects.requireNonNull(option.inventory(), "inventory");
        Objects.requireNonNull(option.ticketType().getTicketTypeId(), "ticketTypeId");
        Objects.requireNonNull(option.ticketType().getName(), "ticketTypeName");
        Objects.requireNonNull(option.inventory().getVisitDate(), "visitDate");
        Objects.requireNonNull(option.discountedPrice(), "discountedPrice");
        Integer stock = option.inventory().getAvailableStock();
        if (stock == null || stock <= 0) {
            throw new IllegalArgumentException("可售库存必须大于 0");
        }
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

    public PendingOrderRequest request() {
        TicketAvailabilityDTO selected = selectedOption();
        return new PendingOrderRequest(selected.ticketType().getTicketTypeId(),
                selected.inventory().getVisitDate(), (Integer) quantitySpinner.getValue(),
                (String) paymentBox.getSelectedItem());
    }

    public static String successText(String itemTitle, PendingOrderResult result) {
        Objects.requireNonNull(result, "result");
        return result.message() + System.lineSeparator()
                + "景点：" + itemTitle + System.lineSeparator()
                + "票种：" + result.ticketTypeName() + System.lineSeparator()
                + "游玩日期：" + result.visitDate() + System.lineSeparator()
                + "购买票数：" + result.quantity() + System.lineSeparator()
                + "订单号：" + result.orderId() + System.lineSeparator()
                + "服务端实际金额：" + UiFormatters.money(result.totalAmount()) + System.lineSeparator()
                + "请进入“我的订单”确认支付。";
    }

    void selectOption(int index) {
        optionBox.setSelectedIndex(index);
    }

    void setQuantity(int quantity) {
        quantitySpinner.setValue(quantity);
    }

    void setPaymentMethod(String paymentMethod) {
        paymentBox.setSelectedItem(paymentMethod);
    }

    int maximumQuantity() {
        return (Integer) ((SpinnerNumberModel) quantitySpinner.getModel()).getMaximum();
    }

    int quantity() {
        return (Integer) quantitySpinner.getValue();
    }

    String estimatedAmountText() {
        return amountLabel.getText();
    }

    public record PendingOrderRequest(long ticketTypeId, LocalDate visitDate, int quantity,
                                      String paymentMethod) {
    }
}
