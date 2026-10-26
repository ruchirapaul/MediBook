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
            server.createContext("/api/hospitals/search", Main::handleHospitalSearch);
            server.createContext("/api/hospital-doctors", Main::handleHospitalDoctors);
            server.createContext("/api/appointments",
                    Main::handleAppointments);
                    server.createContext("/api/doctor/appointments",
                        Main::handleDoctorAppointments);

            server.createContext("/api/doctor/appointment-status",
                         Main::handleDoctorAppointmentStatus);
                         server.createContext("/api/doctor/profile",
        Main::handleDoctorProfile);

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

private static void handleDoctors(HttpExchange exchange)
        throws IOException {

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

    String state = null;
    String rawQuery = exchange.getRequestURI().getRawQuery();

    if (rawQuery != null) {
        for (String parameter : rawQuery.split("&")) {
            String[] pair = parameter.split("=", 2);

            if (pair.length == 2 && pair[0].equals("state")) {
                state = URLDecoder.decode(
                        pair[1], StandardCharsets.UTF_8).trim();
                break;
            }
        }
    }

    boolean filterByState = state != null && !state.isEmpty();

    String query =
            "SELECT id, name, specialization, qualification, "
            + "experience, available, state FROM doctors "
            + (filterByState
                ? "WHERE LOWER(TRIM(state)) = LOWER(TRIM(?)) "
                : "")
            + "ORDER BY name";

    try (Connection connection =
                 DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement statement =
                 connection.prepareStatement(query)) {

        if (filterByState) {
            statement.setString(1, state);
        }

        try (ResultSet rs = statement.executeQuery()) {
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
                    .append(rs.getBoolean("available")).append(",")
                    .append("\"state\":\"")
                    .append(escapeJson(rs.getString("state")))
                    .append("\"")
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
   
    // CREATE AND GET APPOINTMENTS
    private static void handleAppointments(
            HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String method = exchange.getRequestMethod();

        // GET ALL APPOINTMENTS FOR ADMIN DASHBOARD
        if ("GET".equalsIgnoreCase(method)) {

            String sql =
                    "SELECT a.id, u.name AS patient_name, "
                    + "u.email AS patient_email, d.name AS doctor_name, "
                    + "a.appointment_date, a.appointment_time, "
                    + "a.reason, a.status "
                    + "FROM appointments a "
                    + "JOIN users u ON a.user_id = u.id "
                    + "JOIN doctors d ON a.doctor_id = d.id "
                    + "ORDER BY a.appointment_date DESC, "
                    + "a.appointment_time DESC";

            try (Connection connection =
                         DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement statement =
                         connection.prepareStatement(sql);
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
                        .append("\"patientName\":\"")
                        .append(escapeJson(rs.getString("patient_name"))).append("\",")
                        .append("\"patientEmail\":\"")
                        .append(escapeJson(rs.getString("patient_email"))).append("\",")
                        .append("\"doctorName\":\"")
                        .append(escapeJson(rs.getString("doctor_name"))).append("\",")
                        .append("\"date\":\"")
                        .append(rs.getDate("appointment_date")).append("\",")
                        .append("\"time\":\"")
                        .append(rs.getTime("appointment_time")).append("\",")
                        .append("\"reason\":\"")
                        .append(escapeJson(rs.getString("reason"))).append("\",")
                        .append("\"status\":\"")
                        .append(escapeJson(rs.getString("status"))).append("\"")
                        .append("}");
                }

                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());

            } catch (SQLException e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500,
                        "{\"error\":\"Could not retrieve appointments.\"}");
            }

            return;
        }

        // KEEP PATIENT APPOINTMENT BOOKING WORKING
        if (!"POST".equalsIgnoreCase(method)) {
            sendJsonResponse(exchange, 405,
                    "{\"error\":\"GET or POST method required.\"}");
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

                // Find patient account
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

                // Save appointment
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

            } catch (SQLException e) {
                e.printStackTrace();
                sendResponse(exchange, 500,
                        "Database error while booking appointment.");
            }

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
    
   
    // SEARCH HOSPITALS FROM MYSQL
    private static void handleHospitalSearch(HttpExchange exchange)
            throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405,
                    "{\"error\":\"Method not allowed\"}");
            return;
        }

        String query = "";
        String rawQuery = exchange.getRequestURI().getRawQuery();

        if (rawQuery != null) {
            for (String parameter : rawQuery.split("&")) {
                String[] pair = parameter.split("=", 2);

                if (pair.length == 2 && pair[0].equals("q")) {
                    query = URLDecoder.decode(
                            pair[1], StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        if (query.trim().isEmpty()) {
            sendJsonResponse(exchange, 200, "[]");
            return;
        }

        String sql =
                "SELECT Sr_No AS id, "
                + "Hospital_Name AS name, "
                + "Address_Original_First_Line AS address, "
                + "Location AS city, "
                + "State AS state, "
                + "District AS district, "
                + "Pincode AS pincode, "
                + "CASE "
                + "WHEN NULLIF(TRIM(Telephone), '') IS NOT NULL "
                + "AND TRIM(Telephone) <> '0' THEN TRIM(Telephone) "
                + "WHEN NULLIF(TRIM(Mobile_Number), '') IS NOT NULL "
                + "AND TRIM(Mobile_Number) <> '0' THEN TRIM(Mobile_Number) "
                + "ELSE '' END AS phone "
                + "FROM hospital_directory "
                + "WHERE Hospital_Name LIKE ? "
                + "OR Location LIKE ? "
                + "OR State LIKE ? "
                + "OR District LIKE ? "
                + "OR Address_Original_First_Line LIKE ? "
                + "OR Pincode LIKE ? "
                + "ORDER BY Hospital_Name LIMIT 50";

        try (Connection con =
                     DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = con.prepareStatement(sql)) {

            String search = "%" + query.trim() + "%";

            for (int i = 1; i <= 6; i++) {
                ps.setString(i, search);
            }

            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                while (rs.next()) {
                    if (!first) {
                        json.append(",");
                    }

                    String name = rs.getString("name");
                    String address = rs.getString("address");
                    String city = rs.getString("city");
                    String state = rs.getString("state");
                    String district = rs.getString("district");
                    String pincode = rs.getString("pincode");
                    String phone = rs.getString("phone");

                    String mapSearch =
                            String.join(" ",
                                    safeText(name),
                                    safeText(address),
                                    safeText(city),
                                    safeText(district),
                                    safeText(state),
                                    safeText(pincode)).trim();

                    String mapUrl =
                            "https://www.google.com/maps/search/?api=1&query="
                            + java.net.URLEncoder.encode(
                                    mapSearch, StandardCharsets.UTF_8);

                    json.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"name\":\"")
                            .append(escapeJson(name)).append("\",")
                            .append("\"address\":\"")
                            .append(escapeJson(address)).append("\",")
                            .append("\"city\":\"")
                            .append(escapeJson(city)).append("\",")
                            .append("\"state\":\"")
                            .append(escapeJson(state)).append("\",")
                            .append("\"district\":\"")
                            .append(escapeJson(district)).append("\",")
                            .append("\"pincode\":\"")
                            .append(escapeJson(pincode)).append("\",")
                            .append("\"phone\":\"")
                            .append(escapeJson(phone)).append("\",")
                            .append("\"map_url\":\"")
                            .append(escapeJson(mapUrl)).append("\"")
                            .append("}");

                    first = false;
                }

                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500,
                    "{\"error\":\"Hospital search failed\"}");
        }
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    // GET DOCTORS AVAILABLE AT A HOSPITAL
private static void handleHospitalDoctors(HttpExchange exchange)
        throws IOException {

    addCorsHeaders(exchange);

    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1);
        return;
    }

    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendResponse(exchange, 405,
                "{\"error\":\"Method not allowed\"}");
        return;
    }

    String query = exchange.getRequestURI().getRawQuery();
    int hospitalId = -1;

    if (query != null) {
        for (String parameter : query.split("&")) {
            String[] pair = parameter.split("=", 2);

            if (pair.length == 2 && pair[0].equals("hospitalId")) {
                try {
                    hospitalId = Integer.parseInt(pair[1]);
                } catch (NumberFormatException e) {
                    hospitalId = -1;
                }
            }
        }
    }

    if (hospitalId < 1) {
        sendResponse(exchange, 400,
                "{\"error\":\"Valid hospitalId is required\"}");
        return;
    }

    String sql = "SELECT d.id, d.name, d.specialization, "
            + "d.qualification, d.experience, "
            + "hd.available_days, hd.start_time, hd.end_time, "
            + "hd.consultation_fee "
            + "FROM hospital_doctors hd "
            + "JOIN doctors d ON hd.doctor_id = d.id "
            + "WHERE hd.hospital_id = ? "
            + "ORDER BY d.name";

    try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, hospitalId);

        try (ResultSet rs = ps.executeQuery()) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"name\":\"")
                    .append(escapeJson(rs.getString("name"))).append("\",")
                    .append("\"specialization\":\"")
                    .append(escapeJson(rs.getString("specialization"))).append("\",")
                    .append("\"qualification\":\"")
                    .append(escapeJson(rs.getString("qualification"))).append("\",")
                    .append("\"experience\":").append(rs.getInt("experience")).append(",")
                    .append("\"available_days\":\"")
                    .append(escapeJson(rs.getString("available_days"))).append("\",")
                    .append("\"start_time\":\"")
                    .append(escapeJson(String.valueOf(rs.getTime("start_time")))).append("\",")
                    .append("\"end_time\":\"")
                    .append(escapeJson(String.valueOf(rs.getTime("end_time")))).append("\",")
                    .append("\"consultation_fee\":")
                    .append(rs.getInt("consultation_fee"))
                    .append("}");

                first = false;
            }

            json.append("]");
            sendResponse(exchange, 200, json.toString());
        }

    } catch (SQLException e) {
        e.printStackTrace();
        sendResponse(exchange, 500,
                "{\"error\":\"Unable to load hospital doctors\"}");
    }
}
// GET APPOINTMENTS FOR A DOCTOR
private static void handleDoctorAppointments(HttpExchange exchange)
        throws IOException {

    addCorsHeaders(exchange);

    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return;
    }

    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJsonResponse(exchange, 405,
                "{\"error\":\"GET method required\"}");
        return;
    }

    String email = "";
    String query = exchange.getRequestURI().getRawQuery();

    if (query != null) {
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals("email")) {
                email = URLDecoder.decode(
                        pair[1], StandardCharsets.UTF_8);
            }
        }
    }

    if (email.trim().isEmpty()) {
        sendJsonResponse(exchange, 400,
                "{\"error\":\"Doctor email is required\"}");
        return;
    }

    String sql =
            "SELECT a.id, u.name AS patient_name, "
            + "u.email AS patient_email, a.appointment_date, "
            + "a.appointment_time, a.reason, a.status "
            + "FROM appointments a "
            + "JOIN users u ON a.user_id = u.id "
            + "JOIN doctors d ON a.doctor_id = d.id "
            + "WHERE d.email = ? "
            + "ORDER BY a.appointment_date DESC, a.appointment_time DESC";

    try (Connection con =
                 DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, email.trim());

        try (ResultSet rs = ps.executeQuery()) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"patientName\":\"")
                    .append(escapeJson(rs.getString("patient_name"))).append("\",")
                    .append("\"patientEmail\":\"")
                    .append(escapeJson(rs.getString("patient_email"))).append("\",")
                    .append("\"date\":\"")
                    .append(rs.getDate("appointment_date")).append("\",")
                    .append("\"time\":\"")
                    .append(rs.getTime("appointment_time")).append("\",")
                    .append("\"reason\":\"")
                    .append(escapeJson(rs.getString("reason"))).append("\",")
                    .append("\"status\":\"")
                    .append(escapeJson(rs.getString("status"))).append("\"")
                    .append("}");

                first = false;
            }

            json.append("]");
            sendJsonResponse(exchange, 200, json.toString());
        }

    } catch (SQLException e) {
        e.printStackTrace();
        sendJsonResponse(exchange, 500,
                "{\"error\":\"Unable to load appointments\"}");
    }
}


// ACCEPT OR REJECT A DOCTOR'S APPOINTMENT
private static void handleDoctorAppointmentStatus(HttpExchange exchange)
        throws IOException {

    addCorsHeaders(exchange);

    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return;
    }

    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJsonResponse(exchange, 405,
                "{\"error\":\"POST method required\"}");
        return;
    }

    Map<String, String> data =
            parseFormData(readRequestBody(exchange));

    String email = data.get("email");
    String appointmentIdText = data.get("appointmentId");
    String status = data.get("status");

    if (isEmpty(email) || isEmpty(appointmentIdText)
            || isEmpty(status)) {
        sendJsonResponse(exchange, 400,
                "{\"error\":\"Email, appointment ID and status are required\"}");
        return;
    }

    if (!status.equals("Accepted") && !status.equals("Rejected")) {
        sendJsonResponse(exchange, 400,
                "{\"error\":\"Status must be Accepted or Rejected\"}");
        return;
    }

    int appointmentId;

    try {
        appointmentId = Integer.parseInt(appointmentIdText);
        if (appointmentId <= 0) {
            throw new NumberFormatException();
        }
    } catch (NumberFormatException e) {
        sendJsonResponse(exchange, 400,
                "{\"error\":\"Invalid appointment ID\"}");
        return;
    }

    String sql =
            "UPDATE appointments a "
            + "JOIN doctors d ON a.doctor_id = d.id "
            + "SET a.status = ? "
            + "WHERE a.id = ? AND d.email = ? "
            + "AND a.status = 'Pending'";

    try (Connection con =
                 DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, status);
        ps.setInt(2, appointmentId);
        ps.setString(3, email.trim());

        int updated = ps.executeUpdate();

        if (updated > 0) {
            sendJsonResponse(exchange, 200,
                    "{\"message\":\"Appointment status updated\","
                    + "\"status\":\"" + status + "\"}");
        } else {
            sendJsonResponse(exchange, 404,
                    "{\"error\":\"Pending appointment not found for this doctor\"}");
        }

    } catch (SQLException e) {
        e.printStackTrace();
        sendJsonResponse(exchange, 500,
                "{\"error\":\"Unable to update appointment status\"}");
    }
}

private static void handleDoctorProfile(HttpExchange exchange)
        throws IOException {

    addCorsHeaders(exchange);

    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return;
    }

    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJsonResponse(exchange, 405,
                "{\"error\":\"GET method required\"}");
        return;
    }

    String email = "";
    String rawQuery = exchange.getRequestURI().getRawQuery();

    if (rawQuery != null) {
        for (String part : rawQuery.split("&")) {
            String[] pair = part.split("=", 2);

            if (pair.length == 2 && pair[0].equals("email")) {
                email = URLDecoder.decode(
                        pair[1], StandardCharsets.UTF_8);
                break;
            }
        }
    }

    if (email.trim().isEmpty()) {
        sendJsonResponse(exchange, 400,
                "{\"error\":\"Doctor email is required\"}");
        return;
    }

    String sql = "SELECT id, name, email, specialization, "
            + "qualification, experience, available, state "
            + "FROM doctors WHERE LOWER(TRIM(email)) = LOWER(TRIM(?)) "
            + "LIMIT 1";

    try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, email.trim());

        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                sendJsonResponse(exchange, 404,
                        "{\"error\":\"Doctor profile not found. "
                        + "Check that the login email matches the doctors table.\"}");
                return;
            }

            String json = "{"
                    + "\"id\":" + rs.getInt("id") + ","
                    + "\"name\":\"" + escapeJson(rs.getString("name")) + "\","
                    + "\"email\":\"" + escapeJson(rs.getString("email")) + "\","
                    + "\"specialization\":\""
                    + escapeJson(rs.getString("specialization")) + "\","
                    + "\"qualification\":\""
                    + escapeJson(rs.getString("qualification")) + "\","
                    + "\"experience\":" + rs.getInt("experience") + ","
                    + "\"available\":" + rs.getBoolean("available") + ","
                    + "\"state\":\"" + escapeJson(rs.getString("state")) + "\""
                    + "}";

            sendJsonResponse(exchange, 200, json);
        }

    } catch (SQLException e) {
        e.printStackTrace();
        sendJsonResponse(exchange, 500,
                "{\"error\":\"Unable to load doctor profile\"}");
    }
}

}