/* =====================================================
   MEDIBOOK - LANDING PAGE JAVASCRIPT
===================================================== */


/* =====================================================
   MOBILE MENU
===================================================== */

function toggleMenu() {

    const menu = document.getElementById("mobileMenu");

    if (menu) {
        menu.classList.toggle("show");
    }

}


function closeMenu() {

    const menu = document.getElementById("mobileMenu");

    if (menu) {
        menu.classList.remove("show");
    }

}


/* =====================================================
   LOGIN
===================================================== */

function goToLogin() {

    window.location.href = "pages/login.html";

}


/* =====================================================
   REGISTER
===================================================== */

function goToRegister() {

    window.location.href = "pages/register.html";

}


/* =====================================================
   FIND DOCTOR
===================================================== */

function findDoctor() {

    const doctorsSection =
        document.getElementById("doctors");

    if (doctorsSection) {

        doctorsSection.scrollIntoView({
            behavior: "smooth"
        });

    }

}


/* =====================================================
   BOOK APPOINTMENT
===================================================== */

function bookAppointment() {

    window.location.href = "pages/login.html";

}


/* =====================================================
   PAGE LOAD
===================================================== */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        console.log(
            "MediBook landing page loaded successfully."
        );

    }
);