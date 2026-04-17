/**
 * Adoptify - Account Settings Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = 'http://localhost:8080/api/auth';
    const token = localStorage.getItem('token');
    
    if (!token) {
        window.location.href = '../auth/login.html';
        return;
    }

    const profileForm = document.getElementById('profile-form');
    const passwordForm = document.getElementById('password-form');
    const ngoFields = document.getElementById('ngo-only-fields');
    const headerName = document.getElementById('header-user-name');

    // 1. Fetch Profile
    const loadProfile = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/profile`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) throw new Error('Unauthenticated');
            
            const user = await response.json();
            
            // Fill form
            document.getElementById('f-name').value = user.fullName;
            document.getElementById('f-phone').value = user.phone;
            document.getElementById('f-city').value = user.city;
            document.getElementById('f-address').value = user.address;
            
            if (headerName) headerName.textContent = user.fullName;

            if (user.role === 'NGO') {
                ngoFields.classList.remove('d-none');
                document.getElementById('f-org').value = user.organizationName || '';
            }
        } catch (error) {
            localStorage.clear();
            window.location.href = '../auth/login.html';
        }
    };

    loadProfile();

    // 2. Update Profile
    profileForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(profileForm);
        const payload = Object.fromEntries(formData.entries());

        try {
            const response = await fetch(`${API_BASE_URL}/profile`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) throw new Error('Update failed');

            Swal.fire({
                icon: 'success',
                title: 'Profile Updated',
                text: 'Your information has been saved successfully.',
                confirmButtonColor: '#f59e0b'
            });
            
            // Update local storage name if changed
            const user = JSON.parse(localStorage.getItem('user'));
            user.fullName = payload.fullName;
            if (payload.organizationName) user.organizationName = payload.organizationName;
            localStorage.setItem('user', JSON.stringify(user));
            if (headerName) headerName.textContent = payload.fullName;

        } catch (error) {
            Swal.fire('Error', 'Failed to update profile. Please try again.', 'error');
        }
    });

    // 3. Change Password
    passwordForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const oldPass = passwordForm.oldPassword.value;
        const newPass = passwordForm.newPassword.value;
        const confirmPass = document.getElementById('confirmPassword').value;

        if (newPass !== confirmPass) {
            return Swal.fire('Error', 'New passwords do not match!', 'error');
        }

        try {
            const response = await fetch(`${API_BASE_URL}/change-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ oldPassword: oldPass, newPassword: newPass })
            });

            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.message || 'Password update failed');
            }

            Swal.fire('Success', 'Password changed successfully!', 'success');
            passwordForm.reset();
        } catch (error) {
            Swal.fire('Error', error.message, 'error');
        }
    });

    // Logout
    document.getElementById('logout-settings-btn').addEventListener('click', () => {
        localStorage.clear();
        window.location.href = '../index.html';
    });
});
