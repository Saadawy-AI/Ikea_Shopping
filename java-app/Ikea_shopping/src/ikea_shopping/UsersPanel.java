package ikea_shopping;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UsersPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;

    public UsersPanel() {
        setLayout(new BorderLayout(5, 5));

        Color ikeaYellow = new Color(255, 189, 0);
        Color ikeaBlue   = new Color(0, 88, 163);
        Color red        = new Color(200, 50, 50);

        // ─── Buttons Panel ───────────────────────────────────
        JButton btnAdd     = new JButton("➕ Add");
        JButton btnEdit    = new JButton("✏️ Edit");
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
            new String[]{"ID", "First Name", "Last Name", "Email", "Phone", "Active"}, 0
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
        btnDelete.addActionListener(e -> deleteUser());
        btnRefresh.addActionListener(e -> loadUsers());
        btnSearch.addActionListener(e -> searchUsers(txtSearch.getText().trim()));
        btnClear.addActionListener(e -> { txtSearch.setText(""); loadUsers(); });
        txtSearch.addActionListener(e -> searchUsers(txtSearch.getText().trim()));

        loadUsers();
    }

    // ─── Style Helper ────────────────────────────────────────
    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // ─── Load ────────────────────────────────────────────────
    private void loadUsers() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT user_id, first_name, last_name, email, phone, is_active FROM Users")) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getBoolean("is_active") ? "Yes" : "No"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ─── Search ──────────────────────────────────────────────
    private void searchUsers(String keyword) {
        tableModel.setRowCount(0);
        String sql = "SELECT user_id, first_name, last_name, email, phone, is_active FROM Users "
                   + "WHERE first_name LIKE ? OR last_name LIKE ? OR email LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getBoolean("is_active") ? "Yes" : "No"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ─── Add Dialog ──────────────────────────────────────────
    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add User", true);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = buildForm(null);
        JButton btnSave = new JButton("Save");
        styleButton(btnSave, new Color(0, 88, 163));
        btnSave.setForeground(Color.WHITE);

        JPanel bottom = new JPanel();
        bottom.add(btnSave);

        dialog.add(form,   BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> {
            if (saveUser(form, -1)) {
                loadUsers();
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    // ─── Edit Dialog ─────────────────────────────────────────
    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.");
            return;
        }
        int userId = (int) tableModel.getValueAt(row, 0);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit User", true);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = buildForm(row);
        JButton btnUpdate = new JButton("Update");
        styleButton(btnUpdate, new Color(0, 88, 163));
        btnUpdate.setForeground(Color.WHITE);

        JPanel bottom = new JPanel();
        bottom.add(btnUpdate);

        dialog.add(form,   BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        btnUpdate.addActionListener(e -> {
            if (saveUser(form, userId)) {
                loadUsers();
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    // ─── Build Form ──────────────────────────────────────────
    private JPanel buildForm(Integer row) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField     txtFirstName = new JTextField(15);
        JTextField     txtLastName  = new JTextField(15);
        JTextField     txtEmail     = new JTextField(15);
        JPasswordField txtPassword  = new JPasswordField(15);
        JTextField     txtPhone     = new JTextField(15);
        JCheckBox      chkActive    = new JCheckBox("Active", true);

        if (row != null) {
            txtFirstName.setText((String) tableModel.getValueAt(row, 1));
            txtLastName.setText((String)  tableModel.getValueAt(row, 2));
            txtEmail.setText((String)     tableModel.getValueAt(row, 3));
            txtPhone.setText((String)     tableModel.getValueAt(row, 4));
            chkActive.setSelected(tableModel.getValueAt(row, 5).equals("Yes"));
        }

        panel.putClientProperty("txtFirstName", txtFirstName);
        panel.putClientProperty("txtLastName",  txtLastName);
        panel.putClientProperty("txtEmail",     txtEmail);
        panel.putClientProperty("txtPassword",  txtPassword);
        panel.putClientProperty("txtPhone",     txtPhone);
        panel.putClientProperty("chkActive",    chkActive);

        String[]    labels = {"First Name:", "Last Name:", "Email:", "Password:", "Phone:", ""};
        Component[] fields = {txtFirstName, txtLastName, txtEmail, txtPassword, txtPhone, chkActive};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            panel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1;
            panel.add(fields[i], gbc);
        }

        return panel;
    }

    // ─── Save ────────────────────────────────────────────────
    private boolean saveUser(JPanel form, int userId) {
        JTextField     txtFirstName = (JTextField)     form.getClientProperty("txtFirstName");
        JTextField     txtLastName  = (JTextField)     form.getClientProperty("txtLastName");
        JTextField     txtEmail     = (JTextField)     form.getClientProperty("txtEmail");
        JPasswordField txtPassword  = (JPasswordField) form.getClientProperty("txtPassword");
        JTextField     txtPhone     = (JTextField)     form.getClientProperty("txtPhone");
        JCheckBox      chkActive    = (JCheckBox)      form.getClientProperty("chkActive");

        String firstName = txtFirstName.getText().trim();
        String lastName  = txtLastName.getText().trim();
        String email     = txtEmail.getText().trim();
        String password  = new String(txtPassword.getPassword()).trim();
        String phone     = txtPhone.getText().trim();
        int    isActive  = chkActive.isSelected() ? 1 : 0;

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "First Name, Last Name and Email are required.");
            return false;
        }

        if (userId == -1 && password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password is required.");
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (userId == -1) {
                // ─── Add ─────────────────────────────────────
                String sql = "INSERT INTO Users (first_name, last_name, email, password_hash, phone, is_active) "
                           + "VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                ps.setString(4, PasswordUtils.hash(password)); // ← Hashing
                ps.setString(5, phone);
                ps.setInt(6, isActive);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "User added!");
            } else {
                // ─── Edit ─────────────────────────────────────
                String sql = password.isEmpty()
                    ? "UPDATE Users SET first_name=?, last_name=?, email=?, phone=?, is_active=? WHERE user_id=?"
                    : "UPDATE Users SET first_name=?, last_name=?, email=?, password_hash=?, phone=?, is_active=? WHERE user_id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                if (!password.isEmpty()) {
                    ps.setString(4, PasswordUtils.hash(password)); // ← Hashing
                    ps.setString(5, phone);
                    ps.setInt(6, isActive);
                    ps.setInt(7, userId);
                } else {
                    ps.setString(4, phone);
                    ps.setInt(5, isActive);
                    ps.setInt(6, userId);
                }
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "User updated!");
            }
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return false;
        }
    }

    // ─── Delete ──────────────────────────────────────────────
    private void deleteUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.");
            return;
        }

        int    userId = (int)    tableModel.getValueAt(row, 0);
        String name   = tableModel.getValueAt(row, 1) + " " + tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete: " + name + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM Users WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
                loadUsers();
                JOptionPane.showMessageDialog(this, "User deleted!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
}