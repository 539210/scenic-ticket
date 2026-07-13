package com.scenicticket.ui;

import com.scenicticket.dto.AdminChangeResult;
import com.scenicticket.dto.AdminUserDetailDTO;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.model.Profile;
import com.scenicticket.model.User;

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
import java.util.ArrayList;
import java.util.List;

public final class UserManagementPanel extends JPanel {
    private final long actorUserId;
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final JTextField usernameField = new JTextField(12);
    private final JTextField emailField = new JTextField(16);
    private final JComboBox<String> roleFilter = new JComboBox<>(new String[]{"全部角色", "管理员", "普通用户"});
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"全部状态", "启用", "禁用"});
    private final DefaultTableModel tableModel = tableModel();
    private final JTable table = UiComponents.table(tableModel);
    private final List<User> visibleUsers = new ArrayList<>();
    private final JTextArea detailArea = UiComponents.readOnlyTextArea(14, 38);
    private final JComboBox<String> targetStatusBox = new JComboBox<>(new String[]{"禁用", "启用"});
    private final JComboBox<String> targetRoleBox = new JComboBox<>(new String[]{"普通用户", "管理员"});
    private final JButton statusButton = UiComponents.primaryButton("更新状态");
    private final JButton roleButton = UiComponents.secondaryButton("更新角色");
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
        setMutationButtonsEnabled(false);
        configureSelection();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                UiComponents.card("用户列表", UiComponents.scroll(table)),
                UiComponents.card("用户详情与操作", createDetailPanel()));
        split.setResizeWeight(0.62);
        split.setDividerLocation(760);
        add(UiComponents.card("用户筛选", createFilters()), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private JPanel createFilters() {
        JPanel filters = UiComponents.toolbar();
        JButton queryButton = UiComponents.primaryButton("查询用户");
        JButton resetButton = UiComponents.secondaryButton("重置");
        filters.add(new JLabel("用户名"));
        filters.add(usernameField);
        filters.add(new JLabel("邮箱"));
        filters.add(emailField);
        filters.add(new JLabel("角色"));
        filters.add(roleFilter);
        filters.add(new JLabel("状态"));
        filters.add(statusFilter);
        filters.add(queryButton);
        filters.add(resetButton);
        queryButton.addActionListener(event -> loadUsers(filterSnapshot(), null));
        resetButton.addActionListener(event -> {
            usernameField.setText("");
            emailField.setText("");
            roleFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            loadUsers(filterSnapshot(), null);
        });
        usernameField.addActionListener(event -> loadUsers(filterSnapshot(), null));
        emailField.addActionListener(event -> loadUsers(filterSnapshot(), null));
        return filters;
    }

    private JPanel createDetailPanel() {
        JPanel actionBar = UiComponents.toolbar();
        actionBar.add(new JLabel("状态"));
        actionBar.add(targetStatusBox);
        actionBar.add(statusButton);
        actionBar.add(new JLabel("角色"));
        actionBar.add(targetRoleBox);
        actionBar.add(roleButton);
        statusButton.addActionListener(event -> changeStatus());
        roleButton.addActionListener(event -> changeRole());
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

    private void loadUsers(FilterSnapshot snapshot, String completionMessage) {
        taskExecutor.run("查询用户", () -> actions.search(snapshot.toCriteria()), users -> {
            List<User> safeUsers = users == null ? List.of() : users;
            visibleUsers.clear();
            visibleUsers.addAll(safeUsers);
            tableModel.setRowCount(0);
            for (User user : safeUsers) {
                tableModel.addRow(new Object[]{user.getUserId(), user.getUsername(), user.getEmail(), user.getPhone(),
                        UiFormatters.role(user.getRole()), user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用",
                        UiFormatters.date(user.getCreatedAt())});
            }
            resetSelection();
            detailArea.setText(safeUsers.isEmpty()
                    ? "没有找到符合条件的用户。" : "查询到 " + safeUsers.size() + " 个用户，请选择查看详情。");
            actions.setStatus(completionMessage != null ? completionMessage : "查询到 " + safeUsers.size() + " 个用户");
        });
    }

    private void selectUser(User user) {
        selectedUser = user;
        targetStatusBox.setSelectedIndex(user.getStatus() != null && user.getStatus() == 1 ? 1 : 0);
        targetRoleBox.setSelectedIndex("ADMIN".equals(user.getRole()) ? 1 : 0);
        boolean mutableTarget = user.getUserId() != null && user.getUserId() != actorUserId;
        setMutationButtonsEnabled(mutableTarget);
        long targetUserId = user.getUserId();
        detailArea.setText("正在加载用户 “" + user.getUsername() + "” 的详情……");
        taskExecutor.run("加载用户详情", () -> actions.detail(targetUserId), detail -> {
            if (isSelectedUser(targetUserId)) {
                detailArea.setText(formatDetail(detail));
            }
        });
    }

    private void changeStatus() {
        long targetUserId = requireMutableTargetId();
        int status = targetStatusBox.getSelectedIndex();
        FilterSnapshot snapshot = filterSnapshot();
        taskExecutor.run("更新用户状态", () -> actions.changeStatus(targetUserId, status), result ->
                loadUsers(snapshot, result.message()));
    }

    private void changeRole() {
        long targetUserId = requireMutableTargetId();
        String role = targetRoleBox.getSelectedIndex() == 1 ? "ADMIN" : "USER";
        FilterSnapshot snapshot = filterSnapshot();
        taskExecutor.run("更新用户角色", () -> actions.changeRole(targetUserId, role), result ->
                loadUsers(snapshot, result.message()));
    }

    private void resetSelection() {
        table.clearSelection();
        selectedUser = null;
        setMutationButtonsEnabled(false);
    }

    private void setMutationButtonsEnabled(boolean enabled) {
        statusButton.setEnabled(enabled);
        roleButton.setEnabled(enabled);
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
        return new FilterSnapshot(usernameField.getText(), emailField.getText(),
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
        return builder.append(System.lineSeparator()).append("行为概况").append(System.lineSeparator())
                .append(detail.isBehaviorDataAvailable()
                        ? "行为日志数量：" + detail.getBehaviorCount()
                        : "MongoDB 行为数据暂不可用，MySQL 用户信息仍可管理")
                .toString();
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
        usernameField.setText(username);
        emailField.setText(email);
        roleFilter.setSelectedIndex(roleIndex);
        statusFilter.setSelectedIndex(statusIndex);
    }

    void selectRow(int row) {
        table.setRowSelectionInterval(row, row);
    }

    void setTargetStatusIndex(int index) {
        targetStatusBox.setSelectedIndex(index);
    }

    void setTargetRoleIndex(int index) {
        targetRoleBox.setSelectedIndex(index);
    }

    int rowCount() {
        return tableModel.getRowCount();
    }

    boolean statusUpdateEnabled() {
        return statusButton.isEnabled();
    }

    boolean roleUpdateEnabled() {
        return roleButton.isEnabled();
    }

    String detailText() {
        return detailArea.getText();
    }

    public interface Actions {
        List<User> search(UserSearchCriteria criteria);

        AdminUserDetailDTO detail(long targetUserId);

        AdminChangeResult changeStatus(long targetUserId, int status);

        AdminChangeResult changeRole(long targetUserId, String role);

        void setStatus(String message);
    }

    private record FilterSnapshot(String username, String email, int roleIndex, int statusIndex) {
        private UserSearchCriteria toCriteria() {
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
            return new UserSearchCriteria(UiInputParsers.blankToNull(username), UiInputParsers.blankToNull(email),
                    role, status, 100, 0);
        }
    }
}
