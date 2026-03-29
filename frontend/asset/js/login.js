/**
 * Adoptify - Login Page Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const togglePasswordBtn = document.getElementById('toggle-password');
    const submitBtn = document.getElementById('submit-btn');
    const btnSpinner = document.getElementById('btn-spinner');
    const btnText = document.getElementById('btn-text');
    const errorAlert = document.getElementById('error-alert');

    // 1. Password Visibility Toggle
    if (togglePasswordBtn) {
        togglePasswordBtn.addEventListener('click', () => {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            togglePasswordBtn.querySelector('i').classList.toggle('fa-eye');
            togglePasswordBtn.querySelector('i').classList.toggle('fa-eye-slash');
        });
    }

    // 2. Form Submission
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            // Reset state
            hideError();
            const email = emailInput.value.trim();
            const password = passwordInput.value;

            // Simple validation
            if (!validateEmail(email)) {
                showError("Please enter a valid email address.");
                return;
            }
            if (password.length < 6) {
                showError("Password must be at least 6 characters.");
                return;
            }

            setLoading(true);

            try {
                const response = await fetch('http://localhost:8080/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || 'Invalid email or password');
                }

                // Success: Store token and user data
                localStorage.setItem('token', data.token);
                localStorage.setItem('user', JSON.stringify(data.user || data)); // Handle both formats

                // Role-based redirection
                const role = data.role || (data.user && data.user.role);
                redirectUser(role);

            } catch (error) {
                console.error("Login error:", error);
                showError(error.message);
            } finally {
                setLoading(false);
            }
        });
    }

    function validateEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    function showError(msg) {
        if (errorAlert) {
            errorAlert.textContent = msg;
            errorAlert.classList.remove('d-none');
        }
    }

    function hideError() {
        if (errorAlert) {
            errorAlert.classList.add('d-none');
        }
    }

    function setLoading(isLoading) {
        if (submitBtn) {
            submitBtn.disabled = isLoading;
            if (btnSpinner) btnSpinner.classList.toggle('d-none', !isLoading);
            if (btnText) btnText.textContent = isLoading ? 'LOGGING IN...' : 'LOGIN';
        }
    }

    function redirectUser(role) {
        switch (role) {
            case 'ADMIN':
                window.location.href = '../admin.html';
                break;
            case 'NGO':
                window.location.href = '../ngo-dashboard.html';
                break;
            case 'REGULAR_USER':
            default:
                window.location.href = '../index.html';
                break;
        }
    }
});
