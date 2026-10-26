# MediBook — Online Doctor Appointment System

MediBook is a web-based Online Doctor Appointment System designed to simplify the process of finding doctors, exploring hospitals, checking doctor availability, and managing appointments through a simple, user-friendly interface.

## Features

- **User Registration and Login** — Authentication for accessing the application.
- **Doctor Directory** — View doctors and their specializations.
- **Doctor Search and Filtering** — Find doctors based on available information.
- **Appointment Booking** — Select a doctor and enter appointment details.
- **Patient Module** — Access the application and manage appointment-related activities.
- **Doctor Dashboard** — Dedicated interface for doctor-related activities.
- **Admin Dashboard** — Interface for administrative activities.
- **Hospital Search** — Explore available hospitals and their locations.
- **Doctor Availability** — Hospital-associated doctor availability and consultation information, where configured.
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
│   ├── src/
│   │   └── Main.java
│   └── .gitignore
├── css/
├── js/
├── pages/
│   ├── doctors.html
│   ├── admin-dashboard.html
│   ├── doctor-dashboard.html
│   └── hospitals.html
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
3. Create and configure the required database tables.
4. Set the `MEDIBOOK_DB_PASSWORD` environment variable to your local MySQL password.
5. Open a terminal in the `backend` directory.

### Compile the Backend

```powershell
javac --add-modules jdk.httpserver -cp "lib\*" -d out src\Main.java
```

### Run the Backend

```powershell
java --add-modules jdk.httpserver -cp "out;lib\*" Main
```

6. Open `index.html` in a browser to access the frontend.

The backend runs at `http://localhost:8080` by default.

**Note:** Database credentials must be configured locally. Never publish passwords or other secrets in the repository.

## Project Goal

MediBook aims to simplify doctor discovery, hospital exploration, and appointment management through a web application backed by a relational database.

## Author

Ruchira Paul
