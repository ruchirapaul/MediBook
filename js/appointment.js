document.addEventListener("DOMContentLoaded", async function () {
    const form = document.getElementById("appointmentForm");
    const doctorSelect = document.getElementById("doctor");
    const doctorName = document.getElementById("selectedDoctorName");
    const doctorSpecialization = document.getElementById("selectedDoctorSpecialization");
    const doctorExperience = document.getElementById("selectedDoctorExperience");
    const dateInput = document.getElementById("date");
    const timeInput = document.getElementById("time");
    const emailInput = document.getElementById("email");
    const patientNameInput = document.getElementById("patientName");

    if (!form || !doctorSelect) {
        console.error("Appointment form or doctor dropdown not found.");
        return;
    }

    const loggedIn =
        localStorage.getItem("medibookLoggedIn") === "true" &&
        localStorage.getItem("medibookRole")?.toLowerCase() === "patient";

    if (!loggedIn) {
        alert("Please log in as a patient before booking an appointment.");
        window.location.href = "login.html";
        return;
    }

    const patientEmail = localStorage.getItem("medibookEmail");
    const savedName =
        localStorage.getItem("medibookUserName") ||
        localStorage.getItem("medibookName") ||
        localStorage.getItem("username") ||
        "";

    if (!patientEmail) {
        alert("Your login email could not be found. Please log in again.");
        window.location.href = "login.html";
        return;
    }

    if (emailInput) {
        emailInput.value = patientEmail;
        emailInput.readOnly = true;
    }

    if (patientNameInput && savedName) {
        patientNameInput.value = savedName;
    }

    if (dateInput) {
        const today = new Date();
        dateInput.min =
            `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
    }

    let doctorsById = new Map();

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

        const savedText =
            localStorage.getItem("medibookSelectedDoctor");

        if (savedText) {
            try {
                const savedDoctor = JSON.parse(savedText);

                const match = doctors.find(doctor =>
                    String(doctor.id) === String(savedDoctor.id)
                ) || doctors.find(doctor =>
                    doctor.name === savedDoctor.name
                );

                if (match && match.available) {
                    doctorSelect.value = String(match.id);
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
            "Could not load doctors. Please make sure the MediBook " +
            "backend is running on port 8080, then reload this page."
        );
    }

    function updateDoctorCard() {
        const doctor = doctorsById.get(doctorSelect.value);

        if (doctorName) {
            doctorName.textContent = doctor
                ? doctor.name
                : "Select a Doctor";
        }

        if (doctorSpecialization) {
            doctorSpecialization.textContent = doctor
                ? doctor.specialization
                : "Choose a doctor for your appointment";
        }

        if (doctorExperience) {
            doctorExperience.textContent = doctor
                ? `${doctor.experience} years experience`
                : "--";
        }
    }

    doctorSelect.addEventListener("change", updateDoctorCard);

    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        const doctorId = doctorSelect.value;
        const appointmentDate = dateInput?.value || "";
        const appointmentTime = timeInput?.value || "";
        const reason =
            document.getElementById("reason")?.value.trim() || "";
        const phone =
            document.getElementById("phone")?.value.trim() || "";
        const email = emailInput?.value.trim() || "";
        const patientName = patientNameInput?.value.trim() || "";

        const modeInput =
            document.querySelector('input[name="mode"]:checked');

        if (!doctorId || !doctorsById.has(doctorId)) {
            alert("Please select a valid doctor.");
            return;
        }

        const doctor = doctorsById.get(doctorId);

        if (!doctor.available) {
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

        if (!reason || reason.length > 500) {
            alert("Please enter a reason of up to 500 characters.");
            return;
        }

        if (email.toLowerCase() !== patientEmail.toLowerCase()) {
            alert(
                "Please use the email of your logged-in patient account."
            );
            return;
        }

        if (!document.getElementById("terms")?.checked) {
            alert("Please accept the appointment terms and conditions.");
            return;
        }

        const submitButton =
            document.getElementById("confirmButton");

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.innerHTML =
                '<span><i class="fa-solid fa-spinner fa-spin"></i> ' +
                'Booking Appointment...</span>';
        }

        try {
            const body = new URLSearchParams({
                patientEmail,
                doctorId,
                appointmentDate,
                appointmentTime: convertTo24Hour(appointmentTime),
                reason
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

    if (submitButton) {
        submitButton.disabled = false;
        submitButton.innerHTML =
            '<span><i class="fa-solid fa-calendar-check"></i> ' +
            'Confirm Appointment</span>' +
            '<i class="fa-solid fa-arrow-right"></i>';
    }

    return;
}

            localStorage.removeItem("medibookPendingBooking");
            localStorage.removeItem("medibookSelectedDoctor");

            showAppointmentConfirmation({
                patientName,
                email: patientEmail,
                doctor: doctor.name,
                specialization: doctor.specialization,
                date: appointmentDate,
                time: formatTime(appointmentTime),
                mode: modeInput.value,
                status: getBookingStatus(message)
            });

        } catch (error) {
            console.error("Appointment booking error:", error);

            alert(
                "Could not contact the MediBook server. " +
                "Make sure the Java backend is running."
            );

        } finally {
            if (submitButton && document.body.contains(submitButton)) {
                submitButton.disabled = false;
                submitButton.innerHTML =
                    '<span><i class="fa-solid fa-calendar-check"></i> ' +
                    'Confirm Appointment</span>' +
                    '<i class="fa-solid fa-arrow-right"></i>';
            }
        }
    });

    function getBookingStatus(message) {
        try {
            const result = JSON.parse(message);
            return result.status || "Pending";
        } catch {
            return "Pending";
        }
    }

    function formatTime(value) {
        const match = value.match(
            /^(\d{1,2}):(\d{2})\s*(AM|PM)$/i
        );

        if (match) {
            return `${match[1].padStart(2, "0")}:${match[2]} ${match[3].toUpperCase()}`;
        }

        const parts = value.split(":");
        if (parts.length < 2) return value;

        let hours = Number(parts[0]);
        const minutes = parts[1];
        const period = hours >= 12 ? "PM" : "AM";

        hours = hours % 12 || 12;

        return `${String(hours).padStart(2, "0")}:${minutes} ${period}`;
    }

    function convertTo24Hour(timeText) {
        const match = timeText.match(
            /^(\d{1,2}):(\d{2})\s*(AM|PM)$/i
        );

        if (!match) return timeText;

        let hours = Number(match[1]);
        const minutes = match[2];
        const period = match[3].toUpperCase();

        if (period === "PM" && hours !== 12) hours += 12;
        if (period === "AM" && hours === 12) hours = 0;

        return `${String(hours).padStart(2, "0")}:${minutes}`;
    }

   
function showAppointmentConfirmation(booking) {
    document.querySelectorAll(
        "body > header, body > .appointment-hero, " +
        "body > main, body > footer"
    ).forEach(element => {
        element.style.display = "none";
    });

    // Prevent duplicate confirmation screens
    document.querySelectorAll(".confirmation-screen").forEach(el => el.remove());

    const screen = document.createElement("main");
    screen.className = "confirmation-screen";

    const safe = value => String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[char]);

    screen.innerHTML = `
        <style>
            .confirmation-screen {
                position: fixed;
                inset: 0;
                z-index: 99999;
                overflow-y: auto;
                display: flex;
                justify-content: center;
                align-items: center;
                padding: 24px 16px;
                box-sizing: border-box;
                background:
                    radial-gradient(circle at 5% 45%, #dff6f1 0, transparent 18%),
                    radial-gradient(circle at 95% 30%, #e0f7f4 0, transparent 18%),
                    linear-gradient(135deg, #effaf8, #f8fbff);
                font-family: Inter, Arial, sans-serif;
                color: #183b39;
            }

            .confirmation-card {
                width: 100%;
                max-width: 760px;
                margin: auto;
                padding: 28px 34px 22px;
                box-sizing: border-box;
                text-align: center;
                background: rgba(255,255,255,.97);
                border: 1px solid #dcefeb;
                border-radius: 24px;
                box-shadow: 0 18px 55px rgba(20,75,67,.12);
                animation: confirmationAppear .4s ease-out;
            }

            .confirmation-check-wrap {
                width: 100%;
                height: 105px;
                margin: 0 auto 12px;
                display: flex;
                align-items: center;
                justify-content: center;
            }

            .confirmation-check {
                width: 80px;
                height: 80px;
                min-width: 80px;
                min-height: 80px;
                border-radius: 50%;
                background: #22c55e;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0;
                padding: 0;
                box-sizing: border-box;
                animation: checkPop .45s ease-out;
            }

            .confirmation-check .confirmation-tick {
                display: block;
                width: 45px;
                height: 45px;
            }

            .confirmation-check .confirmation-tick path {
                stroke: #ffffff;
                stroke-width: 5;
                fill: none;
                stroke-linecap: round;
                stroke-linejoin: round;
            }

            .confirmation-card h1 {
                margin: 0 0 10px;
                font-size: clamp(27px, 4vw, 36px);
                color: #087f70;
            }

            .confirmation-subtitle {
                margin: 0 auto 24px;
                line-height: 1.6;
                color: #657a78;
            }

            .confirmation-details {
                padding: 22px 24px;
                text-align: left;
                background: #f7fbfa;
                border: 1px solid #e1eeeb;
                border-radius: 16px;
            }

            .confirmation-details h2 {
                margin: 0 0 18px;
                font-size: 20px;
                color: #173e3a;
            }

            .confirmation-grid {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 18px 24px;
            }

            .confirmation-row {
                display: flex;
                gap: 12px;
                align-items: flex-start;
                min-width: 0;
            }

            .confirmation-row > i {
                width: 21px;
                flex-shrink: 0;
                margin-top: 3px;
                color: #078f83;
                text-align: center;
            }

            .confirmation-row small {
                display: block;
                margin-bottom: 4px;
                color: #718582;
            }

            .confirmation-row strong {
                overflow-wrap: anywhere;
                line-height: 1.5;
            }

            .confirmation-status {
                display: inline-block;
                padding: 6px 12px;
                border-radius: 20px;
                background: #fff0cf;
                color: #815b06;
                font-size: 13px;
                font-weight: 700;
            }

            .confirmation-divider {
                height: 1px;
                margin: 22px 0 15px;
                background: #dfede9;
            }

            .confirmation-home {
                width: min(100%, 320px);
                padding: 15px 20px;
                border: 0;
                border-radius: 12px;
                background: #078f83;
                color: #ffffff;
                font-size: 16px;
                font-weight: 700;
                cursor: pointer;
                transition: background .2s, transform .2s;
            }

            .confirmation-home:hover {
                background: #06685d;
                transform: translateY(-1px);
            }

            .confirmation-note {
                margin: 16px 0 0;
                color: #7a8b88;
                font-size: 12px;
                line-height: 1.5;
            }

            @keyframes confirmationAppear {
                from {
                    opacity: 0;
                    transform: translateY(12px);
                }
                to {
                    opacity: 1;
                    transform: translateY(0);
                }
            }

            @keyframes checkPop {
                0% { transform: scale(.6); }
                75% { transform: scale(1.08); }
                100% { transform: scale(1); }
            }

            @media (max-width: 560px) {
                .confirmation-card {
                    padding: 22px 16px;
                }

                .confirmation-grid {
                    grid-template-columns: 1fr;
                    gap: 17px;
                }

                .confirmation-details {
                    padding: 18px;
                }
            }

            @media (prefers-reduced-motion: reduce) {
                .confirmation-card,
                .confirmation-check {
                    animation: none;
                }
            }
        </style>

        <section class="confirmation-card" aria-labelledby="confirmationTitle">
            <div class="confirmation-check-wrap" aria-hidden="true">
                <div class="confirmation-check">
                    <svg
                        class="confirmation-tick"
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 52 52"
                    >
                        <path d="M14 27 L22 35 L39 17"></path>
                    </svg>
                </div>
            </div>

            <h1 id="confirmationTitle">Appointment Confirmed!</h1>

            <p class="confirmation-subtitle">
                Your appointment has been booked successfully.<br>
                Thank you for choosing MediBook.
            </p>

            <div class="confirmation-details">
                <h2>
                    <i class="fa-regular fa-calendar-check"></i>
                    &nbsp; Appointment Details
                </h2>

                <div class="confirmation-grid">
                    <div class="confirmation-row">
                        <i class="fa-solid fa-user"></i>
                        <div>
                            <small>Patient</small>
                            <strong>${safe(booking.patientName)}</strong>
                        </div>
                    </div>

                    <div class="confirmation-row">
                        <i class="fa-regular fa-clock"></i>
                        <div>
                            <small>Time</small>
                            <strong>${safe(booking.time)}</strong>
                        </div>
                    </div>

                    <div class="confirmation-row">
                        <i class="fa-solid fa-envelope"></i>
                        <div>
                            <small>Email</small>
                            <strong>${safe(booking.email)}</strong>
                        </div>
                    </div>

                    <div class="confirmation-row">
                        <i class="fa-solid fa-video"></i>
                        <div>
                            <small>Consultation Mode</small>
                            <strong>${safe(booking.mode)}</strong>
                        </div>
                    </div>

                    <div class="confirmation-row">
                        <i class="fa-solid fa-user-doctor"></i>
                        <div>
                            <small>Doctor</small>
                            <strong>${safe(booking.doctor)}</strong>
                            <div>${safe(booking.specialization)}</div>
                        </div>
                    </div>

                    <div class="confirmation-row">
                        <i class="fa-solid fa-circle-info"></i>
                        <div>
                            <small>Booking Status</small>
                            <span class="confirmation-status">
                                ${safe(booking.status)}
                            </span>
                        </div>
                    </div>

                    <div class="confirmation-row">
                        <i class="fa-solid fa-calendar-day"></i>
                        <div>
                            <small>Date</small>
                            <strong>${safe(booking.date)}</strong>
                        </div>
                    </div>
                </div>

                <div class="confirmation-divider"></div>

                <div style="text-align:center">
                    <button type="button" class="confirmation-home">
                        <i class="fa-solid fa-house"></i>
                        &nbsp; Go to Home &nbsp;
                        <i class="fa-solid fa-arrow-right"></i>
                    </button>
                </div>

                <p class="confirmation-note" style="text-align:center">
                    <i class="fa-solid fa-circle-info"></i>
                    Please keep your appointment details for future reference.
                </p>
            </div>
        </section>
    `;

    document.body.appendChild(screen);

    screen.querySelector(".confirmation-home").addEventListener(
        "click",
        function () {
            window.location.href = "../index.html";
        }
    );
    }
});
