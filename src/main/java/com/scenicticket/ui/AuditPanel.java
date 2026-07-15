package com.scenicticket.ui;

import com.scenicticket.dto.AuditLogQuery;
import org.bson.Document;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public final class AuditPanel extends JPanel {
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final JTextField userIdField = new JTextField(10);
    private final DatePickerField startDateField = new DatePickerField(null);
    private final DatePickerField endDateField = new DatePickerField(null);
    private final JTextField keywordField = new JTextField(12);
    private final JTextField limitField = new JTextField("80", 5);
    private final JComboBox<String> logTypeBox = new JComboBox<>(
            new String[]{"全部类型", "登录", "退出", "注册", "预定下单", "支付购票", "取消预定", "预定过期",
                    "退款", "发表评论", "更新评论", "门票核销", "景点更新", "查看报表", "用户状态变更", "用户角色变更"});
    private final JComboBox<String> levelBox = new JComboBox<>(
            new String[]{"全部级别", "正常", "警告", "错误"});
    private final DefaultTableModel logModel = tableModel("时间", "用户ID", "类型", "级别", "内容", "业务对象", "业务详情", "IP地址");
    private final DefaultTableModel summaryModel = tableModel("类型", "级别", "操作次数", "涉及用户", "最近时间");
    private final DefaultTableModel trendModel = tableModel("日期", "类型", "级别", "次数");
    private final DefaultTableModel userSummaryModel = tableModel("用户ID", "操作次数", "警告", "错误", "最近时间", "操作类型");
    private final JScrollPane logScroll = UiComponents.scroll(UiComponents.table(logModel));
    private final JScrollPane summaryScroll = UiComponents.scroll(UiComponents.table(summaryModel));
    private final JScrollPane trendScroll = UiComponents.scroll(UiComponents.table(trendModel));
    private final JScrollPane userSummaryScroll = UiComponents.scroll(UiComponents.table(userSummaryModel));
    private final JTabbedPane auditTabs = new JTabbedPane(JTabbedPane.TOP);
    private final JButton queryButton = UiComponents.secondaryButton("查询日志");
    private final JButton summaryButton = UiComponents.secondaryButton("审计汇总");
    private final JButton trendButton = UiComponents.secondaryButton("审计趋势");
    private final JButton userSummaryButton = UiComponents.secondaryButton("用户操作");
    private final List<JButton> viewButtons = List.of(queryButton, summaryButton, trendButton, userSummaryButton);

    public AuditPanel(UiTaskExecutor taskExecutor, Actions actions) {
        super(new BorderLayout(12, 12));
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        auditTabs.addTab("日志明细", logScroll);
        auditTabs.addTab("审计汇总", summaryScroll);
        auditTabs.addTab("审计趋势", trendScroll);
        auditTabs.addTab("用户操作", userSummaryScroll);
        auditTabs.addChangeListener(event -> updateSelectedButton(auditTabs.getSelectedIndex()));
        add(UiComponents.card("审计条件", createToolbar()), BorderLayout.NORTH);
        add(UiComponents.card("审计结果", auditTabs), BorderLayout.CENTER);
        updateSelectedButton(0);
    }

    private JPanel createToolbar() {
        JPanel filters = UiComponents.toolbar();
        JPanel moreFilters = UiComponents.toolbar();
        JPanel buttons = UiComponents.toolbar();
        JPanel toolbar = new JPanel(new GridLayout(3, 1, 0, 4));
        toolbar.setOpaque(false);
        JButton refreshButton = UiComponents.secondaryButton("刷新当前结果");
        JButton clearButton = UiComponents.secondaryButton("清空条件");

        filters.add(new JLabel("用户ID"));
        filters.add(userIdField);
        filters.add(new JLabel("类型"));
        filters.add(logTypeBox);
        filters.add(new JLabel("级别"));
        filters.add(levelBox);
        moreFilters.add(new JLabel("开始日期"));
        moreFilters.add(startDateField);
        moreFilters.add(new JLabel("结束日期"));
        moreFilters.add(endDateField);
        moreFilters.add(new JLabel("关键词"));
        moreFilters.add(keywordField);
        moreFilters.add(new JLabel("条数"));
        moreFilters.add(limitField);
        buttons.add(queryButton);
        buttons.add(summaryButton);
        buttons.add(trendButton);
        buttons.add(userSummaryButton);
        buttons.add(refreshButton);
        buttons.add(clearButton);
        toolbar.add(filters);
        toolbar.add(moreFilters);
        toolbar.add(buttons);

        queryButton.addActionListener(event -> refreshAuditLogs());
        summaryButton.addActionListener(event -> loadSummary());
        trendButton.addActionListener(event -> loadTrend());
        userSummaryButton.addActionListener(event -> loadUserSummary());
        refreshButton.addActionListener(event -> refreshCurrentTab());
        clearButton.addActionListener(event -> clearFilters());
        userIdField.addActionListener(event -> refreshAuditLogs());
        keywordField.addActionListener(event -> refreshAuditLogs());
        return toolbar;
    }

    private void refreshAuditLogs() {
        showView(0);
        FilterSnapshot snapshot = filterSnapshot();
        taskExecutor.run("审计日志查询", () -> actions.query(snapshot.toQuery()), documents -> {
            List<Document> safeDocuments = safeDocuments(documents);
            int scrollPosition = logScroll.getVerticalScrollBar().getValue();
            logModel.setRowCount(0);
            for (Document document : safeDocuments) {
                Document detail = document.get("action_detail", Document.class);
                logModel.addRow(new Object[]{UiFormatters.date(document.get("timestamp")), valueText(document.get("user_id")),
                        logTypeName(document.getString("log_type")), logLevelName(document.getString("log_level")),
                        valueText(document.get("message")), businessObject(detail), businessDetail(detail),
                        detail == null ? "-" : valueText(detail.get("ip"))});
            }
            restoreScroll(logScroll, scrollPosition);
            actions.setStatus("查询到 " + safeDocuments.size() + " 条审计日志");
        });
    }

    private void loadSummary() {
        showView(1);
        DateRangeSnapshot snapshot = dateRangeSnapshot();
        taskExecutor.run("审计汇总", () -> actions.summary(snapshot.startTime(), snapshot.endTime()), documents -> {
            int scrollPosition = summaryScroll.getVerticalScrollBar().getValue();
            summaryModel.setRowCount(0);
            for (Document document : safeDocuments(documents)) {
                summaryModel.addRow(new Object[]{logTypeName(document.getString("log_type")),
                        logLevelName(document.getString("log_level")), numberText(document.get("operation_count")),
                        numberText(document.get("user_count")), UiFormatters.date(document.get("latest_timestamp"))});
            }
            restoreScroll(summaryScroll, scrollPosition);
        });
    }

    private void loadTrend() {
        showView(2);
        DateRangeSnapshot snapshot = dateRangeSnapshot();
        taskExecutor.run("审计趋势", () -> actions.trend(snapshot.startTime(), snapshot.endTime()), documents -> {
            int scrollPosition = trendScroll.getVerticalScrollBar().getValue();
            trendModel.setRowCount(0);
            for (Document document : safeDocuments(documents)) {
                trendModel.addRow(new Object[]{valueText(document.get("date")),
                        logTypeName(document.getString("log_type")), logLevelName(document.getString("log_level")),
                        numberText(document.get("operation_count"))});
            }
            restoreScroll(trendScroll, scrollPosition);
        });
    }

    private void loadUserSummary() {
        showView(3);
        LocalDate startDate = startDateField.getDate();
        LocalDate endDate = endDateField.getDate();
        String limitText = limitField.getText();
        taskExecutor.run("用户操作汇总", () -> actions.userSummary(
                startOfDay(startDate), endOfDay(endDate),
                UiInputParsers.optionalInt(limitText, 50, "条数")), documents -> {
            int scrollPosition = userSummaryScroll.getVerticalScrollBar().getValue();
            userSummaryModel.setRowCount(0);
            for (Document document : safeDocuments(documents)) {
                userSummaryModel.addRow(new Object[]{valueText(document.get("user_id")),
                        numberText(document.get("operation_count")), numberText(document.get("warn_count")),
                        numberText(document.get("error_count")), UiFormatters.date(document.get("latest_timestamp")),
                        logTypeListText(document.get("log_types"))});
            }
            restoreScroll(userSummaryScroll, scrollPosition);
        });
    }

    private void refreshCurrentTab() {
        switch (auditTabs.getSelectedIndex()) {
            case 0 -> refreshAuditLogs();
            case 1 -> loadSummary();
            case 2 -> loadTrend();
            case 3 -> loadUserSummary();
            default -> refreshAuditLogs();
        }
    }

    private void clearFilters() {
        userIdField.setText("");
        startDateField.clearDate();
        endDateField.clearDate();
        keywordField.setText("");
        limitField.setText("80");
        logTypeBox.setSelectedIndex(0);
        levelBox.setSelectedIndex(0);
        refreshAuditLogs();
    }

    private FilterSnapshot filterSnapshot() {
        return new FilterSnapshot(userIdField.getText(), logTypeBox.getSelectedIndex(), levelBox.getSelectedIndex(),
                startDateField.getDate(), endDateField.getDate(), keywordField.getText(), limitField.getText());
    }

    private DateRangeSnapshot dateRangeSnapshot() {
        return new DateRangeSnapshot(startDateField.getDate(), endDateField.getDate());
    }

    private void showView(int index) {
        auditTabs.setSelectedIndex(index);
        updateSelectedButton(index);
    }

    private void updateSelectedButton(int selectedIndex) {
        for (int index = 0; index < viewButtons.size(); index++) {
            UiComponents.setSelectedStyle(viewButtons.get(index), index == selectedIndex);
        }
    }

    private static Date startOfDay(LocalDate date) {
        return date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date endOfDay(LocalDate date) {
        return date == null ? null : Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .minusNanos(1).toInstant());
    }

    private static String selectedLogType(int index) {
        return switch (index) {
            case 1 -> "LOGIN";
            case 2 -> "LOGOUT";
            case 3 -> "REGISTER";
            case 4 -> "ORDER_CREATE";
            case 5 -> "ORDER_PAY";
            case 6 -> "ORDER_CANCEL";
            case 7 -> "ORDER_EXPIRE";
            case 8 -> "ORDER_REFUND";
            case 9 -> "COMMENT_CREATE";
            case 10 -> "COMMENT_UPDATE";
            case 11 -> "ADMISSION";
            case 12 -> "ITEM_UPDATE";
            case 13 -> "REPORT_VIEW";
            case 14 -> "USER_STATUS_UPDATE";
            case 15 -> "USER_ROLE_UPDATE";
            default -> null;
        };
    }

    private static String selectedLogLevel(int index) {
        return switch (index) {
            case 1 -> "INFO";
            case 2 -> "WARN";
            case 3 -> "ERROR";
            default -> null;
        };
    }

    private static String logTypeName(String logType) {
        if (logType == null || logType.isBlank()) {
            return "-";
        }
        return switch (logType) {
            case "LOGIN" -> "登录";
            case "LOGOUT" -> "退出";
            case "REGISTER" -> "注册";
            case "ORDER_CREATE", "ORDER" -> "预定下单";
            case "ORDER_PAY" -> "支付购票";
            case "ORDER_CANCEL" -> "取消预定";
            case "ORDER_EXPIRE" -> "预定过期";
            case "ORDER_REFUND" -> "退款";
            case "COMMENT", "COMMENT_CREATE" -> "发表评论";
            case "COMMENT_UPDATE" -> "更新评论";
            case "ADMISSION" -> "门票核销";
            case "ITEM_UPDATE" -> "景点更新";
            case "REPORT_VIEW" -> "查看报表";
            case "USER_STATUS_UPDATE" -> "用户状态变更";
            case "USER_ROLE_UPDATE" -> "用户角色变更";
            default -> logType;
        };
    }

    private static String logLevelName(String level) {
        if (level == null || level.isBlank()) {
            return "-";
        }
        return switch (level) {
            case "INFO" -> "正常";
            case "WARN" -> "警告";
            case "ERROR" -> "错误";
            default -> level;
        };
    }

    private static String businessObject(Document detail) {
        if (detail == null) return "-";
        Object orderId = detail.get("order_id");
        Object itemId = detail.get("item_id");
        Object targetUserId = detail.get("target_user_id");
        if (orderId != null && itemId != null) return "订单 #" + orderId + " / 景点 #" + itemId;
        if (orderId != null) return "订单 #" + orderId;
        if (itemId != null) return "景点 #" + itemId;
        if (targetUserId != null) return "用户 #" + targetUserId;
        return "-";
    }

    private static String businessDetail(Document detail) {
        if (detail == null) return "-";
        List<String> parts = new java.util.ArrayList<>();
        addDetail(parts, "操作", detail.get("operation"));
        addDetail(parts, "票种", detail.get("ticket_type_name"));
        addDetail(parts, "游玩日期", detail.get("visit_date"));
        addDetail(parts, "数量", detail.get("quantity"));
        addDetail(parts, "金额", detail.get("amount"));
        addDetail(parts, "付款方式", detail.get("payment_method"));
        addDetail(parts, "评分", detail.get("rating"));
        Object tags = detail.get("tags");
        if (tags instanceof List<?> values && !values.isEmpty()) {
            parts.add("标签=" + values.stream().map(String::valueOf).reduce((left, right) -> left + "、" + right).orElse("-"));
        }
        addDetail(parts, "原因", detail.get("reason"));
        addDetail(parts, "目标账号", detail.get("target_username"));
        addDetail(parts, "原状态", userStatusText(detail.get("previous_status")));
        addDetail(parts, "新状态", userStatusText(detail.get("new_status")));
        addDetail(parts, "原角色", detail.get("previous_role"));
        addDetail(parts, "新角色", detail.get("new_role"));
        addDetail(parts, "核销数量", detail.get("admitted_quantity"));
        return parts.isEmpty() ? "-" : String.join("；", parts);
    }

    private static void addDetail(List<String> parts, String label, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) parts.add(label + "=" + value);
    }

    private static Object userStatusText(Object value) {
        if (!(value instanceof Number number)) return value;
        return number.intValue() == 1 ? "启用" : "禁用";
    }

    private static String logTypeListText(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(item -> logTypeName(String.valueOf(item))).distinct()
                    .reduce((left, right) -> left + "、" + right).orElse("-");
        }
        return valueText(value);
    }

    private static String valueText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String numberText(Object value) {
        return value instanceof Number number ? String.valueOf(number.longValue()) : valueText(value);
    }

    private static List<Document> safeDocuments(List<Document> documents) {
        return documents == null ? List.of() : documents;
    }

    private static void restoreScroll(JScrollPane scrollPane, int position) {
        javax.swing.SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(position));
    }

    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    void setFilters(String userId, int logTypeIndex, int levelIndex, String startDate,
                    String endDate, String keyword, String limit) {
        userIdField.setText(userId);
        logTypeBox.setSelectedIndex(logTypeIndex);
        levelBox.setSelectedIndex(levelIndex);
        startDateField.setDate(parseOptionalDate(startDate, "开始日期"));
        endDateField.setDate(parseOptionalDate(endDate, "结束日期"));
        keywordField.setText(keyword);
        limitField.setText(limit);
    }

    int selectedTab() {
        return auditTabs.getSelectedIndex();
    }

    int rowCount(int tabIndex) {
        return switch (tabIndex) {
            case 0 -> logModel.getRowCount();
            case 1 -> summaryModel.getRowCount();
            case 2 -> trendModel.getRowCount();
            case 3 -> userSummaryModel.getRowCount();
            default -> throw new IllegalArgumentException("未知审计页签");
        };
    }

    Object tableValueAt(int tabIndex, int row, int column) {
        return switch (tabIndex) {
            case 0 -> logModel.getValueAt(row, column);
            case 1 -> summaryModel.getValueAt(row, column);
            case 2 -> trendModel.getValueAt(row, column);
            case 3 -> userSummaryModel.getValueAt(row, column);
            default -> throw new IllegalArgumentException("未知审计页签");
        };
    }

    boolean actionSelected(int index) {
        return UiComponents.isSelectedStyle(viewButtons.get(index));
    }

    private static LocalDate parseOptionalDate(String value, String fieldName) {
        return value == null || value.isBlank() ? null : UiInputParsers.requiredDate(value, fieldName);
    }

    public interface Actions {
        List<Document> query(AuditLogQuery query);

        List<Document> summary(Date startTime, Date endTime);

        List<Document> trend(Date startTime, Date endTime);

        List<Document> userSummary(Date startTime, Date endTime, int limit);

        void setStatus(String message);
    }

    private record FilterSnapshot(String userId, int logTypeIndex, int levelIndex,
                                  LocalDate startDate, LocalDate endDate, String keyword, String limit) {
        private AuditLogQuery toQuery() {
            AuditLogQuery query = new AuditLogQuery();
            query.setUserId(UiInputParsers.optionalLong(userId));
            query.setLogType(selectedLogType(logTypeIndex));
            query.setLogLevel(selectedLogLevel(levelIndex));
            query.setStartTime(startOfDay(startDate));
            query.setEndTime(endOfDay(endDate));
            query.setKeyword(keyword);
            query.setLimit(UiInputParsers.optionalInt(limit, 80, "条数"));
            return query;
        }
    }

    private record DateRangeSnapshot(LocalDate startDate, LocalDate endDate) {
        private Date startTime() {
            return startOfDay(startDate);
        }

        private Date endTime() {
            return endOfDay(endDate);
        }
    }
}
