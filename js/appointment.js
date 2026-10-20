
document.addEventListener("DOMContentLoaded", async function () {
    const form = document.getElementById("appointmentForm");
    const doctorSelect = document.getElementById("doctor");
    const doctorName = document.getElementById("selectedDoctorName");
    const doctorSpecialization = document.getElementById(
        "selectedDoctorSpecialization"
    );
    const dateInput = document.getElementById("date");
    const timeInput = document.getElementById("time");
    const emailInput = document.getElementById("email");
    const patientNameInput = document.getElementById("patientName");

    if (!form || !doctorSelect) {
        console.error("Appointment form or doctor dropdown not found.");
        return;
    }

    // Require a logged-in patient.
    const loggedIn =
        localStorage.getItem("medibookLoggedIn") === "true" &&
        localStorage.getItem("medibookRole")?.toLowerCase() === "patient";

    if (!loggedIn) {
        alert("Please log in as a patient before booking an appointment.");
        window.location.href = "login.html";
        return;
    }

    // Get the logged-in patient's details.
    const patientEmail = localStorage.getItem("medibookEmail");
    const savedName = localStorage.getItem("medibookUserName");

    if (!patientEmail) {
        alert("Your login email could not be found. Please log in again.");
        window.location.href = "login.html";
        return;
    }

    // Automatically fill in the patient's details.
    if (emailInput) {
        emailInput.value = patientEmail;
        emailInput.readOnly = true;
    }

    if (patientNameInput && savedName) {
        patientNameInput.value = savedName;
    }

    // Prevent selecting a past date.
    if (dateInput) {
        const localToday = new Date();
        const year = localToday.getFullYear();
        const month = String(localToday.getMonth() + 1).padStart(2, "0");
        const day = String(localToday.getDate()).padStart(2, "0");

        dateInput.min = `${year}-${month}-${day}`;
    }

    let doctorsById = new Map();

    // Load real doctors from the Java backend.
    try {
        const response = await fetch("http://localhost:8080/api/doctors");

        if (!response.ok) {
            throw new Error("Could not load doctors.");
        }

        const doctors = await response.json();

        doctorsById = new Map(
            doctors.map(doctor => [String(doctor.id), doctor])
        );

        doctorSelect.innerHTML =
            '<option value="">Select a Doctor</option>';

        doctors.forEach(doctor => {
            const option = document.createElement("option");

            option.value = String(doctor.id);
            option.textContent =
                `${doctor.name} — ${doctor.specialization}`;

            if (!doctor.available) {
                option.disabled = true;
                option.textContent += " (Unavailable)";
            }

            doctorSelect.appendChild(option);
        });

        // Restore the doctor selected on the doctors page.
        const savedText = localStorage.getItem("medibookSelectedDoctor");

        if (savedText) {
            try {
                const savedDoctor = JSON.parse(savedText);

                const matchingDoctor = doctors.find(doctor =>
                    String(doctor.id) === String(savedDoctor.id)
                );

                if (matchingDoctor && matchingDoctor.available) {
                    doctorSelect.value = String(matchingDoctor.id);
                } else {
                    const nameMatch = doctors.find(doctor =>
                        doctor.name === savedDoctor.name &&
                        doctor.available
                    );

                    if (nameMatch) {
                        doctorSelect.value = String(nameMatch.id);
                    }
                }
            } catch (error) {
                console.error(
                    "Could not restore selected doctor:",
                    error
                );
            }
        }

        updateDoctorCard();

    } catch (error) {
        console.error("Doctor loading error:", error);

        alert(
            "Could not load doctors. Please check that the MediBook Java " +
            "backend is running on port 8080, then reload this page."
        );
    }

    function updateDoctorCard() {
        const doctor = doctorsById.get(doctorSelect.value);

        if (!doctor) {
            if (doctorName) {
                doctorName.textContent = "Select a Doctor";
            }

            if (doctorSpecialization) {
                doctorSpecialization.textContent =
                    "Choose a doctor for your appointment";
            }

            return;
        }

        if (doctorName) {
            doctorName.textContent = doctor.name;
        }

        if (doctorSpecialization) {
            doctorSpecialization.textContent = doctor.specialization;
        }
    }

    doctorSelect.addEventListener("change", updateDoctorCard);

    // Submit the appointment to Java and save it in MySQL.
    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        const doctorId = doctorSelect.value;
        const appointmentDate = dateInput?.value || "";
        const appointmentTime = timeInput?.value || "";
        const reasonInput = document.getElementById("reason");
        const phoneInput = document.getElementById("phone");

        const reason = reasonInput ? reasonInput.value.trim() : "";
        const phone = phoneInput ? phoneInput.value.trim() : "";

        const email = emailInput ? emailInput.value.trim() : "";
        const patientName = patientNameInput
            ? patientNameInput.value.trim()
            : "";

        const modeInput =
            document.querySelector('input[name="mode"]:checked');

        if (!doctorId || !doctorsById.has(doctorId)) {
            alert("Please select a valid doctor.");
            return;
        }

        if (!doctorsById.get(doctorId).available) {
            alert("This doctor is currently unavailable.");
            return;
        }

        if (!patientName) {
            alert("Please enter your name.");
            return;
        }

        if (!/^[0-9]{10}$/.test(phone)) {
            alert("Please enter a valid 10-digit phone number.");
            return;
        }

        if (!appointmentDate || !appointmentTime) {
            alert("Please select an appointment date and time.");
            return;
        }

        if (!modeInput) {
            alert("Please select a consultation mode.");
            return;
        }

        if (reason.length > 500) {
            alert("Reason for visit must be 500 characters or fewer.");
            return;
        }

        if (!reason) {
            alert("Please enter a reason for your visit.");
            return;
        }

        if (email.toLowerCase() !== patientEmail.toLowerCase()) {
            alert(
                "Please use the email address of your logged-in patient account."
            );
            return;
        }

        const submitButton = form.querySelector(
            'button[type="submit"]'
        );

        if (submitButton) {
            submitButton.disabled = true;
        }

        try {
            const body = new URLSearchParams({
                patientEmail: patientEmail,
                doctorId: doctorId,
                appointmentDate: appointmentDate,
                appointmentTime: convertTo24Hour(appointmentTime),
                reason: reason
            });

            const response = await fetch(
                "http://localhost:8080/api/appointments",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/x-www-form-urlencoded;charset=UTF-8"
                    },
                    body: body.toString()
                }
            );

            const message = await response.text();

            if (!response.ok) {
                alert(message || "Could not book the appointment.");
                return;
            }

            alert(
                message +
                "\n\nPatient: " + patientName +
                "\nEmail: " + patientEmail +
                "\nDoctor: " + doctorsById.get(doctorId).name +
                "\nDate: " + appointmentDate +
                "\nTime: " + appointmentTime +
                "\nMode: " + modeInput.value +
                "\n\nYour appointment was saved successfully."
            );

            localStorage.removeItem("medibookPendingBooking");
            localStorage.removeItem("medibookSelectedDoctor");

            form.reset();

            // Restore patient details after resetting the form.
            if (emailInput) {
                emailInput.value = patientEmail;
                emailInput.readOnly = true;
            }

            if (patientNameInput && savedName) {
                patientNameInput.value = savedName;
            }

            updateDoctorCard();

        } catch (error) {
            console.error("Appointment booking error:", error);

            alert(
                "Could not contact the MediBook server. " +
                "Make sure the Java backend is running."
            );

        } finally {
            if (submitButton) {
                submitButton.disabled = false;
            }
        }
    });

    // Convert times such as "02:00 PM" to "14:00".
    // Already formatted 24-hour values remain unchanged.
    function convertTo24Hour(timeText) {
        const match = timeText.match(
            /^(\d{1,2}):(\d{2})\s*(AM|PM)$/i
        );

        if (!match) {
            return timeText;
        }

        let hours = Number(match[1]);
        const minutes = match[2];
        const period = match[3].toUpperCase();

        if (period === "PM" && hours !== 12) {
            hours += 12;
        }

        if (period === "AM" && hours === 12) {
            hours = 0;
        }

        return `${String(hours).padStart(2, "0")}:${minutes}`;
    }
});

