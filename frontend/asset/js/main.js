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
            
            if (userNameSpan) userNameSpan.textContent = user.fullName || 'User';
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
