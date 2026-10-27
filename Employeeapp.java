import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.*;

public class EmployeeApp extends JFrame {
    private JTextField nameField, roleField, emailField, deptField, salaryField, searchField;
    private DefaultTableModel tableModel;
    private JTable table;
    private Connection conn;

    public EmployeeApp() {
        setTitle("Employee Management System V2");
        setSize(900, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initDatabase();

        // Top Panel: Search & Export
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Search Name/Role:"));
        searchField = new JTextField(15);
        JButton searchButton = new JButton("Search");
        JButton exportButton = new JButton("Export to CSV");
        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(exportButton);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel: Data Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Role", "Email", "Department", "Salary"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Panel: Form Inputs & Actions
        JPanel bottomPanel = new JPanel(new GridLayout(2, 6, 5, 5));
        bottomPanel.add(new JLabel("Name:")); nameField = new JTextField(); bottomPanel.add(nameField);
        bottomPanel.add(new JLabel("Role:")); roleField = new JTextField(); bottomPanel.add(roleField);
        bottomPanel.add(new JLabel("Email:")); emailField = new JTextField(); bottomPanel.add(emailField);
        bottomPanel.add(new JLabel("Dept:")); deptField = new JTextField(); bottomPanel.add(deptField);
        bottomPanel.add(new JLabel("Salary:")); salaryField = new JTextField(); bottomPanel.add(salaryField);
        
        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        bottomPanel.add(addButton);
        bottomPanel.add(updateButton);
        bottomPanel.add(deleteButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Selection Listener
        table.getSelectionModel().addListSelectionListener(e -> populateFieldsFromTable());

        loadData("");

        // Button Actions
        addButton.addActionListener(e -> addEmployee());
        updateButton.addActionListener(e -> updateEmployee());
        deleteButton.addActionListener(e -> deleteEmployee());
        searchButton.addActionListener(e -> loadData(searchField.getText().trim()));
        exportButton.addActionListener(e -> exportToCSV());
    }

    private void initDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:employees.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS employees (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, role TEXT, email TEXT, department TEXT, salary REAL)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData(String searchTerm) {
        tableModel.setRowCount(0);
        try {
            PreparedStatement pstmt;
            if (searchTerm.isEmpty()) {
                pstmt = conn.prepareStatement("SELECT * FROM employees");
            } else {
                pstmt = conn.prepareStatement("SELECT * FROM employees WHERE name LIKE ? OR role LIKE ?");
                pstmt.setString(1, "%" + searchTerm + "%");
                pstmt.setString(2, "%" + searchTerm + "%");
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("name"), rs.getString("role"), 
                    rs.getString("email"), rs.getString("department"), rs.getDouble("salary")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        if (nameField.getText().trim().isEmpty() || roleField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Role are required.");
            return false;
        }
        if (!emailField.getText().contains("@")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address containing '@'.");
            return false;
        }
        try {
            Double.parseDouble(salaryField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Salary must be a valid number.");
            return false;
        }
        return true;
    }

    private void addEmployee() {
        if (!validateInputs()) return;
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO employees (name, role, email, department, salary) VALUES (?, ?, ?, ?, ?)");
            setPreparedStatementValues(pstmt);
            pstmt.executeUpdate();
            clearFields();
            loadData("");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateEmployee() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an employee to update.");
            return; 
        }
        if (!validateInputs()) return;
        try {
            PreparedStatement pstmt = conn.prepareStatement("UPDATE employees SET name=?, role=?, email=?, department=?, salary=? WHERE id=?");
            setPreparedStatementValues(pstmt);
            pstmt.setInt(6, (int) tableModel.getValueAt(selectedRow, 0));
            pstmt.executeUpdate();
            clearFields();
            loadData("");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteEmployee() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;
        try {
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM employees WHERE id = ?");
            pstmt.setInt(1, (int) tableModel.getValueAt(selectedRow, 0));
            pstmt.executeUpdate();
            clearFields();
            loadData("");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void exportToCSV() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("employees_export.csv"))) {
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                writer.write(tableModel.getColumnName(i) + (i == tableModel.getColumnCount() - 1 ? "" : ","));
            }
            writer.newLine();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    writer.write(tableModel.getValueAt(i, j).toString() + (j == tableModel.getColumnCount() - 1 ? "" : ","));
                }
                writer.newLine();
            }
            JOptionPane.showMessageDialog(this, "Data exported successfully to employees_export.csv!");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting data.");
        }
    }

    private void setPreparedStatementValues(PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, nameField.getText().trim());
        pstmt.setString(2, roleField.getText().trim());
        pstmt.setString(3, emailField.getText().trim());
        pstmt.setString(4, deptField.getText().trim());
        pstmt.setDouble(5, Double.parseDouble(salaryField.getText().trim()));
    }

    private void populateFieldsFromTable() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            nameField.setText(tableModel.getValueAt(row, 1).toString());
            roleField.setText(tableModel.getValueAt(row, 2).toString());
            emailField.setText(tableModel.getValueAt(row, 3).toString());
            deptField.setText(tableModel.getValueAt(row, 4).toString());
            salaryField.setText(tableModel.getValueAt(row, 5).toString());
        }
    }

    private void clearFields() {
        nameField.setText(""); roleField.setText(""); emailField.setText(""); 
        deptField.setText(""); salaryField.setText(""); table.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmployeeApp().setVisible(true));
    }
}