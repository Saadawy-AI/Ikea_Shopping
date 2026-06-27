package ikea_shopping;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ProductsPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;

    public ProductsPanel() {
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
            new String[]{"ID", "Name", "Category", "Price", "Stock", "Active"}, 0
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
        btnDelete.addActionListener(e -> deleteProduct());
        btnRefresh.addActionListener(e -> loadProducts());
        btnSearch.addActionListener(e -> searchProducts(txtSearch.getText().trim()));
        btnClear.addActionListener(e -> { txtSearch.setText(""); loadProducts(); });
        txtSearch.addActionListener(e -> searchProducts(txtSearch.getText().trim()));

        loadProducts();
    }

    // ─── Style Helper ────────────────────────────────────────
    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // ─── Load ────────────────────────────────────────────────
    private void loadProducts() {
        tableModel.setRowCount(0);
        String sql = "SELECT p.product_id, p.name, c.name AS category, p.price, p.stock_qty, p.is_active "
                   + "FROM Products p JOIN Categories c ON p.category_id = c.category_id";
        try (Connection conn = DatabaseConnection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stock_qty"),
                    rs.getBoolean("is_active") ? "Yes" : "No"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ─── Search ──────────────────────────────────────────────
    private void searchProducts(String keyword) {
        tableModel.setRowCount(0);
        String sql = "SELECT p.product_id, p.name, c.name AS category, p.price, p.stock_qty, p.is_active "
                   + "FROM Products p JOIN Categories c ON p.category_id = c.category_id "
                   + "WHERE p.name LIKE ? OR c.name LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stock_qty"),
                    rs.getBoolean("is_active") ? "Yes" : "No"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ─── Add Dialog ──────────────────────────────────────────
    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Product", true);
        dialog.setSize(400, 350);
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
            if (saveProduct(form, -1)) {
                loadProducts();
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    // ─── Edit Dialog ─────────────────────────────────────────
    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit.");
            return;
        }
        int productId = (int) tableModel.getValueAt(row, 0);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Product", true);
        dialog.setSize(400, 350);
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
            if (saveProduct(form, productId)) {
                loadProducts();
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

        JTextField txtName  = new JTextField(15);
        JTextField txtPrice = new JTextField(15);
        JTextField txtStock = new JTextField(15);
        JComboBox<String> cbCategory = new JComboBox<>();
        JCheckBox chkActive = new JCheckBox("Active", true);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT category_id, name FROM Categories")) {
            while (rs.next())
                cbCategory.addItem(rs.getInt("category_id") + " - " + rs.getString("name"));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage());
        }

        if (row != null) {
            txtName.setText((String) tableModel.getValueAt(row, 1));
            txtPrice.setText(String.valueOf(tableModel.getValueAt(row, 3)));
            txtStock.setText(String.valueOf(tableModel.getValueAt(row, 4)));
            chkActive.setSelected(tableModel.getValueAt(row, 5).equals("Yes"));
        }

        panel.putClientProperty("txtName",    txtName);
        panel.putClientProperty("txtPrice",   txtPrice);
        panel.putClientProperty("txtStock",   txtStock);
        panel.putClientProperty("cbCategory", cbCategory);
        panel.putClientProperty("chkActive",  chkActive);

        String[]    labels = {"Name:", "Category:", "Price:", "Stock:", ""};
        Component[] fields = {txtName, cbCategory, txtPrice, txtStock, chkActive};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            panel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1;
            panel.add(fields[i], gbc);
        }

        return panel;
    }

    // ─── Save ────────────────────────────────────────────────
    private boolean saveProduct(JPanel form, int productId) {
        JTextField   txtName  = (JTextField)   form.getClientProperty("txtName");
        JTextField   txtPrice = (JTextField)   form.getClientProperty("txtPrice");
        JTextField   txtStock = (JTextField)   form.getClientProperty("txtStock");
        JComboBox<?> cbCat    = (JComboBox<?>)  form.getClientProperty("cbCategory");
        JCheckBox    chkActive = (JCheckBox)   form.getClientProperty("chkActive");

        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.");
            return false;
        }

        double price; int stock;
        try {
            price = Double.parseDouble(txtPrice.getText().trim());
            stock = Integer.parseInt(txtStock.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price and Stock must be numbers.");
            return false;
        }

        int categoryId = Integer.parseInt(cbCat.getSelectedItem().toString().split(" - ")[0]);
        int isActive   = chkActive.isSelected() ? 1 : 0;

        String sql = productId == -1
            ? "INSERT INTO Products (name, category_id, price, stock_qty, is_active) VALUES (?, ?, ?, ?, ?)"
            : "UPDATE Products SET name=?, category_id=?, price=?, stock_qty=?, is_active=? WHERE product_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, categoryId);
            ps.setDouble(3, price);
            ps.setInt(4, stock);
            ps.setInt(5, isActive);
            if (productId != -1) ps.setInt(6, productId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, productId == -1 ? "Product added!" : "Product updated!");
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return false;
        }
    }

    // ─── Delete ──────────────────────────────────────────────
    private void deleteProduct() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete.");
            return;
        }

        int    productId = (int)    tableModel.getValueAt(row, 0);
        String name      = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete: " + name + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM Products WHERE product_id = ?")) {
                ps.setInt(1, productId);
                ps.executeUpdate();
                loadProducts();
                JOptionPane.showMessageDialog(this, "Product deleted!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
}
        