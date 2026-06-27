package ikea_shopping;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class OrdersPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;

    public OrdersPanel() {
        setLayout(new BorderLayout(5, 5));

        Color ikeaYellow = new Color(255, 189, 0);
        Color ikeaBlue   = new Color(0, 88, 163);
        Color red        = new Color(200, 50, 50);

        // ─── Buttons Panel ───────────────────────────────────
        JButton btnAdd     = new JButton("➕ Add");
        JButton btnEdit    = new JButton("✏️ Edit Status");
        JButton btnDelete  = new JButton("🗑️ Delete");
        JButton btnRefresh = new JButton("🔄 Refresh");

        styleButton(btnAdd,     ikeaYellow);
        styleButton(btnEdit,    ikeaYellow);
        styleButton(btnDelete,  red);
        styleButton(btnRefresh, ikeaBlue);
        btnDelete.setForeground(Color.WHITE);
        btnRefresh.setForeground(Color.WHITE);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        // ─── Search Panel ────────────────────────────────────
        JTextField txtSearch = new JTextField(20);
        JButton btnSearch    = new JButton("🔍 Search");
        JButton btnClear     = new JButton("✖ Clear");

        styleButton(btnSearch, ikeaBlue);
        styleButton(btnClear,  ikeaYellow);
        btnSearch.setForeground(Color.WHITE);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClear);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(btnPanel,    BorderLayout.WEST);
        topPanel.add(searchPanel, BorderLayout.EAST);

        // ─── Table ───────────────────────────────────────────
        tableModel = new DefaultTableModel(
            new String[]{"Order ID", "Customer", "Address", "Status", "Total", "Date"}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);

        add(topPanel,   BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // ─── Actions ─────────────────────────────────────────
        btnAdd.addActionListener(e -> showAddDialog());
        btnEdit.addActionListener(e -> showEditDialog());
        btnDelete.addActionListener(e -> deleteOrder());
        btnRefresh.addActionListener(e -> loadOrders());
        btnSearch.addActionListener(e -> searchOrders(txtSearch.getText().trim()));
        btnClear.addActionListener(e -> { txtSearch.setText(""); loadOrders(); });
        txtSearch.addActionListener(e -> searchOrders(txtSearch.getText().trim()));

        loadOrders();
    }

    // ─── Style Helper ────────────────────────────────────────
    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // ─── Load ────────────────────────────────────────────────
    private void loadOrders() {
        tableModel.setRowCount(0);
        String sql = "SELECT o.order_id, u.first_name + ' ' + u.last_name AS customer, "
                   + "a.street + ', ' + a.city AS address, "
                   + "o.status, o.total_price, o.created_at "
                   + "FROM Orders o "
                   + "JOIN Users u ON o.user_id = u.user_id "
                   + "JOIN Addresses a ON o.address_id = a.address_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("order_id"),
                    rs.getString("customer"),
                    rs.getString("address"),
                    rs.getString("status"),
                    rs.getDouble("total_price"),
                    rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ─── Search ──────────────────────────────────────────────
    private void searchOrders(String keyword) {
        tableModel.setRowCount(0);
        String sql = "SELECT o.order_id, u.first_name + ' ' + u.last_name AS customer, "
                   + "a.street + ', ' + a.city AS address, "
                   + "o.status, o.total_price, o.created_at "
                   + "FROM Orders o "
                   + "JOIN Users u ON o.user_id = u.user_id "
                   + "JOIN Addresses a ON o.address_id = a.address_id "
                   + "WHERE u.first_name LIKE ? OR u.last_name LIKE ? OR o.status LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("order_id"),
                    rs.getString("customer"),
                    rs.getString("address"),
                    rs.getString("status"),
                    rs.getDouble("total_price"),
                    rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ─── Add Dialog ──────────────────────────────────────────
    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Order", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = buildAddForm();
        JButton btnSave = new JButton("Save");
        styleButton(btnSave, new Color(0, 88, 163));
        btnSave.setForeground(Color.WHITE);

        JPanel bottom = new JPanel();
        bottom.add(btnSave);

        dialog.add(form,   BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> {
            if (saveOrder(form)) {
                loadOrders();
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    // ─── Build Add Form ──────────────────────────────────────
    private JPanel buildAddForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> cbUser    = new JComboBox<>();
        JComboBox<String> cbAddress = new JComboBox<>();
        JComboBox<String> cbStatus  = new JComboBox<>(
            new String[]{"pending", "processing", "shipped", "delivered", "cancelled"}
        );
        JTextField txtTotal = new JTextField(15);
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT user_id, first_name + ' ' + last_name AS name FROM Users")) {
            while (rs.next())
                cbUser.addItem(rs.getInt("user_id") + " - " + rs.getString("name"));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT address_id, street + ', ' + city AS addr FROM Addresses")) {
            while (rs.next())
                cbAddress.addItem(rs.getInt("address_id") + " - " + rs.getString("addr"));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }

        panel.putClientProperty("cbUser",    cbUser);
        panel.putClientProperty("cbAddress", cbAddress);
        panel.putClientProperty("cbStatus",  cbStatus);
        panel.putClientProperty("txtTotal",  txtTotal);

        String[]    labels = {"User:", "Address:", "Status:", "Total Price:"};
        Component[] fields = {cbUser, cbAddress, cbStatus, txtTotal};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            panel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1;
            panel.add(fields[i], gbc);
        }

        return panel;
    }

    // ─── Save Order ──────────────────────────────────────────
    private boolean saveOrder(JPanel form) {
        JComboBox<?> cbUser    = (JComboBox<?>) form.getClientProperty("cbUser");
        JComboBox<?> cbAddress = (JComboBox<?>) form.getClientProperty("cbAddress");
        JComboBox<?> cbStatus  = (JComboBox<?>) form.getClientProperty("cbStatus");
        JTextField   txtTotal  = (JTextField)   form.getClientProperty("txtTotal");

        double total;
        try {
            total = Double.parseDouble(txtTotal.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Total Price must be a number.");
            return false;
        }

        int    userId    = Integer.parseInt(cbUser.getSelectedItem().toString().split(" - ")[0]);
        int    addressId = Integer.parseInt(cbAddress.getSelectedItem().toString().split(" - ")[0]);
        String status    = cbStatus.getSelectedItem().toString();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Orders (user_id, address_id, status, total_price) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, addressId);
            ps.setString(3, status);
            ps.setDouble(4, total);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Order added!");
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return false;
        }
    }

    // ─── Edit Status ─────────────────────────────────────────
    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order to edit.");
            return;
        }

        int    orderId       = (int)    tableModel.getValueAt(row, 0);
        String currentStatus = (String) tableModel.getValueAt(row, 3);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Order Status", true);
        dialog.setSize(350, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JComboBox<String> cbStatus = new JComboBox<>(
            new String[]{"pending", "processing", "shipped", "delivered", "cancelled"}
        );
        cbStatus.setSelectedItem(currentStatus);

        JPanel form = new JPanel(new FlowLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.add(new JLabel("Status:"));
        form.add(cbStatus);

        JButton btnUpdate = new JButton("Update");
        styleButton(btnUpdate, new Color(0, 88, 163));
        btnUpdate.setForeground(Color.WHITE);

        JPanel bottom = new JPanel();
        bottom.add(btnUpdate);

        dialog.add(form,   BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        btnUpdate.addActionListener(e -> {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE Orders SET status = ? WHERE order_id = ?")) {
                ps.setString(1, cbStatus.getSelectedItem().toString());
                ps.setInt(2, orderId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Order status updated!");
                loadOrders();
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    // ─── Delete ──────────────────────────────────────────────
    private void deleteOrder() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order to delete.");
            return;
        }

        int orderId = (int) tableModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete Order #" + orderId + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM Orders WHERE order_id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
                loadOrders();
                JOptionPane.showMessageDialog(this, "Order deleted!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
}