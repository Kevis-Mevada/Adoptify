/**
 * Adoptify - Main Application JavaScript
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Navigation
    initNavigation();

    // 2. Setup Smooth Scrolling
    setupSmoothScroll();

    // 3. Setup Intersection Observer for Animations
    setupScrollAnimations();
});

/**
 * Handles Authentication State in the Header
 */
function initNavigation() {
    const authSection = document.getElementById('auth-section');
    const userSection = document.getElementById('user-section');
    const userNameSpan = document.getElementById('user-name');
    const logoutBtn = document.getElementById('logout-btn');

    const token = localStorage.getItem('token');
    const userJson = localStorage.getItem('user');

    if (token && userJson) {
        try {
            const user = JSON.parse(userJson);
            if (authSection) authSection.classList.add('d-none');
            if (userSection) userSection.classList.remove('d-none');
            if (userSection) userSection.classList.add('d-flex');
            
            if (userNameSpan) {
                const isRoot = window.location.pathname.endsWith('index.html') || window.location.pathname.endsWith('/');
                const settingsPath = isRoot ? 'profile/settings.html' : '../profile/settings.html';
                userNameSpan.innerHTML = `${user.organizationName || user.fullName || 'User'} <a href="${settingsPath}" class="ms-2 text-warning small text-decoration-none"><i class="fa-solid fa-cog"></i></a>`;
            }

            // Dynamically add NGO Dashboard link if user is NGO
            if (user.role === 'NGO') {
                const actionDrop = document.getElementById('animalActionDrop') || document.querySelector('.dropdown-toggle');
                if (actionDrop) {
                    const dropdownMenu = actionDrop.nextElementSibling;
                    if (dropdownMenu && !dropdownMenu.querySelector('[href*="ngo-dashboard.html"]')) {
                        const li = document.createElement('li');
                        // Determine relative path based on current location
                        const isRoot = window.location.pathname.endsWith('index.html') || window.location.pathname.endsWith('/');
                        const path = isRoot ? 'ngo/ngo-dashboard.html' : '../ngo/ngo-dashboard.html';
                        li.innerHTML = `<a class="dropdown-item" href="${path}">NGO Dashboard</a>`;
                        dropdownMenu.appendChild(li);
                    }
                }
            }
        } catch (e) {
            console.error("Error parsing user data:", e);
            clearAuth();
        }
    } else {
        if (authSection) authSection.classList.remove('d-none');
        if (userSection) userSection.classList.add('d-none');
    }

    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            clearAuth();
            window.location.reload();
        });
    }
}

function clearAuth() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
}

/**
 * Basic Smooth Scrolling for Anchor Links
 */
function setupSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const href = this.getAttribute('href');
            if (href === '#') return;
            
            e.preventDefault();
            const target = document.querySelector(href);
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth'
                });
            }
        });
    });
}

/**
 * Simple Fade-in Animations on Scroll
 */
function setupScrollAnimations() {
    const observerOptions = {
        threshold: 0.1
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('animate-visible');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    document.querySelectorAll('.animate-on-scroll').forEach(el => {
        observer.observe(el);
    });
}

/**
 * Global Helper to resolve relative image URLs to the server
 */
window.getFullImageUrl = (url) => {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    // Check if it's a relative upload path
    if (url.startsWith('/uploads')) {
        return `http://localhost:8080${url}`;
    }
    return url;
};
