package com.scenicticket.ui;

import com.scenicticket.dto.LoginResult;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.model.Item;
import com.scenicticket.model.User;
import com.scenicticket.service.BehaviorLogService;
import com.scenicticket.service.BusinessService;
import com.scenicticket.service.CrossDatabaseQueryService;
import com.scenicticket.service.RecommendService;
import com.scenicticket.service.StatisticsService;
import com.scenicticket.service.SystemLogService;
import com.scenicticket.service.UserService;
import org.bson.Document;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class AppFrame extends JFrame {
    private final UserService userService = new UserService();
    private final BusinessService businessService = new BusinessService();
    private final CrossDatabaseQueryService crossDatabaseQueryService = new CrossDatabaseQueryService();
    private final RecommendService recommendService = new RecommendService();
    private final StatisticsService statisticsService = new StatisticsService();
    private final SystemLogService systemLogService = new SystemLogService();
    private final BehaviorLogService behaviorLogService = new BehaviorLogService();

    private final JLabel statusLabel = new JLabel("未登录");
    private Long currentUserId;
    private String currentRole;

    public AppFrame() {
        setTitle("景点售票系统");
        setSize(1080, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("登录注册", createAuthPanel());
        tabs.addTab("景点查询", createItemPanel());
        tabs.addTab("推荐", createRecommendPanel());
        tabs.addTab("统计报表", createReportPanel());
        tabs.addTab("系统审计", createAuditPanel());

        add(tabs, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JTextField usernameField = new JTextField(24);
        JPasswordField passwordField = new JPasswordField(24);
        JTextField emailField = new JTextField(24);
        JTextField phoneField = new JTextField(24);
        JTextField ipField = new JTextField("127.0.0.1", 24);
        JLabel resultLabel = new JLabel("请输入账号信息");

        addField(panel, 0, "用户名", usernameField);
        addField(panel, 1, "密码", passwordField);
        addField(panel, 2, "邮箱", emailField);
        addField(panel, 3, "手机", phoneField);
        addField(panel, 4, "IP", ipField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loginButton = new JButton("登录");
        JButton registerButton = new JButton("注册");
        buttons.add(loginButton);
        buttons.add(registerButton);

        GridBagConstraints gbc = baseConstraints(5);
        gbc.gridx = 1;
        panel.add(buttons, gbc);
        gbc = baseConstraints(6);
        gbc.gridx = 1;
        panel.add(resultLabel, gbc);

        loginButton.addActionListener(event -> runTask("用户登录", () -> userService.login(
                usernameField.getText(),
                new String(passwordField.getPassword()),
                ipField.getText()
        ), result -> {
            resultLabel.setText(result.getMessage());
            if (result.isSuccess()) {
                User user = result.getUser();
                currentUserId = user.getUserId();
                currentRole = user.getRole();
                setStatus("已登录：" + user.getUsername() + " / " + currentRole);
            }
        }));

        registerButton.addActionListener(event -> runTask("用户注册", () -> userService.register(
                usernameField.getText(),
                new String(passwordField.getPassword()),
                emailField.getText(),
                phoneField.getText()
        ), userId -> {
            currentUserId = userId;
            currentRole = "USER";
            resultLabel.setText("注册成功，用户ID：" + userId);
            setStatus("已注册：用户ID " + userId);
        }));
        return panel;
    }

    private JPanel createItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField keywordField = new JTextField(16);
        JTextField categoryField = new JTextField(6);
        JTextField itemIdField = new JTextField(6);
        JTextField orderAmountField = new JTextField("0.00", 8);
        JTextField ratingField = new JTextField("5", 4);
        JTextField commentField = new JTextField(24);

        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"ID", "标题", "分类", "状态", "更新时间"}, 0);
        JTable table = new JTable(tableModel);
        JTextArea detailArea = createTextArea();

        JPanel queryBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("查询");
        JButton detailButton = new JButton("详情");
        JButton orderButton = new JButton("下单");
        JButton commentButton = new JButton("评论");
        queryBar.add(new JLabel("关键词"));
        queryBar.add(keywordField);
        queryBar.add(new JLabel("分类ID"));
        queryBar.add(categoryField);
        queryBar.add(searchButton);
        queryBar.add(new JLabel("景点ID"));
        queryBar.add(itemIdField);
        queryBar.add(detailButton);
        queryBar.add(new JLabel("金额"));
        queryBar.add(orderAmountField);
        queryBar.add(orderButton);
        queryBar.add(new JLabel("评分"));
        queryBar.add(ratingField);
        queryBar.add(commentField);
        queryBar.add(commentButton);

        panel.add(queryBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(new JScrollPane(detailArea), BorderLayout.SOUTH);

        searchButton.addActionListener(event -> runTask("景点查询", () -> businessService.searchItems(
                keywordField.getText(), parseOptionalLong(categoryField.getText()), 50, 0
        ), items -> {
            tableModel.setRowCount(0);
            for (Item item : items) {
                tableModel.addRow(new Object[]{
                        item.getItemId(), item.getTitle(), item.getCategoryId(), item.getStatus(), item.getUpdatedAt()
                });
            }
        }));

        detailButton.addActionListener(event -> runTask("景点详情", () -> crossDatabaseQueryService.getItemDetail(
                parseRequiredLong(itemIdField.getText(), "景点ID"), 5
        ), dto -> detailArea.setText(String.valueOf(dto.getItem().getTitle()) + System.lineSeparator()
                + documentToText(dto.getDetail()) + System.lineSeparator()
                + "评分：" + documentToText(dto.getRatingSummary()) + System.lineSeparator()
                + "评论数：" + dto.getComments().size())));

        orderButton.addActionListener(event -> runTask("创建订单", () -> businessService.createOrder(
                requireCurrentUserId(),
                parseRequiredLong(itemIdField.getText(), "景点ID"),
                new BigDecimal(orderAmountField.getText().trim())
        ), orderId -> detailArea.setText("订单创建成功，订单ID：" + orderId)));

        commentButton.addActionListener(event -> runTask("发表评论", () -> {
            behaviorLogService.addComment(
                    requireCurrentUserId(),
                    parseRequiredLong(itemIdField.getText(), "景点ID"),
                    commentField.getText(),
                    Integer.parseInt(ratingField.getText().trim()),
                    List.of("界面提交"),
                    "127.0.0.1"
            );
            return "评论提交成功";
        }, detailArea::setText));
        return panel;
    }

    private JPanel createRecommendPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField userIdField = new JTextField(8);
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"ID", "标题", "分数", "原因"}, 0);
        JTable table = new JTable(tableModel);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton personalButton = new JButton("个性化推荐");
        JButton hotButton = new JButton("热门推荐");
        JButton ratedButton = new JButton("高评分推荐");
        bar.add(new JLabel("用户ID"));
        bar.add(userIdField);
        bar.add(personalButton);
        bar.add(hotButton);
        bar.add(ratedButton);

        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        personalButton.addActionListener(event -> runTask("个性化推荐", () -> recommendService.recommendForUser(
                userIdField.getText().isBlank() ? requireCurrentUserId() : parseRequiredLong(userIdField.getText(), "用户ID"), 10
        ), recommendations -> fillRecommendationTable(tableModel, recommendations)));

        hotButton.addActionListener(event -> runTask("热门推荐", () -> recommendService.recommendHotItems(null, null, 10),
                recommendations -> fillRecommendationTable(tableModel, recommendations)));

        ratedButton.addActionListener(event -> runTask("高评分推荐", () -> recommendService.recommendTopRatedItems(10),
                recommendations -> fillRecommendationTable(tableModel, recommendations)));
        return panel;
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField yearField = new JTextField(String.valueOf(LocalDate.now().getYear()), 6);
        JTextField monthField = new JTextField(String.valueOf(LocalDate.now().getMonthValue()), 4);
        JTextField userIdField = new JTextField(8);
        JTextArea reportArea = createTextArea();

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton monthlyButton = new JButton("月度订单报表");
        JButton hotButton = new JButton("热门排行");
        JButton userButton = new JButton("用户报告");
        bar.add(new JLabel("年份"));
        bar.add(yearField);
        bar.add(new JLabel("月份"));
        bar.add(monthField);
        bar.add(monthlyButton);
        bar.add(hotButton);
        bar.add(new JLabel("用户ID"));
        bar.add(userIdField);
        bar.add(userButton);

        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        monthlyButton.addActionListener(event -> runTask("月度订单报表", () -> statisticsService.getMonthlyOrderReport(
                Integer.parseInt(yearField.getText().trim()),
                Integer.parseInt(monthField.getText().trim())
        ), reports -> reportArea.setText(formatMonthlyReports(reports))));

        hotButton.addActionListener(event -> runTask("热门排行", () -> statisticsService.getHotItemRanking(null, null, 10),
                documents -> reportArea.setText(formatDocuments(documents))));

        userButton.addActionListener(event -> runTask("用户报告", () -> statisticsService.getUserReport(
                userIdField.getText().isBlank() ? requireCurrentUserId() : parseRequiredLong(userIdField.getText(), "用户ID"),
                null,
                null
        ), document -> reportArea.setText(documentToText(document))));
        return panel;
    }

    private JPanel createAuditPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField userIdField = new JTextField(8);
        JTextField logTypeField = new JTextField(10);
        JTextField logLevelField = new JTextField(8);
        JTextArea auditArea = createTextArea();

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton queryButton = new JButton("查询日志");
        JButton summaryButton = new JButton("审计汇总");
        JButton trendButton = new JButton("审计趋势");
        bar.add(new JLabel("用户ID"));
        bar.add(userIdField);
        bar.add(new JLabel("类型"));
        bar.add(logTypeField);
        bar.add(new JLabel("级别"));
        bar.add(logLevelField);
        bar.add(queryButton);
        bar.add(summaryButton);
        bar.add(trendButton);

        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(auditArea), BorderLayout.CENTER);

        queryButton.addActionListener(event -> runTask("审计日志查询", () -> systemLogService.queryAuditLogs(
                parseOptionalLong(userIdField.getText()),
                blankToNull(logTypeField.getText()),
                blankToNull(logLevelField.getText()),
                null,
                null,
                50
        ), documents -> auditArea.setText(formatDocuments(documents))));

        summaryButton.addActionListener(event -> runTask("审计汇总", () -> systemLogService.getAuditSummary(null, null),
                documents -> auditArea.setText(formatDocuments(documents))));

        trendButton.addActionListener(event -> runTask("审计趋势", () -> systemLogService.getDailyAuditTrend(null, null),
                documents -> auditArea.setText(formatDocuments(documents))));
        return panel;
    }

    private void fillRecommendationTable(DefaultTableModel tableModel, List<RecommendationDTO> recommendations) {
        tableModel.setRowCount(0);
        for (RecommendationDTO recommendation : recommendations) {
            Item item = recommendation.getItem();
            tableModel.addRow(new Object[]{
                    item == null ? "" : item.getItemId(),
                    item == null ? "" : item.getTitle(),
                    recommendation.getScore(),
                    recommendation.getReason()
            });
        }
    }

    private String formatMonthlyReports(List<MonthlyOrderReportDTO> reports) {
        StringBuilder builder = new StringBuilder();
        for (MonthlyOrderReportDTO report : reports) {
            builder.append(report.getOrderDate())
                    .append(" 订单数：").append(report.getOrderCount())
                    .append(" 金额：").append(report.getTotalAmount())
                    .append(System.lineSeparator());
        }
        return builder.isEmpty() ? "暂无报表数据" : builder.toString();
    }

    private String formatDocuments(List<Document> documents) {
        StringBuilder builder = new StringBuilder();
        for (Document document : documents) {
            builder.append(documentToText(document)).append(System.lineSeparator());
        }
        return builder.isEmpty() ? "暂无数据" : builder.toString();
    }

    private String documentToText(Document document) {
        return document == null ? "{}" : document.toJson();
    }

    private JTextArea createTextArea() {
        JTextArea textArea = new JTextArea(8, 80);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        return textArea;
    }

    private void addField(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints labelConstraints = baseConstraints(row);
        labelConstraints.gridx = 0;
        panel.add(new JLabel(label), labelConstraints);
        GridBagConstraints fieldConstraints = baseConstraints(row);
        fieldConstraints.gridx = 1;
        panel.add(field, fieldConstraints);
    }

    private GridBagConstraints baseConstraints(int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private <T> void runTask(String name, Callable<T> task, Consumer<T> onSuccess) {
        setStatus(name + "处理中...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    onSuccess.accept(result);
                    setStatus(name + "完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setStatus(name + "已中断");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    showError(cause);
                    setStatus(name + "失败：" + cause.getMessage());
                }
            }
        }.execute();
    }

    private void showError(Throwable throwable) {
        JOptionPane.showMessageDialog(this, throwable.getMessage(), "操作失败", JOptionPane.ERROR_MESSAGE);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private long requireCurrentUserId() {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalStateException("请先登录或填写用户ID");
        }
        return currentUserId;
    }

    private Long parseOptionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }

    private long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return Long.parseLong(value.trim());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
