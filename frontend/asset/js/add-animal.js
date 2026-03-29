/**
 * Adoptify - Add Animal Page Logic
 * Handles multi-step form, image previews, and sequential API integration
 */

document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = 'http://localhost:8080/api';
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user'));

    // 1. Auth Guard
    if (!token || !user || (user.role !== 'REGULAR_USER' && user.role !== 'NGO')) {
        window.location.href = '../auth/login.html?redirect=animals/add-animal.html';
        return;
    }

    // Initialize Navigation (from main.js logic)
    initLocalNavigation();

    // 2. State & Elements
    let selectedFiles = []; // Array of { file, isPrimary }
    const form = document.getElementById('addAnimalForm');
    const uploadZone = document.getElementById('uploadZone');
    const fileInput = document.getElementById('fileInput');
    const previewContainer = document.getElementById('previewContainer');
    const charCount = document.getElementById('charCount');
    const description = form.querySelector('textarea[name="description"]');
    const submitBtn = document.getElementById('submitBtn');
    const btnSpinner = document.getElementById('btnSpinner');
    const btnText = document.getElementById('btnText');
    const successModal = new bootstrap.Modal(document.getElementById('successModal'));

    // 3. Image Upload Logic
    uploadZone.addEventListener('click', () => fileInput.click());

    uploadZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadZone.classList.add('dragover');
    });

    uploadZone.addEventListener('dragleave', () => uploadZone.classList.remove('dragover'));

    uploadZone.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadZone.classList.remove('dragover');
        handleFiles(e.dataTransfer.files);
    });

    fileInput.addEventListener('change', (e) => handleFiles(e.target.files));

    function handleFiles(files) {
        if (selectedFiles.length + files.length > 5) {
            Swal.fire('Limit Exceeded', 'You can upload up to 5 photos only.', 'warning');
            return;
        }

        Array.from(files).forEach(file => {
            if (!file.type.startsWith('image/')) return;
            if (file.size > 5 * 1024 * 1024) {
                Swal.fire('File Too Large', `${file.name} exceeds 5MB limit.`, 'error');
                return;
            }

            const isPrimary = selectedFiles.length === 0;
            selectedFiles.push({ file, isPrimary });
        });

        renderPreviews();
    }

    function renderPreviews() {
        previewContainer.innerHTML = '';
        selectedFiles.forEach((item, index) => {
            const card = document.createElement('div');
            card.className = `preview-card ${item.isPrimary ? 'is-primary' : ''}`;
            
            const reader = new FileReader();
            reader.onload = (e) => {
                card.innerHTML = `
                    <img src="${e.target.result}" alt="Preview">
                    ${item.isPrimary ? '<div class="primary-badge">Primary</div>' : ''}
                    <div class="preview-overlay">
                        <button type="button" class="btn-preview btn-star" onclick="setPrimary(${index})" title="Set as Primary">
                            <i class="fa-solid fa-star"></i>
                        </button>
                        <button type="button" class="btn-preview btn-delete" onclick="removePhoto(${index})" title="Delete">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                `;
            };
            reader.readAsDataURL(item.file);
            previewContainer.appendChild(card);
        });
    }

    window.setPrimary = (index) => {
        selectedFiles.forEach((f, i) => f.isPrimary = (i === index));
        renderPreviews();
    };

    window.removePhoto = (index) => {
        selectedFiles.splice(index, 1);
        if (selectedFiles.length > 0 && !selectedFiles.some(f => f.isPrimary)) {
            selectedFiles[0].isPrimary = true;
        }
        renderPreviews();
    };

    // 4. Description Char Count
    description.addEventListener('input', () => {
        const count = description.value.length;
        charCount.textContent = `${count} characters`;
        charCount.style.color = count < 50 ? '#ff4757' : '#2ed573';
    });

    // 5. Form Submission
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (selectedFiles.length === 0) {
            Swal.fire('Photos Required', 'Please upload at least one photo.', 'warning');
            return;
        }

        if (description.value.length < 50) {
            Swal.fire('Description Too Short', 'Description must be at least 50 characters.', 'warning');
            return;
        }

        setLoading(true);

        try {
            // Step 1: Create Animal
            const formDataJSON = new FormData(form);
            const animalData = {
                name: formDataJSON.get('name'),
                species: formDataJSON.get('species'),
                breed: formDataJSON.get('breed'),
                ageYears: parseInt(formDataJSON.get('ageYears')),
                ageMonths: parseInt(formDataJSON.get('ageMonths')),
                gender: formDataJSON.get('gender'),
                size: formDataJSON.get('size'),
                color: formDataJSON.get('color'),
                description: formDataJSON.get('description'),
                healthStatus: formDataJSON.get('healthStatus'),
                vaccinated: form.vaccinated.checked,
                dewormed: form.dewormed.checked,
                neutered: form.neutered.checked,
                specialNeeds: formDataJSON.get('specialNeeds'),
                behaviorNotes: formDataJSON.get('behaviorNotes'),
                goodWithKids: form.goodWithKids.checked,
                goodWithPets: form.goodWithPets.checked,
                adoptionFee: parseFloat(formDataJSON.get('adoptionFee') || 0),
                status: 'AVAILABLE'
            };

            const response = await fetch(`${API_BASE_URL}/animals`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(animalData)
            });

            if (!response.ok) throw new Error('Failed to create animal listing');

            const result = await response.json();
            const animalId = result.id;

            // Step 2: Upload Photos
            // We upload the primary photo first to ensure it's marked correctly on backend if it uses order
            const primaryIdx = selectedFiles.findIndex(f => f.isPrimary);
            const uploadQueue = [...selectedFiles];
            if (primaryIdx > -1) {
                const primary = uploadQueue.splice(primaryIdx, 1)[0];
                uploadQueue.unshift(primary);
            }

            for (const item of uploadQueue) {
                const imgFormData = new FormData();
                imgFormData.append('file', item.file);

                await fetch(`${API_BASE_URL}/animals/${animalId}/images`, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    },
                    body: imgFormData
                });
            }

            setLoading(false);
            successModal.show();

        } catch (error) {
            console.error('Error:', error);
            setLoading(false);
            Swal.fire('Error', error.message || 'Something went wrong. Please try again.', 'error');
        }
    });

    function setLoading(isLoading) {
        submitBtn.disabled = isLoading;
        btnSpinner.classList.toggle('d-none', !isLoading);
        btnText.textContent = isLoading ? 'PUBLISHING...' : 'PUBLISH LISTING';
    }

    function initLocalNavigation() {
        const userNameSpan = document.getElementById('user-name');
        const userSection = document.getElementById('user-section');
        const authSection = document.getElementById('auth-section');
        const logoutBtn = document.getElementById('logout-btn');

        if (token && user) {
            if (authSection) authSection.classList.add('d-none');
            if (userSection) userSection.classList.remove('d-none');
            if (userSection) userSection.classList.add('d-flex');
            if (userNameSpan) userNameSpan.textContent = user.fullName || 'User';
        }

        if (logoutBtn) {
            logoutBtn.addEventListener('click', (e) => {
                e.preventDefault();
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                window.location.href = '../index.html';
            });
        }
    }
});
