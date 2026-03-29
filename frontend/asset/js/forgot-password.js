/**
 * Adoptify - Forgot Password Page Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    const forgotForm = document.getElementById('forgot-form');
    const submitBtn = document.getElementById('submit-btn');
    const btnSpinner = document.getElementById('btn-spinner');
    const btnText = document.getElementById('btn-text');
    const errorAlert = document.getElementById('error-alert');
    const successAlert = document.getElementById('success-alert');

    // 1. Real-time Validation
    const emailInput = document.getElementById('email');
    if (emailInput) {
        emailInput.addEventListener('input', () => {
            const isValid = emailInput.checkValidity();
            submitBtn.disabled = !isValid;
        });
    }

    // 2. Form Submission
    if (forgotForm) {
        forgotForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            hideAlerts();
            setLoading(true);

            const email = emailInput.value;

            try {
                const response = await fetch('http://localhost:8080/api/auth/forgot-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email })
                });

                // For security reasons, often API returns 200 even if email not found
                // We show a generic success message
                showSuccess("If an account exists with this email, you will receive a reset link shortly.");
                
                // Redirect after delay
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 3000);

            } catch (error) {
                console.error("Forgot password error:", error);
                showError("Something went wrong. Please try again later.");
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
            forgotForm.classList.add('d-none'); // Hide form on success
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
            if (btnText) btnText.textContent = isLoading ? 'SENDING...' : 'SEND RESET LINK';
        }
    }
});
