/**
 * Adoptify - Reset Password Page Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    const resetForm = document.getElementById('reset-form');
    const submitBtn = document.getElementById('submit-btn');
    const btnSpinner = document.getElementById('btn-spinner');
    const btnText = document.getElementById('btn-text');
    const errorAlert = document.getElementById('error-alert');
    const successAlert = document.getElementById('success-alert');

    // Get token from URL
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    if (!token) {
        showError("Invalid or missing reset token. Please request a new link.");
        if (resetForm) resetForm.classList.add('d-none');
    }

    // 1. Password Matching & Complexity
    const passwordInput = document.getElementById('password');
    const confirmInput = document.getElementById('confirmPassword');

    const validatePasswords = () => {
        const password = passwordInput.value;
        const confirm = confirmInput.value;
        const isLengthValid = password.length >= 6;
        const isMatch = password === confirm;

        submitBtn.disabled = !(isLengthValid && isMatch && token);
    };

    if (passwordInput && confirmInput) {
        passwordInput.addEventListener('input', validatePasswords);
        confirmInput.addEventListener('input', validatePasswords);
    }

    // 2. Password Toggle Logic
    setupPasswordToggle('password', 'toggle-password');
    setupPasswordToggle('confirmPassword', 'toggle-confirm-password');

    function setupPasswordToggle(inputId, toggleId) {
        const input = document.getElementById(inputId);
        const btn = document.getElementById(toggleId);
        if (input && btn) {
            btn.addEventListener('click', () => {
                const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
                input.setAttribute('type', type);
                btn.querySelector('i').classList.toggle('fa-eye');
                btn.querySelector('i').classList.toggle('fa-eye-slash');
            });
        }
    }

    // 3. Form Submission
    if (resetForm) {
        resetForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            hideAlerts();
            setLoading(true);

            const newPassword = passwordInput.value;

            try {
                const response = await fetch('http://localhost:8080/api/auth/reset-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ token, newPassword })
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || 'Reset failed');
                }

                showSuccess("Password reset successfully! Redirecting to login...");
                
                // Redirect after success
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 3000);

            } catch (error) {
                console.error("Reset password error:", error);
                showError(error.message || "Failed to reset password. The link might be expired.");
            } finally {
                setLoading(false);
            }
        });
    }

    function showError(msg) {
        if (errorAlert) {
            errorAlert.textContent = msg;
            errorAlert.classList.remove('d-none');
        }
    }

    function showSuccess(msg) {
        if (successAlert) {
            successAlert.textContent = msg;
            successAlert.classList.remove('d-none');
            if (resetForm) resetForm.classList.add('d-none');
        }
    }

    function hideAlerts() {
        if (errorAlert) errorAlert.classList.add('d-none');
        if (successAlert) successAlert.classList.add('d-none');
    }

    function setLoading(isLoading) {
        if (submitBtn) {
            submitBtn.disabled = isLoading;
            if (btnSpinner) btnSpinner.classList.toggle('d-none', !isLoading);
            if (btnText) btnText.textContent = isLoading ? 'RESETTING...' : 'RESET PASSWORD';
        }
    }
});
