document.addEventListener('DOMContentLoaded', () => {
    // 1. Authentication Check
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user'));

    if (!token || !user) {
        window.location.href = '../auth/login.html?redirect=rescue/rescue.html';
        return;
    }

    // 2. Constants & Elements
    const API_BASE_URL = 'http://localhost:8080/api';
    const rescueForm = document.getElementById('rescueForm');
    const dropZone = document.getElementById('dropZone');
    const imageInput = document.getElementById('imageInput');
    const imagePreviewContainer = document.getElementById('imagePreview');
    const imageError = document.getElementById('imageError');
    const btnLocation = document.getElementById('btnGetCurrentLocation');
    const userContainer = document.getElementById('userContainer');
    const successModal = new bootstrap.Modal(document.getElementById('successModal'));

    // Render User Header Items
    if (userContainer) {
        userContainer.innerHTML = `
            <div id="user-section" class="d-flex align-items-center gap-3">
                <div class="text-end lh-1">
                    <div id="user-name" class="fw-bold small">${user.fullName || user.username}</div>
                    <div id="user-role" class="text-muted" style="font-size: 0.7rem;">${user.role}</div>
                </div>
                <div class="user-placeholder-circle bg-warning border-0 text-dark">
                    <i class="fa-solid fa-user"></i>
                </div>
                <a href="#" id="logout-btn" class="text-muted"><i class="fa-solid fa-right-from-bracket fs-5"></i></a>
            </div>
        `;
        document.getElementById('logout-btn').addEventListener('click', (e) => {
            e.preventDefault();
            localStorage.clear();
            window.location.href = '../auth/login.html';
        });
    }

    // Auto-fill reporter details
    document.getElementById('reporterName').value = user.fullName || user.username;
    document.getElementById('reporterPhone').value = user.phone || '';

    let selectedFiles = [];

    // 3. Geolocation Functions
    btnLocation.addEventListener('click', getCurrentLocation);

    function getCurrentLocation() {
        if (!navigator.geolocation) {
            showToast('Geolocation is not supported by your browser', 'error');
            return;
        }

        btnLocation.disabled = true;
        btnLocation.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> FETCHING...';

        navigator.geolocation.getCurrentPosition(
            async (position) => {
                const { latitude, longitude } = position.coords;
                document.getElementById('latitude').value = latitude;
                document.getElementById('longitude').value = longitude;
                
                try {
                    const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}`);
                    const data = await response.json();
                    if (data && data.display_name) {
                        document.getElementById('locationAddress').value = data.display_name;
                        updateMapPreview(latitude, longitude);
                    }
                } catch (error) {
                    console.error("Geocoding error:", error);
                    showToast('Location fetched but address could not be resolved', 'info');
                    updateMapPreview(latitude, longitude);
                } finally {
                    resetLocationBtn();
                }
            },
            (error) => {
                handleLocationError(error);
                resetLocationBtn();
            },
            { enableHighAccuracy: true, timeout: 10000 }
        );
    }

    function resetLocationBtn() {
        btnLocation.disabled = false;
        btnLocation.innerHTML = '<i class="fa-solid fa-crosshairs me-1"></i> USE CURRENT LOCATION';
    }

    function handleLocationError(error) {
        let msg = 'Could not get your location.';
        switch(error.code) {
            case error.PERMISSION_DENIED: msg = 'Location permission denied. Please allow location access.'; break;
            case error.POSITION_UNAVAILABLE: msg = 'Location information is unavailable.'; break;
            case error.TIMEOUT: msg = 'Location request timed out.'; break;
        }
        showToast(msg, 'error');
    }

    function updateMapPreview(lat, lng) {
        const mapDiv = document.getElementById('locationMap');
        mapDiv.innerHTML = `
            <div class="w-100 h-100 d-flex flex-column align-items-center justify-content-center bg-info-subtle">
                <i class="fa-solid fa-location-dot text-danger fs-1 mb-2"></i>
                <div class="fw-bold">LOCATION PINNED</div>
                <div class="small text-muted">${lat.toFixed(6)}, ${lng.toFixed(6)}</div>
            </div>
        `;
    }

    // 4. Image Handling (Drag & Drop + Click)
    dropZone.addEventListener('click', () => imageInput.click());
    
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('bg-secondary-subtle');
    });

    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('bg-secondary-subtle'));

    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('bg-secondary-subtle');
        handleFiles(e.dataTransfer.files);
    });

    imageInput.addEventListener('change', (e) => {
        handleFiles(e.target.files);
        imageInput.value = ''; // Reset for re-selection
    });

    function handleFiles(files) {
        const fileList = Array.from(files);
        
        fileList.forEach(file => {
            const error = validateImageFile(file);
            if (error) {
                showToast(error, 'error');
                return;
            }

            if (selectedFiles.length >= 5) {
                showToast('Maximum 5 images allowed', 'warning');
                return;
            }

            // Optional: AI Pre-check for first file or a random selected file
            // For now we just add to preview
            addFileToPreview(file);
        });

        imageError.classList.add('d-none');
    }

    function validateImageFile(file) {
        const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg'];
        if (!allowedTypes.includes(file.type)) return 'Invalid file type. Only JPG and PNG allowed.';
        if (file.size > 10 * 1024 * 1024) return 'File too large. Max 10MB per image.';
        return null;
    }

    async function addFileToPreview(file) {
        selectedFiles.push(file);
        
        const reader = new FileReader();
        reader.onload = (e) => {
            const wrapper = document.createElement('div');
            wrapper.className = 'preview-wrapper animate__animated animate__fadeIn';
            wrapper.innerHTML = `
                <img src="${e.target.result}">
                <button type="button" class="remove-img" data-name="${file.name}">&times;</button>
            `;
            imagePreviewContainer.appendChild(wrapper);
        };
        reader.readAsDataURL(file);

        // Immediate AI Validation Pre-check
        validateImageAI(file);
    }

    async function validateImageAI(file) {
        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch(`${API_BASE_URL}/rescue/validate-image`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });

            const result = await response.json();
            if (result.isAI) {
                showToast(`Potential AI detection: ${result.message}`, 'warning');
                // We don't remove it yet, but warn the user
            }
        } catch (error) {
            console.error("AI Pre-check failed", error);
        }
    }

    imagePreviewContainer.addEventListener('click', (e) => {
        if (e.target.classList.contains('remove-img')) {
            const name = e.target.getAttribute('data-name');
            selectedFiles = selectedFiles.filter(f => f.name !== name);
            e.target.parentElement.remove();
        }
    });

    // 5. Form Submission
    rescueForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!validateForm()) return;

        const submitBtn = document.getElementById('submitReportBtn');
        const originalText = submitBtn.innerHTML;
        const uploadBarContainer = document.getElementById('uploadBarContainer');
        const uploadBar = document.getElementById('uploadBar');

        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> PROCESSING REPORT...';
        uploadBarContainer.classList.remove('d-none');
        uploadBar.style.width = '30%';

        try {
            const reportData = {
                animalType: document.getElementById('animalType').value,
                animalCount: parseInt(document.getElementById('animalCount').value),
                animalCondition: document.getElementById('animalCondition').value,
                emergencyLevel: document.getElementById('emergencyLevel').value,
                locationAddress: document.getElementById('locationAddress').value,
                landmark: document.getElementById('landmark').value,
                latitude: parseFloat(document.getElementById('latitude').value),
                longitude: parseFloat(document.getElementById('longitude').value),
                description: document.getElementById('description').value,
                reporterName: document.getElementById('reporterName').value,
                reporterPhone: document.getElementById('reporterPhone').value,
                imagesRequired: true
            };

            const formData = new FormData();
            
            // Build the JSON part as a Blob with MIME type
            const blob = new Blob([JSON.stringify(reportData)], { type: 'application/json' });
            formData.append('report', blob);
            
            // Append files
            selectedFiles.forEach(file => formData.append('files', file));

            uploadBar.style.width = '60%';

            const response = await fetch(`${API_BASE_URL}/rescue/report`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                body: formData
            });

            uploadBar.style.width = '100%';
            const result = await response.json();

            if (response.ok) {
                document.getElementById('displayRescueId').innerText = `#${result.id}`;
                successModal.show();
                rescueForm.reset();
                imagePreviewContainer.innerHTML = '';
                selectedFiles = [];
            } else {
                throw new Error(result.message || 'Submission failed');
            }
        } catch (error) {
            console.error("Submission error:", error);
            handleError(error.message);
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
            uploadBarContainer.classList.add('d-none');
        }
    });

    function handleError(msg) {
        if (msg.toLowerCase().includes('ai-generated') || msg.toLowerCase().includes('fake')) {
            Swal.fire({
                icon: 'error',
                title: 'AI Image Detected',
                text: msg,
                footer: 'Please upload original photos taken by your device.'
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Oops!',
                text: msg
            });
        }
    }

    function validateForm() {
        let isValid = true;
        
        // Custom Bootstrap validation
        const fields = rescueForm.querySelectorAll('[required]');
        fields.forEach(field => {
            if (!field.value || field.value.trim() === '') {
                field.classList.add('is-invalid');
                isValid = false;
            } else {
                field.classList.remove('is-invalid');
            }
        });

        // Min length checks
        const animalType = document.getElementById('animalType');
        if (animalType.value.length < 2) {
            animalType.classList.add('is-invalid');
            isValid = false;
        }

        const address = document.getElementById('locationAddress');
        if (address.value.length < 5) {
            address.classList.add('is-invalid');
            isValid = false;
        }

        const description = document.getElementById('description');
        if (description.value.length < 10) {
            description.classList.add('is-invalid');
            isValid = false;
        }

        // Location checks
        const lat = document.getElementById('latitude').value;
        if (!lat) {
            Swal.fire({
                icon: 'warning',
                title: 'Missing Coordinates',
                text: 'Please use the "USE CURRENT LOCATION" button to pin the rescue spot so NGOs can find you.'
            });
            isValid = false;
        }

        // IMAGE VALIDATION (REQUIRED)
        if (selectedFiles.length === 0) {
            imageError.classList.remove('d-none');
            dropZone.classList.add('border-danger');
            showToast('At least one physical photo is required.', 'error');
            isValid = false;
        } else {
            imageError.classList.add('d-none');
            dropZone.classList.remove('border-danger');
        }

        return isValid;
    }

    function showToast(msg, icon = 'success') {
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true
        });
        Toast.fire({
            icon: icon,
            title: msg
        });
    }
});
