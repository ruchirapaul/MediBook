
"use strict";

const API_URL = "http://localhost:8080";

function selectRole(role) {
    const selectedRoleInput = document.getElementById("selectedRole");

    if (selectedRoleInput) {
        selectedRoleInput.value = role;
    }

    document.querySelectorAll(".role-btn").forEach(button => {
        button.classList.remove("active");
    });

    // Use the clicked button when the browser provides the event.
    if (typeof event !== "undefined" && event.currentTarget) {
        event.currentTarget.classList.add("active");
    }
}

function togglePassword() {
    const passwordInput = document.getElementById("password");

    if (!passwordInput) return;

    passwordInput.type =
        passwordInput.type === "password" ? "text" : "password";
}

async function handleLogin(event) {
    event.preventDefault();

    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const selectedRoleInput = document.getElementById("selectedRole");

    if (!emailInput || !passwordInput || !selectedRoleInput) {
        alert("Login form fields could not be found. Please check login.html.");
        return;
    }

    const email = emailInput.value.trim();
    const password = passwordInput.value;
    const role = selectedRoleInput.value;

    if (!email || !password || !role) {
        alert("Please enter your email, password and select your role.");
        return;
    }

    try {
        const response = await fetch(`${API_URL}/api/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                "Accept": "application/json"
            },
            body: new URLSearchParams({
                email: email,
                password: password,
                role: role
            })
        });

        // Support JSON success responses and plain-text error responses.
        const responseText = await response.text();
        let result;

        try {
            result = JSON.parse(responseText);
        } catch {
            result = { message: responseText };
        }

        if (!response.ok) {
            alert(result.message || "Login failed. Please try again.");
            return;
        }

        if (!result.name || !result.email || !result.role) {
            console.error("Unexpected login response:", result);
            alert("Login response is incomplete. Please try again.");
            return;
        }

        // Save the actual name retrieved from the MySQL users table.
        localStorage.setItem("medibookLoggedIn", "true");
        localStorage.setItem("medibookEmail", result.email);
        localStorage.setItem("medibookRole", result.role);
        localStorage.setItem("medibookUserName", result.name);

        alert(result.message || `Login successful! Welcome, ${result.name}.`);

        const pendingBooking =
            localStorage.getItem("medibookPendingBooking") === "true";

        if (
            pendingBooking &&
            result.role.toLowerCase() === "patient"
        ) {
            window.location.href = "doctors.html?continueBooking=true";
        } else {
            window.location.href = "doctors.html";
        }

    } catch (error) {
        console.error("Login error:", error);

        alert(
            "Unable to connect to MediBook. " +
            "Please make sure the Java backend is running on port 8080."
        );
    }
}

