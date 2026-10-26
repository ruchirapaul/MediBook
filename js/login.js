
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

    const email = document.getElementById("email")?.value.trim();
    const password = document.getElementById("password")?.value;
    const role = document.getElementById("selectedRole")?.value;

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
            body: new URLSearchParams({ email, password, role })
        });

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
            alert("Login response is incomplete. Please try again.");
            console.error("Login response:", result);
            return;
        }

        const actualRole = result.role.toLowerCase();

        if (actualRole !== role.toLowerCase()) {
            alert("The selected role does not match your account role.");
            return;
        }

        localStorage.setItem("medibookLoggedIn", "true");
        localStorage.setItem("medibookEmail", result.email);
        localStorage.setItem("medibookRole", actualRole);
        localStorage.setItem("medibookUserName", result.name);

        // Compatibility with the doctor dashboard.
        localStorage.setItem("user", JSON.stringify({
            name: result.name,
            email: result.email,
            role: actualRole
        }));

        alert(result.message || `Login successful! Welcome, ${result.name}.`);

        const pendingBooking =
            localStorage.getItem("medibookPendingBooking") === "true";

        if (actualRole === "doctor") {
            window.location.href = "doctor-dashboard.html";
        } else if (actualRole === "admin") {
            window.location.href = "admin-dashboard.html";
        } else if (pendingBooking) {
            window.location.href = "doctors.html?continueBooking=true";
        } else {
            window.location.href = "index.html";
        }

    } catch (error) {
        console.error("Login error:", error);
        alert(
            "Unable to connect to MediBook. " +
            "Please make sure the Java backend is running on port 8080."
        );
    }
}
