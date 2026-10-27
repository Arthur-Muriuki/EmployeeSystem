# Employee Management System

A lightweight desktop application built with Java Swing and SQLite for managing employee records.

## Features
* **Add Employees:** Input new employee names and roles.
* **View Records:** Display all employees in a clean tabular interface.
* **Local Storage:** Zero-configuration local database using SQLite.

## Requirements
* Java Development Kit (JDK) 8 or higher
* SQLite JDBC Driver (v3.42.0.0+)

## How to Run
1. Clone this repository.
2. Ensure the SQLite JDBC driver is in the `lib` folder.
3. Compile: `javac -cp ".;lib/sqlite-jdbc.jar" EmployeeApp.java` (Use `:` instead of `;` on Mac/Linux)
4. Run: `java -cp ".;lib/sqlite-jdbc.jar" EmployeeApp`