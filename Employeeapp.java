// ==========================================
// IMPORTS: Bringing in built-in Java libraries
// ==========================================
import javax.swing.*;                  // Contains graphical user interface (GUI) components like buttons, text fields, and windows
import javax.swing.table.DefaultTableModel; // Allows us to add, update, and clear rows inside our table grid
import java.awt.*;                     // Contains layout managers (BorderLayout, FlowLayout, GridLayout) and fonts
import java.io.BufferedWriter;          // Writes text to files efficiently
import java.io.FileWriter;              // Creates/opens physical files on your hard drive for writing
import java.sql.*;                     // Contains database tools (Connection, Statement, PreparedStatement, ResultSet)

// "extends JFrame" means this class creates a main window for a desktop application
public class EmployeeApp extends JFrame {
    
    // ==========================================
    // CLASS VARIABLES (FIELDS)
    // These variables can be accessed anywhere inside this class
    // ==========================================
    
    // Text boxes where users type information
    private JTextField nameField, roleField, emailField, salaryField, searchField;
    
    // Dropdown list for selecting a department
    private JComboBox<String> deptDropdown;
    
    // Controls the raw data inside the table (rows and columns)
    private DefaultTableModel tableModel;
    
    // The visual table component displayed on screen
    private JTable table;
    
    // Holds the active connection to our local SQLite database file
    private Connection conn;

    // Fixed array of department options for the software company dropdown
    private final String[] DEPARTMENTS = {
        "Frontend Engineering", 
        "Backend Engineering", 
        "Full Stack Engineering", 
        "DevOps & Infrastructure", 
        "QA & Testing", 
        "UI/UX Design", 
        "Product Management"
    };

    // ==========================================
    // CONSTRUCTOR: Prepares and builds the GUI
    // This code runs automatically when the app starts
    // ==========================================
    public EmployeeApp() {
        // 1. Set basic window properties
        setTitle("AM & Devs - Software Engineering Management System"); // Title shown in the window bar
        setSize(950, 520);                                             // Window dimensions: width 950px, height 520px
        setDefaultCloseOperation(EXIT_ON_CLOSE);                        // Completely close app when clicking the 'X' button
        setLayout(new BorderLayout());                                 // Divide window into 5 zones: North, South, East, West, Center

        // 2. Connect to database and create table if it doesn't exist
        initDatabase();

        // ==========================================
        // TOP PANEL: Search bar, company name, and export button
        // ==========================================
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // FlowLayout aligns items horizontally from left to right
        
        // Brand label
        JLabel brandLabel = new JLabel("AM & Devs | ");
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 14)); // Make text bold and slightly larger
        topPanel.add(brandLabel);

        // Search inputs
        topPanel.add(new JLabel("Search Dev/Role:"));
        searchField = new JTextField(15);                         // Text field that is 15 columns wide
        JButton searchButton = new JButton("Search");
        JButton exportButton = new JButton("Export Roster (CSV)");
        
        // Add search components to top panel
        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(exportButton);
        
        // Place top panel at the top (NORTH) of the window
        add(topPanel, BorderLayout.NORTH);

        // ==========================================
        // CENTER PANEL: Main data table
        // ==========================================
        // Define column headers and start with 0 initial rows
        tableModel = new DefaultTableModel(new String[]{"ID", "Developer Name", "Role", "Email", "Department", "Salary (KSh)"}, 0);
        table = new JTable(tableModel);                           // Put data model inside the visual table
        add(new JScrollPane(table), BorderLayout.CENTER);          // Wrap table in a scroll pane and put it in the CENTER zone

        // ==========================================
        // BOTTOM PANEL: Form inputs and Action buttons
        // ==========================================
        // Grid layout with 2 rows, 6 columns, and 5-pixel spacing between items
        JPanel bottomPanel = new JPanel(new GridLayout(2, 6, 5, 5));
        
        // Row 1 & 2 Inputs: Add labels and text boxes
        bottomPanel.add(new JLabel("Name:")); 
        nameField = new JTextField(); 
        bottomPanel.add(nameField);

        bottomPanel.add(new JLabel("Role:")); 
        roleField = new JTextField(); 
        bottomPanel.add(roleField);

        bottomPanel.add(new JLabel("Email:")); 
        emailField = new JTextField(); 
        bottomPanel.add(emailField);
        
        bottomPanel.add(new JLabel("Dept:")); 
        deptDropdown = new JComboBox<>(DEPARTMENTS);               // Populate dropdown with our array
        bottomPanel.add(deptDropdown);
        
        bottomPanel.add(new JLabel("Salary (KSh):")); 
        salaryField = new JTextField(); 
        bottomPanel.add(salaryField);
        
        // Action buttons
        JButton addButton = new JButton("Add Developer");
        JButton updateButton = new JButton("Update Record");
        JButton deleteButton = new JButton("Remove Developer");
        
        bottomPanel.add(addButton);
        bottomPanel.add(updateButton);
        bottomPanel.add(deleteButton);
        
        // Place bottom panel at the bottom (SOUTH) of the window
        add(bottomPanel, BorderLayout.SOUTH);

        // ==========================================
        // EVENT LISTENERS: Responding to user clicks
        // ==========================================
        
        // When user clicks a row in the table, populate form inputs with that row's data
        table.getSelectionModel().addListSelectionListener(e -> populateFieldsFromTable());

        // Load all database records into table when app starts
        loadData("");

        // Attach action handlers to buttons
        addButton.addActionListener(e -> addEmployee());
        updateButton.addActionListener(e -> updateEmployee());
        deleteButton.addActionListener(e -> deleteEmployee());
        searchButton.addActionListener(e -> loadData(searchField.getText().trim()));
        exportButton.addActionListener(e -> exportToCSV());
    }

    // ==========================================
    // DATABASE INITIALIZATION
    // ==========================================
    private void initDatabase() {
        try {
            // Creates or opens 'employees.db' file locally in the project directory
            conn = DriverManager.getConnection("jdbc:sqlite:employees.db");
            Statement stmt = conn.createStatement();
            
            // SQL command: Creates 'employees' table if it doesn't already exist
            stmt.execute("CREATE TABLE IF NOT EXISTS employees (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " + // ID automatically increases for every new employee
                        "name TEXT, " +
                        "role TEXT, " +
                        "email TEXT, " +
                        "department TEXT, " +
                        "salary REAL)");                            // REAL stores decimal numbers for salary
        } catch (SQLException e) {
            e.printStackTrace(); // Prints error details to terminal if database connection fails
        }
    }

    // ==========================================
    // LOAD & SEARCH DATA FROM DATABASE
    // ==========================================
    private void loadData(String searchTerm) {
        tableModel.setRowCount(0); // Wipe out existing table rows in the UI before reloading
        try {
            PreparedStatement pstmt;
            
            // If search bar is empty, fetch all employees
            if (searchTerm.isEmpty()) {
                pstmt = conn.prepareStatement("SELECT * FROM employees");
            } else {
                // Search matching name, role, or department using SQL LIKE wildcard (%)
                pstmt = conn.prepareStatement("SELECT * FROM employees WHERE name LIKE ? OR role LIKE ? OR department LIKE ?");
                pstmt.setString(1, "%" + searchTerm + "%");
                pstmt.setString(2, "%" + searchTerm + "%");
                pstmt.setString(3, "%" + searchTerm + "%");
            }
            
            // Execute query and store results in ResultSet 'rs'
            ResultSet rs = pstmt.executeQuery();
            
            // Loop through each record returned by the database
            while (rs.next()) {
                // Add new row into tableModel using database values
                tableModel.addRow(new Object[]{
                    rs.getInt("id"), 
                    rs.getString("name"), 
                    rs.getString("role"), 
                    rs.getString("email"), 
                    rs.getString("department"), 
                    rs.getDouble("salary")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // INPUT VALIDATION: Checks required fields before saving
    // ==========================================
    private boolean validateInputs() {
        // Ensure Name and Role fields are not empty
        if (nameField.getText().trim().isEmpty() || roleField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Role are required.");
            return false;
        }
        
        // Ensure email contains an '@' symbol
        if (!emailField.getText().contains("@")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.");
            return false;
        }
        
        // Ensure salary is a valid numeric value
        try {
            Double.parseDouble(salaryField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Salary must be a valid number.");
            return false; // Return false if user typed letters in salary field
        }
        
        return true; // Inputs passed all checks
    }

    // ==========================================
    // ADD NEW EMPLOYEE
    // ==========================================
    private void addEmployee() {
        if (!validateInputs()) return; // Stop if user input is invalid
        
        try {
            // SQL query to insert new row into database
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO employees (name, role, email, department, salary) VALUES (?, ?, ?, ?, ?)"
            );
            
            setPreparedStatementValues(pstmt); // Fill parameters 1 through 5
            pstmt.executeUpdate();             // Run insert command
            
            clearFields();                     // Reset UI fields
            loadData("");                      // Refresh table view
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // UPDATE EXISTING EMPLOYEE
    // ==========================================
    private void updateEmployee() {
        int selectedRow = table.getSelectedRow();
        
        // Check if user clicked a row in table
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a team member to update.");
            return; 
        }
        
        if (!validateInputs()) return;
        
        try {
            // SQL query to update existing record by matching ID
            PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE employees SET name=?, role=?, email=?, department=?, salary=? WHERE id=?"
            );
            
            setPreparedStatementValues(pstmt); // Fill parameters 1 through 5
            
            // Parameter 6: Get employee ID from 1st column (index 0) of selected table row
            pstmt.setInt(6, (int) tableModel.getValueAt(selectedRow, 0));
            
            pstmt.executeUpdate();             // Run update command
            clearFields();                     // Reset UI fields
            loadData("");                      // Refresh table view
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // DELETE EMPLOYEE
    // ==========================================
    private void deleteEmployee() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return; // Do nothing if no row is selected
        
        try {
            // SQL query to remove record matching ID
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM employees WHERE id = ?");
            pstmt.setInt(1, (int) tableModel.getValueAt(selectedRow, 0)); // Set parameter 1 to selected employee ID
            pstmt.executeUpdate();
            
            clearFields();
            loadData("");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // EXPORT TO CSV FILE
    // ==========================================
    private void exportToCSV() {
        // Try-with-resources: Automatically closes file writer when done
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("am_and_devs_roster.csv"))) {
            
            // 1. Write column headers separated by commas
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                writer.write(tableModel.getColumnName(i) + (i == tableModel.getColumnCount() - 1 ? "" : ","));
            }
            writer.newLine(); // Move to next line
            
            // 2. Loop through rows and write data separated by commas
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    writer.write(tableModel.getValueAt(i, j).toString() + (j == tableModel.getColumnCount() - 1 ? "" : ","));
                }
                writer.newLine(); // Move to next line after writing each row
            }
            
            JOptionPane.showMessageDialog(this, "Roster exported to am_and_devs_roster.csv!");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting roster.");
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    // Helper method to fill SQL parameters from form fields
    private void setPreparedStatementValues(PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, nameField.getText().trim());
        pstmt.setString(2, roleField.getText().trim());
        pstmt.setString(3, emailField.getText().trim());
        pstmt.setString(4, deptDropdown.getSelectedItem().toString());
        pstmt.setDouble(5, Double.parseDouble(salaryField.getText().trim()));
    }

    // Reads selected row data from table and puts values back into input fields for editing
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

    // Resets input fields and dropdown back to default empty state
    private void clearFields() {
        nameField.setText(""); 
        roleField.setText(""); 
        emailField.setText(""); 
        salaryField.setText(""); 
        deptDropdown.setSelectedIndex(0); 
        table.clearSelection();
    }

    // ==========================================
    // MAIN METHOD: Program entry point
    // ==========================================
    public static void main(String[] args) {
        // SwingUtilities.invokeLater runs GUI creation on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> new EmployeeApp().setVisible(true));
    }
}