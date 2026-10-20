// ================================
// MEDIBOOK REGISTRATION JAVASCRIPT
// ================================

let selectedRole = "patient";

const API_URL = "http://localhost:8080";


// ================================
// ROLE SELECTION
// ================================

function selectRole(role) {

    selectedRole = role;

    document.getElementById("role").value = role;

    document.querySelectorAll(".role-option").forEach(function(button) {
        button.classList.remove("active");
    });

    if (role === "patient") {
        document.getElementById("patientRole").classList.add("active");
    }

    else if (role === "doctor") {
        document.getElementById("doctorRole").classList.add("active");
    }

    else if (role === "admin") {
        document.getElementById("adminRole").classList.add("active");
    }
}


// ================================
// PASSWORD VISIBILITY
// ================================

function togglePassword(inputId, iconId) {

    const passwordInput = document.getElementById(inputId);
    const icon = document.getElementById(iconId);

    if (passwordInput.type === "password") {

        passwordInput.type = "text";

        icon.classList.remove("fa-eye");
        icon.classList.add("fa-eye-slash");

    } else {

        passwordInput.type = "password";

        icon.classList.remove("fa-eye-slash");
        icon.classList.add("fa-eye");
    }
}


// ================================
// PASSWORD STRENGTH
// ================================

const passwordInput = document.getElementById("password");

passwordInput.addEventListener("input", function() {

    const password = passwordInput.value;

    const strengthBar = document.getElementById("strengthBar");
    const strengthText = document.getElementById("strengthText");

    let strength = 0;

    if (password.length >= 6) {
        strength++;
    }

    if (/[A-Z]/.test(password)) {
        strength++;
    }

    if (/[0-9]/.test(password)) {
        strength++;
    }

    if (/[^A-Za-z0-9]/.test(password)) {
        strength++;
    }

    if (password.length === 0) {

        strengthBar.style.width = "0%";
        strengthText.textContent = "Password strength";

    }

    else if (strength === 1) {

        strengthBar.style.width = "25%";
        strengthText.textContent = "Weak";

    }

    else if (strength === 2) {

        strengthBar.style.width = "50%";
        strengthText.textContent = "Fair";

    }

    else if (strength === 3) {

        strengthBar.style.width = "75%";
        strengthText.textContent = "Good";

    }

    else {

        strengthBar.style.width = "100%";
        strengthText.textContent = "Strong";
    }
});


// ================================
// SHOW MESSAGE
// ================================

function showMessage(message, type) {

    const messageBox = document.getElementById("message");

    messageBox.textContent = message;

    messageBox.className = "message " + type;
}


// ================================
// REGISTRATION
// ================================

async function handleRegister(event) {

    event.preventDefault();

    const fullName =
        document.getElementById("fullName").value.trim();

    const email =
        document.getElementById("email").value.trim();

    const phone =
        document.getElementById("phone").value.trim();

    const dob =
        document.getElementById("dob").value;

    const gender =
        document.getElementById("gender").value;

    const password =
        document.getElementById("password").value;

    const confirmPassword =
        document.getElementById("confirmPassword").value;

    const role =
        document.getElementById("role").value;

    const terms =
        document.getElementById("terms").checked;


    // ================================
    // FRONTEND VALIDATION
    // ================================

    if (fullName.length < 3) {

        showMessage(
            "Please enter a valid full name.",
            "error"
        );

        return;
    }


    const phonePattern = /^[0-9]{10}$/;

    if (!phonePattern.test(phone)) {

        showMessage(
            "Please enter a valid 10-digit phone number.",
            "error"
        );

        return;
    }


    if (!dob) {

        showMessage(
            "Please select your date of birth.",
            "error"
        );

        return;
    }


    if (!gender) {

        showMessage(
            "Please select your gender.",
            "error"
        );

        return;
    }


    if (password.length < 6) {

        showMessage(
            "Password must contain at least 6 characters.",
            "error"
        );

        return;
    }


    if (password !== confirmPassword) {

        showMessage(
            "Passwords do not match.",
            "error"
        );

        return;
    }


    if (!terms) {

        showMessage(
            "Please accept the Terms & Conditions.",
            "error"
        );

        return;
    }


    // ================================
    // SEND DATA TO JAVA BACKEND
    // ================================

    const registerButton =
        document.querySelector(".register-btn");

    registerButton.disabled = true;

    registerButton.querySelector("span").textContent =
        "Creating Account...";


    try {

        const formData = new URLSearchParams();

        formData.append("fullName", fullName);
        formData.append("email", email);
        formData.append("phone", phone);
        formData.append("dob", dob);
        formData.append("gender", gender);
        formData.append("role", role);
        formData.append("password", password);


        const response = await fetch(
            `${API_URL}/api/register`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body: formData.toString()
            }
        );


        const result = await response.text();


        // ================================
        // BACKEND RESPONSE
        // ================================

        if (response.ok) {

            showMessage(
                result,
                "success"
            );

            document.getElementById("registerForm").reset();

            selectedRole = "patient";

            document.getElementById("role").value =
                "patient";

            document.querySelectorAll(".role-option")
                .forEach(function(button) {
                    button.classList.remove("active");
                });

            document.getElementById("patientRole")
                .classList.add("active");

            document.getElementById("strengthBar")
                .style.width = "0%";

            document.getElementById("strengthText")
                .textContent = "Password strength";

        } else {

            showMessage(
                result,
                "error"
            );
        }


    } catch (error) {

        console.error(
            "Registration Error:",
            error
        );

        showMessage(
            "Could not connect to MediBook server. Make sure the Java backend is running.",
            "error"
        );

    } finally {

        registerButton.disabled = false;

        registerButton.querySelector("span").textContent =
            "Create Account";
    }
}


// ================================
// PHONE INPUT
// ================================

document.getElementById("phone").addEventListener(
    "input",
    function() {

        this.value =
            this.value.replace(/\D/g, "");

    }
);