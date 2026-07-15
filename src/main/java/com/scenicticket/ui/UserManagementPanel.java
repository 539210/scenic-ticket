package com.scenicticket.ui;

import com.scenicticket.dto.AdminChangeResult;
import com.scenicticket.dto.AdminUserDetailDTO;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.model.Profile;
import com.scenicticket.model.Order;
import com.scenicticket.model.User;
import org.bson.Document;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public final class UserManagementPanel extends JPanel {
    private static final int PAGE_SIZE = 50;
    private final long actorUserId;
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final JTextField userIdField = new JTextField(7);
    private final JTextField usernameField = new JTextField(12);
    private final JTextField emailField = new JTextField(16);
    private final JComboBox<String> roleFilter = new JComboBox<>(new String[]{"全部角色", "管理员", "普通用户"});
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"全部状态", "启用", "禁用"});
    private final DefaultTableModel tableModel = tableModel();
    private final JTable table = UiComponents.table(tableModel);
    private final List<User> visibleUsers = new ArrayList<>();
    private final JTextArea detailArea = UiComponents.readOnlyTextArea(14, 38);
    private final JComboBox<String> targetRoleBox = new JComboBox<>(new String[]{"普通用户", "管理员"});
    private final JTextField banReasonField = new JTextField(16);
    private final JButton banButton = UiComponents.primaryButton("封禁账号");
    private final JButton unbanButton = UiComponents.secondaryButton("解除封禁");
    private final JButton roleButton = UiComponents.secondaryButton("更新角色");
    private final JButton refreshDetailButton = UiComponents.secondaryButton("刷新详情");
    private final JButton previousButton = UiComponents.secondaryButton("上一页");
    private final JButton nextButton = UiComponents.secondaryButton("下一页");
    private final JLabel pageLabel = new JLabel("第 1 页");
    private int currentOffset;
    private User selectedUser;

    public UserManagementPanel(long actorUserId, UiTaskExecutor taskExecutor, Actions actions) {
        super(new BorderLayout(12, 12));
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("当前管理员无效，请重新登录");
        }
        this.actorUserId = actorUserId;
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        setOpaque(false);
        detailArea.setText("请先查询并选择用户。\n服务层会再次校验管理员权限。");
        setMutationButtonsEnabled(false, null);
        configureSelection();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                UiComponents.card("用户列表", UiComponents.scroll(table)),
                UiComponents.card("用户详情与操作", createDetailPanel()));
        split.setResizeWeight(0.62);
        split.setDividerLocation(760);
        add(UiComponents.card("用户筛选", createFilters()), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        loadUsers(filterSnapshot(), 0, null);
    }

    private JPanel createFilters() {
        JPanel filters = UiComponents.toolbar();
        JPanel paging = UiComponents.toolbar();
        JPanel container = new JPanel(new GridLayout(2, 1, 0, 4));
        container.setOpaque(false);
        JButton queryButton = UiComponents.primaryButton("查询用户");
        JButton refreshButton = UiComponents.secondaryButton("刷新");
        JButton resetButton = UiComponents.secondaryButton("重置");
        filters.add(new JLabel("用户ID"));
        filters.add(userIdField);
        filters.add(new JLabel("用户名"));
        filters.add(usernameField);
        filters.add(new JLabel("邮箱"));
        filters.add(emailField);
        filters.add(new JLabel("角色"));
        filters.add(roleFilter);
        filters.add(new JLabel("状态"));
        filters.add(statusFilter);
        filters.add(queryButton);
        filters.add(refreshButton);
        filters.add(resetButton);
        paging.add(previousButton);
        paging.add(pageLabel);
        paging.add(nextButton);
        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
        queryButton.addActionListener(event -> loadUsers(filterSnapshot(), 0, null));
        refreshButton.addActionListener(event -> loadUsers(filterSnapshot(), currentOffset, null));
        resetButton.addActionListener(event -> {
            userIdField.setText("");
            usernameField.setText("");
            emailField.setText("");
            roleFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            loadUsers(filterSnapshot(), 0, null);
        });
        userIdField.addActionListener(event -> loadUsers(filterSnapshot(), 0, null));
        usernameField.addActionListener(event -> loadUsers(filterSnapshot(), 0, null));
        emailField.addActionListener(event -> loadUsers(filterSnapshot(), 0, null));
        previousButton.addActionListener(event -> loadUsers(filterSnapshot(), Math.max(0, currentOffset - PAGE_SIZE), null));
        nextButton.addActionListener(event -> loadUsers(filterSnapshot(), currentOffset + PAGE_SIZE, null));
        container.add(filters);
        container.add(paging);
        return container;
    }

    private JPanel createDetailPanel() {
        JPanel actionBar = UiComponents.toolbar();
        actionBar.add(new JLabel("封禁原因"));
        actionBar.add(banReasonField);
        actionBar.add(banButton);
        actionBar.add(unbanButton);
        actionBar.add(new JLabel("角色"));
        actionBar.add(targetRoleBox);
        actionBar.add(roleButton);
        actionBar.add(refreshDetailButton);
        banButton.addActionListener(event -> changeStatus(0));
        unbanButton.addActionListener(event -> changeStatus(1));
        roleButton.addActionListener(event -> changeRole());
        refreshDetailButton.addActionListener(event -> refreshSelectedDetail());
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(UiComponents.scroll(detailArea), BorderLayout.CENTER);
        panel.add(actionBar, BorderLayout.SOUTH);
        return panel;
    }

    private void configureSelection() {
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || table.getSelectedRow() < 0) {
                return;
            }
            int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
            if (modelRow < 0 || modelRow >= visibleUsers.size()) {
                return;
            }
            selectUser(visibleUsers.get(modelRow));
        });
    }

    private void loadUsers(FilterSnapshot snapshot, int requestedOffset, String completionMessage) {
        taskExecutor.run("查询用户", () -> actions.search(snapshot.toCriteria(requestedOffset)), users -> {
            List<User> safeUsers = users == null ? List.of() : users;
            visibleUsers.clear();
            visibleUsers.addAll(safeUsers);
            currentOffset = requestedOffset;
            tableModel.setRowCount(0);
            for (User user : safeUsers) {
                tableModel.addRow(new Object[]{user.getUserId(), user.getUsername(), user.getEmail(), user.getPhone(),
                        UiFormatters.role(user.getRole()), user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用",
                        UiFormatters.date(user.getCreatedAt())});
            }
            resetSelection();
            pageLabel.setText("第 " + (currentOffset / PAGE_SIZE + 1) + " 页");
            previousButton.setEnabled(currentOffset > 0);
            nextButton.setEnabled(safeUsers.size() == PAGE_SIZE);
            detailArea.setText(safeUsers.isEmpty()
                    ? "没有找到符合条件的用户。" : "查询到 " + safeUsers.size() + " 个用户，请选择查看详情。");
            actions.setStatus(completionMessage != null ? completionMessage : "查询到 " + safeUsers.size() + " 个用户");
        });
    }

    private void selectUser(User user) {
        selectedUser = user;
        targetRoleBox.setSelectedIndex("ADMIN".equals(user.getRole()) ? 1 : 0);
        boolean mutableTarget = user.getUserId() != null && user.getUserId() != actorUserId;
        setMutationButtonsEnabled(mutableTarget, user.getStatus());
        long targetUserId = user.getUserId();
        detailArea.setText("正在加载用户 “" + user.getUsername() + "” 的详情……");
        loadDetail(targetUserId);
    }

    private void loadDetail(long targetUserId) {
        taskExecutor.run("加载用户详情", () -> actions.detail(targetUserId), detail -> {
            if (isSelectedUser(targetUserId)) {
                detailArea.setText(formatDetail(detail));
            }
        });
    }

    private void refreshSelectedDetail() {
        if (selectedUser == null || selectedUser.getUserId() == null) {
            throw new IllegalArgumentException("请先选择用户");
        }
        loadDetail(selectedUser.getUserId());
    }

    private void changeStatus(int status) {
        long targetUserId = requireMutableTargetId();
        String reason = banReasonField.getText();
        FilterSnapshot snapshot = filterSnapshot();
        taskExecutor.run(status == 0 ? "封禁用户账号" : "解除用户封禁",
                () -> actions.changeStatus(targetUserId, status, reason), result ->
                        loadUsers(snapshot, currentOffset, result.message()));
    }

    private void changeRole() {
        long targetUserId = requireMutableTargetId();
        String role = targetRoleBox.getSelectedIndex() == 1 ? "ADMIN" : "USER";
        FilterSnapshot snapshot = filterSnapshot();
        taskExecutor.run("更新用户角色", () -> actions.changeRole(targetUserId, role), result ->
                loadUsers(snapshot, currentOffset, result.message()));
    }

    private void resetSelection() {
        table.clearSelection();
        selectedUser = null;
        banReasonField.setText("");
        setMutationButtonsEnabled(false, null);
    }

    private void setMutationButtonsEnabled(boolean enabled, Integer status) {
        banButton.setEnabled(enabled && status != null && status == 1);
        unbanButton.setEnabled(enabled && (status == null || status != 1));
        roleButton.setEnabled(enabled);
        refreshDetailButton.setEnabled(selectedUser != null);
        banReasonField.setEnabled(enabled && status != null && status == 1);
    }

    private long requireMutableTargetId() {
        if (selectedUser == null || selectedUser.getUserId() == null) {
            throw new IllegalArgumentException("请先选择用户");
        }
        if (selectedUser.getUserId() == actorUserId) {
            throw new IllegalArgumentException("不能在当前会话中修改自己的状态或角色");
        }
        return selectedUser.getUserId();
    }

    private boolean isSelectedUser(long userId) {
        return selectedUser != null && selectedUser.getUserId() != null && selectedUser.getUserId() == userId;
    }

    private FilterSnapshot filterSnapshot() {
        return new FilterSnapshot(userIdField.getText(), usernameField.getText(), emailField.getText(),
                roleFilter.getSelectedIndex(), statusFilter.getSelectedIndex());
    }

    private static String formatDetail(AdminUserDetailDTO detail) {
        if (detail == null || detail.getUser() == null) {
            return "用户详情不可用";
        }
        User user = detail.getUser();
        Profile profile = detail.getProfile();
        var orders = detail.getOrderSummary();
        StringBuilder builder = new StringBuilder("用户基本信息")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("用户ID：").append(valueText(user.getUserId())).append(System.lineSeparator())
                .append("用户名：").append(valueText(user.getUsername())).append(System.lineSeparator())
                .append("邮箱：").append(valueText(user.getEmail())).append(System.lineSeparator())
                .append("手机号：").append(valueText(user.getPhone())).append(System.lineSeparator())
                .append("角色：").append(UiFormatters.role(user.getRole())).append(System.lineSeparator())
                .append("状态：").append(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用")
                .append(System.lineSeparator()).append("注册时间：").append(UiFormatters.date(user.getCreatedAt()))
                .append(System.lineSeparator()).append("最近更新：").append(UiFormatters.date(user.getUpdatedAt()))
                .append(System.lineSeparator()).append(System.lineSeparator()).append("用户档案").append(System.lineSeparator());
        if (profile == null) {
            builder.append("暂无档案").append(System.lineSeparator());
        } else {
            builder.append("真实姓名：").append(valueText(profile.getRealName())).append(System.lineSeparator())
                    .append("证件号：").append(valueText(profile.getIdCard())).append(System.lineSeparator())
                    .append("地址：").append(valueText(profile.getAddress())).append(System.lineSeparator())
                    .append("备注：").append(valueText(profile.getNotes())).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("订单概况").append(System.lineSeparator());
        if (orders == null) {
            builder.append("暂无订单概况").append(System.lineSeparator());
        } else {
            builder.append("总订单：").append(orders.getTotalOrders())
                    .append("，待支付：").append(orders.getPendingOrders())
                    .append("，已支付：").append(orders.getPaidOrders())
                    .append("，已取消：").append(orders.getCancelledOrders())
                    .append("，已完成：").append(orders.getCompletedOrders()).append(System.lineSeparator())
                    .append("有效订单金额：").append(UiFormatters.money(orders.getPaidAmount())).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("近期订单（最多10条）").append(System.lineSeparator());
        if (detail.getRecentOrders().isEmpty()) {
            builder.append("暂无订单").append(System.lineSeparator());
        } else {
            for (Order order : detail.getRecentOrders()) {
                builder.append("#").append(order.getOrderId())
                        .append("  景点#").append(order.getItemId())
                        .append("  ").append(UiFormatters.orderStatus(order.getStatus()))
                        .append("  ").append(UiFormatters.money(order.getAmount()))
                        .append("  ").append(valueText(order.getVisitDate())).append(System.lineSeparator());
            }
        }
        builder.append(System.lineSeparator()).append("行为概况").append(System.lineSeparator())
                .append(detail.isBehaviorDataAvailable()
                        ? "行为日志数量：" + detail.getBehaviorCount()
                        : "MongoDB 行为数据暂不可用，MySQL 用户信息仍可管理")
                .append(System.lineSeparator());
        if (detail.isBehaviorDataAvailable()) {
            appendRecentActions(builder, detail.getRecentActions());
            appendRecentComments(builder, detail.getRecentComments());
            appendRecentAudit(builder, detail.getRecentAuditLogs());
        }
        return builder.toString();
    }

    private static void appendRecentActions(StringBuilder builder, List<Document> actions) {
        builder.append(System.lineSeparator()).append("近期行为（最多10条）").append(System.lineSeparator());
        if (actions.isEmpty()) {
            builder.append("暂无行为记录").append(System.lineSeparator());
            return;
        }
        for (Document action : actions) {
            builder.append(UiFormatters.date(action.get("created_at"))).append("  ")
                    .append(valueText(action.get("action_type"))).append("  景点#")
                    .append(valueText(action.get("item_id"))).append(System.lineSeparator());
        }
    }

    private static void appendRecentComments(StringBuilder builder, List<Document> comments) {
        builder.append(System.lineSeparator()).append("近期评论（最多10条）").append(System.lineSeparator());
        if (comments.isEmpty()) {
            builder.append("暂无评论").append(System.lineSeparator());
            return;
        }
        for (Document comment : comments) {
            builder.append("景点#").append(valueText(comment.get("item_id")))
                    .append("  评分").append(valueText(comment.get("rating")))
                    .append("  ").append(shortText(comment.get("content"), 60)).append(System.lineSeparator());
        }
    }

    private static void appendRecentAudit(StringBuilder builder, List<Document> logs) {
        builder.append(System.lineSeparator()).append("近期审计（最多10条）").append(System.lineSeparator());
        if (logs.isEmpty()) {
            builder.append("暂无审计记录").append(System.lineSeparator());
            return;
        }
        for (Document log : logs) {
            builder.append(UiFormatters.date(log.get("timestamp"))).append("  ")
                    .append(valueText(log.get("log_type"))).append("  ")
                    .append(shortText(log.get("message"), 80)).append(System.lineSeparator());
        }
    }

    private static String shortText(Object value, int maxLength) {
        String text = UiFormatters.readableText(value, "-");
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }

    private static String valueText(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value);
    }

    private static DefaultTableModel tableModel() {
        return new DefaultTableModel(
                new Object[]{"用户ID", "用户名", "邮箱", "手机号", "角色", "状态", "创建时间"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    void setFilters(String username, String email, int roleIndex, int statusIndex) {
        setFilters("", username, email, roleIndex, statusIndex);
    }

    void setFilters(String userId, String username, String email, int roleIndex, int statusIndex) {
        userIdField.setText(userId);
        usernameField.setText(username);
        emailField.setText(email);
        roleFilter.setSelectedIndex(roleIndex);
        statusFilter.setSelectedIndex(statusIndex);
    }

    void selectRow(int row) {
        table.setRowSelectionInterval(row, row);
    }

    void setTargetRoleIndex(int index) {
        targetRoleBox.setSelectedIndex(index);
    }

    int rowCount() {
        return tableModel.getRowCount();
    }

    boolean statusUpdateEnabled() {
        return banButton.isEnabled() || unbanButton.isEnabled();
    }

    boolean banEnabled() { return banButton.isEnabled(); }

    boolean unbanEnabled() { return unbanButton.isEnabled(); }

    void setBanReason(String reason) { banReasonField.setText(reason); }

    boolean roleUpdateEnabled() {
        return roleButton.isEnabled();
    }

    String detailText() {
        return detailArea.getText();
    }

    public interface Actions {
        List<User> search(UserSearchCriteria criteria);

        AdminUserDetailDTO detail(long targetUserId);

        AdminChangeResult changeStatus(long targetUserId, int status, String reason);

        AdminChangeResult changeRole(long targetUserId, String role);

        void setStatus(String message);
    }

    private record FilterSnapshot(String userId, String username, String email, int roleIndex, int statusIndex) {
        private UserSearchCriteria toCriteria(int offset) {
            String role = switch (roleIndex) {
                case 1 -> "ADMIN";
                case 2 -> "USER";
                default -> null;
            };
            Integer status = switch (statusIndex) {
                case 1 -> 1;
                case 2 -> 0;
                default -> null;
            };
            return new UserSearchCriteria(UiInputParsers.optionalLong(userId),
                    UiInputParsers.blankToNull(username), UiInputParsers.blankToNull(email),
                    role, status, PAGE_SIZE, offset);
        }
    }
}
