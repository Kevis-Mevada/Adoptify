/**
 * Adoptify - Registration Page Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    const registerForm = document.getElementById('register-form');
    const roleRadios = document.querySelectorAll('input[name="role"]');
    const ngoFields = document.getElementById('ngo-fields');
    const submitBtn = document.getElementById('submit-btn');
    const btnSpinner = document.getElementById('btn-spinner');
    const btnText = document.getElementById('btn-text');
    const errorAlert = document.getElementById('error-alert');

    // 1. NGO Fields Toggle
    roleRadios.forEach(radio => {
        radio.addEventListener('change', (e) => {
            if (e.target.value === 'NGO') {
                ngoFields.classList.remove('d-none');
                setNgoFieldsRequired(true);
            } else {
                ngoFields.classList.add('d-none');
                setNgoFieldsRequired(false);
            }
            validateForm();
        });
    });

    function setNgoFieldsRequired(required) {
        document.getElementById('organizationName').required = required;
        document.getElementById('licenseNumber').required = required;
    }

    // 2. Real-time Validation & Button State
    const inputs = registerForm.querySelectorAll('input, textarea');
    inputs.forEach(input => {
        input.addEventListener('input', validateForm);
    });

    function validateForm() {
        const isValid = registerForm.checkValidity();
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        const passwordsMatch = password === confirmPassword;
        const termsChecked = document.getElementById('terms').checked;

        submitBtn.disabled = !(isValid && passwordsMatch && termsChecked);
    }

    // 3. Password Toggle Logic
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

    // 4. Form Submission
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            hideError();
            setLoading(true);

            const formData = new FormData(registerForm);
            const rawPayload = Object.fromEntries(formData.entries());
            
            // Build the exact payload structure requested
            const payload = {
                fullName: rawPayload.fullName,
                email: rawPayload.email,
                phone: rawPayload.phone,
                password: rawPayload.password,
                city: rawPayload.city,
                address: rawPayload.address,
                role: rawPayload.role
            };

            // Add NGO specific fields if applicable
            if (rawPayload.role === 'NGO') {
                payload.organizationName = rawPayload.organizationName;
                payload.licenseNumber = rawPayload.licenseNumber;
            }

            try {
                const response = await fetch('http://localhost:8080/api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || 'Registration failed');
                }

                // Success notification & redirect
                alert(payload.role === 'NGO' 
                    ? "NGO Registration submitted! Pending admin verification." 
                    : "Account created successfully!");
                
                window.location.href = payload.role === 'NGO' ? '../index.html' : 'login.html';

            } catch (error) {
                console.error("Register error:", error);
                showError(error.message);
            } finally {
                setLoading(false);
            }
        });
    }

    function showError(msg) {
        if (errorAlert) {
            errorAlert.textContent = msg;
            errorAlert.classList.remove('d-none');
            errorAlert.scrollIntoView({ behavior: 'smooth', block: 'center' });
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
            if (btnText) btnText.textContent = isLoading ? 'REGISTERING...' : 'REGISTER';
        }
    }

    function showSuccessModal(message, redirectUrl) {
        // Use Bootstrap Modal API if available, or simple alert/redirect
        alert(message);
        window.location.href = redirectUrl;
    }
});
