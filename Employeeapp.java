import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.*;

public class EmployeeApp extends JFrame {
    private JTextField nameField, roleField, emailField, salaryField, searchField;
    private JComboBox<String> deptDropdown;
    private DefaultTableModel tableModel;
    private JTable table;
    private Connection conn;

    private final String[] DEPARTMENTS = {
        "Frontend Engineering", 
        "Backend Engineering", 
        "Full Stack Engineering", 
        "DevOps & Infrastructure", 
        "QA & Testing", 
        "UI/UX Design", 
        "Product Management"
    };

    public EmployeeApp() {
        // 1. Replaced em-dash with standard hyphen to fix 'â€“' mojibake
        setTitle("AM & Devs - Software Engineering Management System");
        setSize(950, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initDatabase();

        // Top Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel brandLabel = new JLabel("AM & Devs | ");
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(brandLabel);

        topPanel.add(new JLabel("Search Dev/Role:"));
        searchField = new JTextField(15);
        JButton searchButton = new JButton("Search");
        JButton exportButton = new JButton("Export Roster (CSV)");
        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(exportButton);
        add(topPanel, BorderLayout.NORTH);

        // 2. Updated Table Column Header to KSh
        tableModel = new DefaultTableModel(new String[]{"ID", "Developer Name", "Role", "Email", "Department", "Salary (KSh)"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new GridLayout(2, 6, 5, 5));
        bottomPanel.add(new JLabel("Name:")); nameField = new JTextField(); bottomPanel.add(nameField);
        bottomPanel.add(new JLabel("Role:")); roleField = new JTextField(); bottomPanel.add(roleField);
        bottomPanel.add(new JLabel("Email:")); emailField = new JTextField(); bottomPanel.add(emailField);
        
        bottomPanel.add(new JLabel("Dept:")); 
        deptDropdown = new JComboBox<>(DEPARTMENTS); 
        bottomPanel.add(deptDropdown);
        
        // 3. Updated Form Field Label to KSh
        bottomPanel.add(new JLabel("Salary (KSh):")); salaryField = new JTextField(); bottomPanel.add(salaryField);
        
        JButton addButton = new JButton("Add Developer");
        JButton updateButton = new JButton("Update Record");
        JButton deleteButton = new JButton("Remove Developer");
        bottomPanel.add(addButton);
        bottomPanel.add(updateButton);
        bottomPanel.add(deleteButton);
        add(bottomPanel, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> populateFieldsFromTable());

        loadData("");

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
                pstmt = conn.prepareStatement("SELECT * FROM employees WHERE name LIKE ? OR role LIKE ? OR department LIKE ?");
                pstmt.setString(1, "%" + searchTerm + "%");
                pstmt.setString(2, "%" + searchTerm + "%");
                pstmt.setString(3, "%" + searchTerm + "%");
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
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.");
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
            JOptionPane.showMessageDialog(this, "Select a team member to update.");
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
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("am_and_devs_roster.csv"))) {
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
            JOptionPane.showMessageDialog(this, "Roster exported to am_and_devs_roster.csv!");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting roster.");
        }
    }

    private void setPreparedStatementValues(PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, nameField.getText().trim());
        pstmt.setString(2, roleField.getText().trim());
        pstmt.setString(3, emailField.getText().trim());
        pstmt.setString(4, deptDropdown.getSelectedItem().toString());
        pstmt.setDouble(5, Double.parseDouble(salaryField.getText().trim()));
    }

    private void populateFieldsFromTable() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            nameField.setText(tableModel.getValueAt(row, 1).toString());
            roleField.setText(tableModel.getValueAt(row, 2).toString());
            emailField.setText(tableModel.getValueAt(row, 3).toString());
            deptDropdown.setSelectedItem(tableModel.getValueAt(row, 4).toString());
            salaryField.setText(tableModel.getValueAt(row, 5).toString());
        }
    }

    private void clearFields() {
        nameField.setText(""); roleField.setText(""); emailField.setText(""); 
        salaryField.setText(""); deptDropdown.setSelectedIndex(0); table.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmployeeApp().setVisible(true));
    }
}