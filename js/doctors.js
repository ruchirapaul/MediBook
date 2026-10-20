
"use strict";

// =============================================
// MEDIBOOK - DOCTORS PAGE
// =============================================

const DOCTORS_API = "http://localhost:8080/api/doctors";

let allDoctors = [];

// =============================================
// MOBILE MENU
// =============================================

function toggleMenu() {
    const menu = document.getElementById("mobileMenu");
    if (!menu) return;

    menu.style.display =
        menu.style.display === "block" ? "none" : "block";
}

function closeMenu() {
    const menu = document.getElementById("mobileMenu");
    if (menu) menu.style.display = "none";
}

// =============================================
// NORMALIZE SPECIALIZATION
// =============================================

function normalizeSpecialization(specialization) {
    const value = String(specialization || "").trim().toLowerCase();
    const normalized = value.replace(/[^a-z]/g, "");

    const specializationMap = {
        cardiology: "cardiologist",
        cardiologist: "cardiologist",
        dermatology: "dermatologist",
        dermatologist: "dermatologist",
        neurology: "neurologist",
        neurologist: "neurologist",
        orthopedics: "orthopedic",
        orthopedic: "orthopedic",
        orthopaedics: "orthopedic",
        orthopaedic: "orthopedic",
        pediatrics: "pediatrician",
        pediatric: "pediatrician",
        pediatrician: "pediatrician",
        paediatrics: "pediatrician",
        paediatrician: "pediatrician",
        dentistry: "dentist",
        dental: "dentist",
        dentist: "dentist"
    };

    return specializationMap[normalized] ||
        value.replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

// =============================================
// DISPLAY SPECIALIZATION
// =============================================

function displaySpecialization(specialization) {
    return String(specialization || "").trim() ||
        "General Practitioner";
}

// =============================================
// CREATE HTML ELEMENT SAFELY
// =============================================

function createElement(tag, className, text) {
    const element = document.createElement(tag);

    if (className) element.className = className;

    if (text !== undefined && text !== null) {
        element.textContent = String(text);
    }

    return element;
}

function createIcon(iconClass) {
    const icon = document.createElement("i");
    icon.className = iconClass;
    icon.setAttribute("aria-hidden", "true");
    return icon;
}

// =============================================
// NAVBAR LOGIN STATUS
// =============================================

function updateDoctorsNavbar() {
    const navButtons = document.querySelector(".nav-buttons");

    if (!navButtons) return;

    const isLoggedIn =
        localStorage.getItem("medibookLoggedIn") === "true";

    const role = (
        localStorage.getItem("medibookRole") || ""
    ).toLowerCase();

    const name =
        localStorage.getItem("medibookUserName") ||
        localStorage.getItem("medibookName") ||
        localStorage.getItem("medibookEmail")?.split("@")[0];

    // Leave the original Login/Register links for logged-out visitors.
    if (!isLoggedIn || role !== "patient" || !name) return;

    navButtons.replaceChildren();

    const welcome = createElement(
        "span",
        "welcome-user",
        "Welcome, " + name
    );

    const logout = createElement("button", "login-btn", "Logout");
    logout.type = "button";

    logout.addEventListener("click", function () {
        localStorage.removeItem("medibookLoggedIn");
        localStorage.removeItem("medibookEmail");
        localStorage.removeItem("medibookRole");
        localStorage.removeItem("medibookUserName");
        localStorage.removeItem("medibookName");

        // Clear booking continuation state on logout.
        localStorage.removeItem("medibookPendingBooking");
        localStorage.removeItem("medibookSelectedDoctor");

        window.location.href = "login.html";
    });

    navButtons.append(welcome, logout);
}

// =============================================
// LOAD DOCTORS FROM JAVA API / MYSQL
// =============================================

async function loadDoctors() {
    const grid = document.getElementById("doctorsGrid");
    const count = document.getElementById("doctorCount");
    const noResults = document.getElementById("noResults");

    if (!grid) return;

    grid.replaceChildren();

    const loadingMessage = createElement(
        "p",
        "",
        "Loading doctors..."
    );

    loadingMessage.id = "doctorsLoading";
    grid.appendChild(loadingMessage);

    if (count) count.textContent = "Loading doctors...";
    if (noResults) noResults.style.display = "none";

    try {
        const response = await fetch(DOCTORS_API, {
            method: "GET",
            headers: { "Accept": "application/json" }
        });

        if (!response.ok) {
            throw new Error("Server returned status " + response.status);
        }

        const contentType = response.headers.get("content-type") || "";

        if (!contentType.includes("application/json")) {
            throw new Error("Doctors API did not return JSON.");
        }

        const doctors = await response.json();

        if (!Array.isArray(doctors)) {
            throw new Error("Unexpected response from doctors API.");
        }

        allDoctors = doctors.map(function (doctor) {
            return {
                id: doctor.id,
                name: String(doctor.name || "").trim(),
                specialization: String(
                    doctor.specialization || ""
                ).trim(),
                qualification: String(
                    doctor.qualification || ""
                ).trim(),
                experience: Number(doctor.experience) || 0,
                available:
                    doctor.available === true ||
                    String(doctor.available).toLowerCase() === "true" ||
                    doctor.available === 1 ||
                    String(doctor.available) === "1"
            };
        });

        displayDoctors(allDoctors);
        filterDoctors();
        continuePendingBooking();

    } catch (error) {
        console.error("Error loading doctors:", error);
        grid.replaceChildren();

        grid.appendChild(createElement(
            "p",
            "",
            "Unable to load doctors. Please make sure the Java backend " +
            "and MySQL database are running, then refresh this page."
        ));

        if (count) count.textContent = "Doctors could not be loaded";
        if (noResults) noResults.style.display = "none";
    }
}

// =============================================
// DISPLAY DOCTOR CARDS
// =============================================

function displayDoctors(doctors) {
    const grid = document.getElementById("doctorsGrid");
    if (!grid) return;

    grid.replaceChildren();

    if (doctors.length === 0) {
        grid.appendChild(createElement(
            "p",
            "",
            "No doctors are currently listed in the database."
        ));
        return;
    }

    doctors.forEach(function (doctor) {
        grid.appendChild(createDoctorCard(doctor));
    });
}

// =============================================
// CREATE ONE DOCTOR CARD
// =============================================

function createDoctorCard(doctor) {
    const card = createElement("div", "doctor-card");
    const doctorName = doctor.name || "Doctor";
    const specialization = displaySpecialization(
        doctor.specialization
    );

    card.dataset.name = doctorName;
    card.dataset.specialization =
        normalizeSpecialization(doctor.specialization);

    // Doctor image/avatar
    const imageSection = createElement("div", "doctor-image");
    const avatar = createElement("div", "doctor-avatar");

    avatar.appendChild(createIcon("fa-solid fa-user-doctor"));
    imageSection.appendChild(avatar);

    // Availability indicator
    const availability = createElement(
        "span",
        doctor.available
            ? "availability available"
            : "availability unavailable"
    );

    availability.appendChild(createElement("span", "", ""));
    availability.appendChild(document.createTextNode(
        doctor.available ? "Available" : "Unavailable"
    ));

    imageSection.appendChild(availability);
    card.appendChild(imageSection);

    // Doctor details
    const info = createElement("div", "doctor-info");

    const displayedName = /^dr\.?\s/i.test(doctorName)
        ? doctorName
        : "Dr. " + doctorName;

    info.appendChild(createElement("h3", "", displayedName));

    const specializationElement = createElement(
        "p",
        "specialization"
    );

    specializationElement.appendChild(
        createIcon("fa-solid fa-stethoscope")
    );

    specializationElement.appendChild(
        document.createTextNode(" " + specialization)
    );

    info.appendChild(specializationElement);

    if (doctor.qualification) {
        const qualificationElement = createElement(
            "p",
            "qualification"
        );

        qualificationElement.appendChild(
            createIcon("fa-solid fa-graduation-cap")
        );

        qualificationElement.appendChild(
            document.createTextNode(" " + doctor.qualification)
        );

        info.appendChild(qualificationElement);
    }

    const experienceElement = createElement("p", "experience");

    experienceElement.appendChild(
        createIcon("fa-solid fa-briefcase")
    );

    experienceElement.appendChild(document.createTextNode(
        " " + doctor.experience +
        (doctor.experience === 1
            ? " Year Experience"
            : " Years Experience")
    ));

    info.appendChild(experienceElement);

    // Buttons
    const footer = createElement("div", "doctor-footer");

    const profileButton = createElement(
        "button",
        "profile-btn",
        "View Profile"
    );

    profileButton.type = "button";

    profileButton.addEventListener("click", function () {
        viewProfile(doctor);
    });

    footer.appendChild(profileButton);

    const bookButton = createElement("button", "book-btn");
    bookButton.type = "button";
    bookButton.disabled = !doctor.available;

    bookButton.appendChild(document.createTextNode(
        doctor.available ? "Book " : "Unavailable "
    ));

    bookButton.appendChild(
        createIcon("fa-solid fa-arrow-right")
    );

    if (doctor.available) {
        bookButton.addEventListener("click", function () {
            bookAppointment(doctor);
        });
    }

    footer.appendChild(bookButton);
    info.appendChild(footer);
    card.appendChild(info);

    return card;
}

// =============================================
// SEARCH AND FILTER DOCTORS
// =============================================

function filterDoctors() {
    const searchInput = document.getElementById("doctorSearch");

    const specializationFilter =
        document.getElementById("specializationFilter");

    const doctorCount = document.getElementById("doctorCount");
    const noResults = document.getElementById("noResults");

    const searchText = searchInput
        ? searchInput.value.toLowerCase().trim()
        : "";

    const selectedSpecialization = specializationFilter
        ? specializationFilter.value
        : "all";

    const doctorCards = document.querySelectorAll(".doctor-card");
    let visibleDoctors = 0;

    doctorCards.forEach(function (card) {
        const doctorName =
            (card.dataset.name || "").toLowerCase();

        const specialization =
            (card.dataset.specialization || "").toLowerCase();

        const specializationText =
            (card.querySelector(".specialization")?.textContent || "")
                .toLowerCase();

        const matchesSearch =
            doctorName.includes(searchText) ||
            specialization.includes(searchText) ||
            specializationText.includes(searchText);

        const matchesSpecialization =
            selectedSpecialization === "all" ||
            specialization === selectedSpecialization;

        const shouldDisplay =
            matchesSearch && matchesSpecialization;

        card.style.display = shouldDisplay ? "" : "none";

        if (shouldDisplay) visibleDoctors++;
    });

    if (doctorCount) {
        doctorCount.textContent = "Showing " + visibleDoctors +
            (visibleDoctors === 1 ? " doctor" : " doctors");
    }

    if (noResults) {
        noResults.style.display =
            visibleDoctors === 0 ? "block" : "none";
    }
}

// =============================================
// VIEW DOCTOR PROFILE
// =============================================

function viewProfile(doctor) {
    const doctorName =
        typeof doctor === "string" ? doctor : doctor.name;

    alert(
        "Doctor profile for " + doctorName +
        " will be available in the next module."
    );
}

// =============================================
// BOOK APPOINTMENT
// =============================================

function bookAppointment(doctor) {
    if (!doctor || !doctor.available) {
        alert("This doctor is currently unavailable.");
        return;
    }

    const selectedDoctor = {
        id: doctor.id,
        name: doctor.name,
        specialization: doctor.specialization,
        qualification: doctor.qualification,
        experience: doctor.experience,
        available: doctor.available
    };

    localStorage.setItem(
        "medibookSelectedDoctor",
        JSON.stringify(selectedDoctor)
    );

    localStorage.setItem("medibookPendingBooking", "true");

    const isPatientLoggedIn =
        localStorage.getItem("medibookLoggedIn") === "true" &&
        (localStorage.getItem("medibookRole") || "").toLowerCase()
            === "patient";

    if (isPatientLoggedIn) {
        window.location.href = "appointment.html";
        return;
    }

    const confirmation = confirm(
        "You selected " + selectedDoctor.name +
        ".\n\nPlease log in as a patient to continue."
    );

    if (confirmation) {
        window.location.href = "login.html";
    }
}

// =============================================
// CONTINUE AFTER LOGIN
// =============================================

function continuePendingBooking() {
    const params = new URLSearchParams(window.location.search);

    const shouldContinue =
        params.get("continueBooking") === "true";

    if (!shouldContinue) return;

    const isPatientLoggedIn =
        localStorage.getItem("medibookLoggedIn") === "true" &&
        (localStorage.getItem("medibookRole") || "").toLowerCase()
            === "patient";

    const savedDoctorText =
        localStorage.getItem("medibookSelectedDoctor");

    if (!isPatientLoggedIn || !savedDoctorText) {
        alert("Please log in as a patient to continue booking.");
        return;
    }

    try {
        const savedDoctor = JSON.parse(savedDoctorText);

        if (!savedDoctor || savedDoctor.id == null) {
            throw new Error("Saved doctor has no database ID.");
        }

        window.history.replaceState(
            {},
            document.title,
            window.location.pathname
        );

        window.location.href = "appointment.html";

    } catch (error) {
        console.error("Could not restore selected doctor:", error);

        alert(
            "Your selected doctor could not be restored. " +
            "Please return to the doctors page and select a doctor again."
        );
    }
}

// =============================================
// INITIALIZE PAGE
// =============================================

document.addEventListener("DOMContentLoaded", function () {
    updateDoctorsNavbar();

    const searchInput = document.getElementById("doctorSearch");

    const specializationFilter =
        document.getElementById("specializationFilter");

    if (searchInput) {
        searchInput.addEventListener("input", filterDoctors);
    }

    if (specializationFilter) {
        specializationFilter.addEventListener(
            "change",
            filterDoctors
        );
    }

    loadDoctors();
});

