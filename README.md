# MediBook — Online Doctor Appointment System

MediBook is a web-based healthcare appointment management system designed to help patients explore doctors and manage appointments through a simple, user-friendly interface.

## Features

- **User Registration and Login** — User authentication for accessing the application.
- **Doctor Directory** — View doctors and their specializations.
- **Doctor Search and Filtering** — Find doctors based on available information.
- **Appointment Booking** — Select a doctor and enter appointment details.
- **Database Integration** — MySQL database integration through a Java backend.
- **Responsive Interface** — HTML, CSS, and JavaScript-based frontend.

## Technologies Used

- **Frontend:** HTML, CSS, JavaScript
- **Backend:** Java
- **Database:** MySQL
- **Database Connectivity:** JDBC
- **Version Control:** Git and GitHub

## Project Structure

```text
MediBook/
├── backend/
│   ├── lib/
│   └── src/
│       └── Main.java
├── css/
├── js/
├── pages/
├── index.html
└── README.md
```

## Prerequisites

- Java Development Kit (JDK)
- MySQL Server
- MySQL Connector/J
- A modern web browser

## Setup Instructions

1. Clone or download this repository.
2. Create the `medibook` database in MySQL.
3. Configure the required database tables.
4. Set the `MEDIBOOK_DB_PASSWORD` environment variable to your MySQL password.
5. Compile and run the Java backend from the `backend` directory.

Compile:

```powershell
javac --add-modules jdk.httpserver -cp "lib\*" -d out src\Main.java
```

Run:

```powershell
java --add-modules jdk.httpserver -cp "out;lib\*" Main
```

6. Open `index.html` in your browser to access the frontend.

The backend runs at `http://localhost:8080` by default.

**Note:** Database credentials must be configured locally. Never publish passwords or other secrets in the repository.

## Project Goal

MediBook aims to simplify the process of finding doctors and managing appointments using a straightforward web application backed by a relational database.

## Author

Ruchira Paul
