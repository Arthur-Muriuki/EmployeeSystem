import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EmployeeApp extends JFrame {
    private JTextField nameField, roleField;
    private DefaultTableModel tableModel;
    private Connection conn;

    public EmployeeApp() {
        setTitle("Employee Management System");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Initialize Database
        initDatabase();

        // 2. Set Up Top Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(1, 5, 5, 5));
        inputPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        inputPanel.add(nameField);
        
        inputPanel.add(new JLabel("Role:"));
        roleField = new JTextField();
        inputPanel.add(roleField);

        JButton addButton = new JButton("Add");
        inputPanel.add(addButton);
        add(inputPanel, BorderLayout.NORTH);

        // 3. Set Up Data Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Role"}, 0);
        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // 4. Load Existing Data & Button Action
        loadData();
        addButton.addActionListener(e -> addEmployee());
    }

    private void initDatabase() {
        try {
            // Creates employees.db locally if it doesn't exist
            conn = DriverManager.getConnection("jdbc:sqlite:employees.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS employees (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, role TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        tableModel.setRowCount(0); // Clear table
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM employees");
            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getString("role")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addEmployee() {
        String name = nameField.getText().trim();
        String role = roleField.getText().trim();
        if (name.isEmpty() || role.isEmpty()) return;

        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO employees (name, role) VALUES (?, ?)");
            pstmt.setString(1, name);
            pstmt.setString(2, role);
            pstmt.executeUpdate();
            
            nameField.setText("");
            roleField.setText("");
            loadData(); // Refresh UI table
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmployeeApp().setVisible(true));
    }
}