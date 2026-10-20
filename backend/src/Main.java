import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final String URL =
            "jdbc:mysql://localhost:3306/medibook";
    private static final String USER = "root";

    // Replace with the MySQL password from your working file.
   private static final String PASSWORD =
                     System.getenv("MEDIBOOK_DB_PASSWORD");

    public static void main(String[] args) {
        System.out.println("MediBook Backend");

        try {
            try (Connection connection =
                         DriverManager.getConnection(URL, USER, PASSWORD)) {
                System.out.println("MySQL Connected Successfully!");
            }

            HttpServer server = HttpServer.create(
                    new InetSocketAddress(8080), 0);

            server.createContext("/api/register", Main::handleRegister);
            server.createContext("/api/login", Main::handleLogin);
            server.createContext("/api/doctors", Main::handleDoctors);
            server.createContext("/api/appointments",
                    Main::handleAppointments);

            server.start();
            System.out.println("MediBook API Server Started!");
            System.out.println("http://localhost:8080");

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    // REGISTER
    private static void handleRegister(
            HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Only POST method is allowed.");
            return;
        }

        try {
            Map<String, String> data =
                    parseFormData(readRequestBody(exchange));

            String fullName = data.get("fullName");
            String email = data.get("email");
            String phone = data.get("phone");
            String dob = data.get("dob");
            String gender = data.get("gender");
            String role = data.get("role");
            String password = data.get("password");

            if (isEmpty(fullName) || isEmpty(email)
                    || isEmpty(phone) || isEmpty(dob)
                    || isEmpty(gender) || isEmpty(role)
                    || isEmpty(password)) {
                sendResponse(exchange, 400,
                        "Please fill all required fields.");
                return;
            }

            String hashedPassword = hashPassword(password);

            try (Connection connection =
                         DriverManager.getConnection(URL, USER, PASSWORD)) {

                String checkQuery =
                        "SELECT id FROM users WHERE email = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(checkQuery)) {
                    statement.setString(1, email.trim());

                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            sendResponse(exchange, 409,
                                    "An account with this email already exists.");
                            return;
                        }
                    }
                }

                String insertQuery =
                        "INSERT INTO users "
                        + "(name, email, password, phone, dob, gender, role) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement statement =
                             connection.prepareStatement(insertQuery)) {
                    statement.setString(1, fullName.trim());
                    statement.setString(2, email.trim());
                    statement.setString(3, hashedPassword);
                    statement.setString(4, phone.trim());
                    statement.setString(5, dob);
                    statement.setString(6, gender);
                    statement.setString(7, role.trim());

                    int rows = statement.executeUpdate();

                    if (rows > 0) {
                        sendResponse(exchange, 201,
                                "Registration successful!");
                    } else {
                        sendResponse(exchange, 500,
                                "Registration failed.");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500,
                    "Server error during registration.");
        }
    }

    // LOGIN
    private static void handleLogin(
            HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Only POST method is allowed.");
            return;
        }

        try {
            Map<String, String> data =
                    parseFormData(readRequestBody(exchange));

            String email = data.get("email");
            String password = data.get("password");
            String role = data.get("role");

            if (isEmpty(email) || isEmpty(password) || isEmpty(role)) {
                sendResponse(exchange, 400,
                        "Please enter email, password and role.");
                return;
            }

            String hashedPassword = hashPassword(password);

            try (Connection connection =
                         DriverManager.getConnection(URL, USER, PASSWORD)) {

                String query =
                        "SELECT id, name, email, password, role "
                        + "FROM users WHERE email = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(query)) {
                    statement.setString(1, email.trim());

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            sendResponse(exchange, 401,
                                    "No account found with this email.");
                            return;
                        }

                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        String userEmail = rs.getString("email");
                        String storedPassword = rs.getString("password");
                        String storedRole = rs.getString("role");

                        if (storedPassword == null
                                || !storedPassword.equals(hashedPassword)) {
                            sendResponse(exchange, 401,
                                    "Password does not match the registered account.");
                            return;
                        }

                        if (storedRole == null
                                || !storedRole.equalsIgnoreCase(role.trim())) {
                            sendResponse(exchange, 401,
                                    "The selected role does not match this account.");
                            return;
                        }

                        System.out.println("User logged in: ID=" + id
                                + ", Name=" + name);

                        String json =
                                "{"
                                + "\"message\":\"Login successful!\","
                                + "\"id\":" + id + ","
                                + "\"name\":\"" + escapeJson(name) + "\","
                                + "\"email\":\"" + escapeJson(userEmail) + "\","
                                + "\"role\":\"" + escapeJson(storedRole) + "\""
                                + "}";

                        sendJsonResponse(exchange, 200, json);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500,
                    "Server error while logging in.");
        }
    }

    // GET DOCTORS
    private static void handleDoctors(
            HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Only GET method is allowed.");
            return;
        }

        try (Connection connection =
                     DriverManager.getConnection(URL, USER, PASSWORD)) {

            String query =
                    "SELECT id, name, specialization, qualification, "
                    + "experience, available FROM doctors";

            try (PreparedStatement statement =
                         connection.prepareStatement(query);
                 ResultSet rs = statement.executeQuery()) {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                while (rs.next()) {
                    if (!first) {
                        json.append(",");
                    }
                    first = false;

                    json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"name\":\"")
                        .append(escapeJson(rs.getString("name"))).append("\",")
                        .append("\"specialization\":\"")
                        .append(escapeJson(rs.getString("specialization")))
                        .append("\",")
                        .append("\"qualification\":\"")
                        .append(escapeJson(rs.getString("qualification")))
                        .append("\",")
                        .append("\"experience\":")
                        .append(rs.getInt("experience")).append(",")
                        .append("\"available\":")
                        .append(rs.getBoolean("available"))
                        .append("}");
                }

                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Could not retrieve doctors.");
        }
    }

    // CREATE APPOINTMENT
    private static void handleAppointments(
            HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Only POST method is allowed.");
            return;
        }

        try {
            Map<String, String> data =
                    parseFormData(readRequestBody(exchange));

            String patientEmail = data.get("patientEmail");
            String doctorIdText = data.get("doctorId");
            String appointmentDate = data.get("appointmentDate");
            String appointmentTime = data.get("appointmentTime");
            String reason = data.get("reason");

            if (isEmpty(patientEmail) || isEmpty(doctorIdText)
                    || isEmpty(appointmentDate)
                    || isEmpty(appointmentTime) || isEmpty(reason)) {
                sendResponse(exchange, 400,
                        "Please fill in all appointment fields.");
                return;
            }

            if (reason.trim().length() > 255) {
                sendResponse(exchange, 400,
                        "Reason must be 255 characters or fewer.");
                return;
            }

            int doctorId;
            java.time.LocalDate date;
            java.time.LocalTime time;

            try {
                doctorId = Integer.parseInt(doctorIdText);
                date = java.time.LocalDate.parse(appointmentDate);
                time = java.time.LocalTime.parse(appointmentTime);

                if (doctorId <= 0) {
                    sendResponse(exchange, 400, "Invalid doctor ID.");
                    return;
                }
            } catch (Exception e) {
                sendResponse(exchange, 400,
                        "Invalid doctor ID, appointment date or time.");
                return;
            }

            java.time.LocalDate today = java.time.LocalDate.now();

            if (date.isBefore(today)
                    || (date.equals(today)
                    && !time.isAfter(java.time.LocalTime.now()))) {
                sendResponse(exchange, 400,
                        "Please choose a future date and time.");
                return;
            }

            try (Connection connection =
                         DriverManager.getConnection(URL, USER, PASSWORD)) {

                // Find patient ID
                int userId;

                String patientQuery =
                        "SELECT id FROM users "
                        + "WHERE email = ? AND role = 'patient'";

                try (PreparedStatement statement =
                             connection.prepareStatement(patientQuery)) {
                    statement.setString(1, patientEmail.trim());

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            sendResponse(exchange, 403,
                                    "Registered patient account not found.");
                            return;
                        }

                        userId = rs.getInt("id");
                    }
                }

                // Check doctor availability
                String doctorQuery =
                        "SELECT id FROM doctors "
                        + "WHERE id = ? AND available = TRUE";

                try (PreparedStatement statement =
                             connection.prepareStatement(doctorQuery)) {
                    statement.setInt(1, doctorId);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            sendResponse(exchange, 400,
                                    "Doctor not found or unavailable.");
                            return;
                        }
                    }
                }

                // Check for a conflicting booking
                String conflictQuery =
                        "SELECT id FROM appointments "
                        + "WHERE doctor_id = ? "
                        + "AND appointment_date = ? "
                        + "AND appointment_time = ? "
                        + "AND status <> 'Cancelled'";

                try (PreparedStatement statement =
                             connection.prepareStatement(conflictQuery)) {
                    statement.setInt(1, doctorId);
                    statement.setDate(2, java.sql.Date.valueOf(date));
                    statement.setTime(3, java.sql.Time.valueOf(time));

                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            sendResponse(exchange, 409,
                                    "This time slot is already booked.");
                            return;
                        }
                    }
                }

                // Insert appointment
                String insertQuery =
                        "INSERT INTO appointments "
                        + "(user_id, doctor_id, appointment_date, "
                        + "appointment_time, reason, status) "
                        + "VALUES (?, ?, ?, ?, ?, 'Pending')";

                try (PreparedStatement statement =
                             connection.prepareStatement(insertQuery)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, doctorId);
                    statement.setDate(3, java.sql.Date.valueOf(date));
                    statement.setTime(4, java.sql.Time.valueOf(time));
                    statement.setString(5, reason.trim());

                    int rows = statement.executeUpdate();

                    if (rows > 0) {
                        sendJsonResponse(exchange, 201,
                                "{\"message\":\"Appointment booked successfully!\","
                                + "\"status\":\"Pending\"}");
                    } else {
                        sendResponse(exchange, 500,
                                "Appointment could not be saved.");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500,
                    "Database error while booking appointment.");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500,
                    "Server error while booking appointment.");
        }
    }

    // READ REQUEST BODY
    private static String readRequestBody(
            HttpExchange exchange) throws IOException {

        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // PARSE FORM DATA
    private static Map<String, String> parseFormData(String body) {
        Map<String, String> data = new HashMap<>();

        if (body == null || body.isEmpty()) {
            return data;
        }

        for (String pair : body.split("&")) {
            String[] keyValue = pair.split("=", 2);

            String key = URLDecoder.decode(
                    keyValue[0], StandardCharsets.UTF_8);

            String value = keyValue.length == 2
                    ? URLDecoder.decode(
                            keyValue[1], StandardCharsets.UTF_8)
                    : "";

            data.put(key, value);
        }

        return data;
    }

    // PASSWORD HASHING
    private static String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(
                password.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {
            String value = Integer.toHexString(0xff & b);
            if (value.length() == 1) {
                hex.append('0');
            }
            hex.append(value);
        }

        return hex.toString();
    }

    // CHECK EMPTY
    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    // CORS
    private static void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    // TEXT RESPONSE
    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    // JSON RESPONSE
    private static void sendJsonResponse(
            HttpExchange exchange,
            int statusCode,
            String response) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    // ESCAPE JSON STRINGS
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}