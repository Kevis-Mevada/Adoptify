/**
 * Adoptify - Browse Animals JavaScript
 * Implements full specification including search, grid, modal tabs, and adoption form.
 */

const API_BASE_URL = 'http://localhost:8080/api';
let currentPage = 0;
const pageSize = 12;
let currentAnimal = null;

document.addEventListener('DOMContentLoaded', () => {
    // 1. Initial Load
    loadAnimals();

    // 2. Event Listeners
    setupEventListeners();

    // 3. Initialize Bootstrap Modals
    const animalModalEl = document.getElementById('animalModal');
    if (animalModalEl) {
        window.animalModal = new bootstrap.Modal(animalModalEl);
    }
    
    // Check Auth State for Navbar
    updateHeaderAuth();
});

/**
 * Fetch animals from API with search and pagination
 */
async function loadAnimals() {
    showLoadingSkeleton();
    
    const search = document.getElementById('searchInput').value;
    const location = document.getElementById('locationInput').value;
    const sort = document.getElementById('sortBy').value;
    
    const params = new URLSearchParams({
        page: currentPage,
        size: pageSize,
        sortBy: sort.split(',')[0],
        direction: sort.split(',')[1] || 'desc'
    });
    
    if (search) params.append('search', search);
    if (location) params.append('location', location);

    try {
        const response = await fetch(`${API_BASE_URL}/animals?${params.toString()}`);
        if (!response.ok) throw new Error('Failed to fetch animals');
        
        const data = await response.json();
        renderAnimalGrid(data.content);
        updatePagination(data.totalPages, data.number);
        
        const countEl = document.getElementById('resultsCount');
        if (countEl) countEl.textContent = `Showing ${data.totalElements} animals`;
    } catch (error) {
        console.error('Error loading animals:', error);
        showEmptyState();
    }
}

/**
 * Render animal cards in grid
 */
function renderAnimalGrid(animals) {
    const grid = document.getElementById('animalGrid');
    const emptyState = document.getElementById('emptyState');
    
    grid.innerHTML = '';
    
    if (!animals || animals.length === 0) {
        emptyState.classList.remove('d-none');
        return;
    }

    emptyState.classList.add('d-none');

    animals.forEach(animal => {
        const col = document.createElement('div');
        col.className = 'col-xl-3 col-lg-4 col-md-6 col-12 mb-4';
        
        const rawImageUrl = animal.images && animal.images.length > 0 
            ? animal.images.find(img => img.isPrimary)?.imageUrl || animal.images[0].imageUrl 
            : null;
        const imageUrl = getFullImageUrl(rawImageUrl) || 'https://images.unsplash.com/photo-1543852786-1cf6624b9987?q=80&w=600&auto=format&fit=crop';

        const totalAgeMonths = (animal.ageYears * 12) + (animal.ageMonths || 0);
        const formattedPrice = new Intl.NumberFormat('en-IN', {
            style: 'currency', currency: 'INR', maximumFractionDigits: 0
        }).format(animal.adoptionFee);

        col.innerHTML = `
            <div class="animal-card">
                <div class="card-img-wrapper">
                    <span class="status-badge status-${animal.status.toLowerCase()}">${animal.status}</span>
                    <img src="${imageUrl}" alt="${animal.name}" loading="lazy">
                </div>
                <div class="card-content">
                    <h4 class="animal-name">${animal.name}</h4>
                    <p class="animal-breed">${animal.breed || animal.species}</p>
                    <div class="animal-meta">
                        <span><i class="fa-solid fa-cake-candles"></i> ${formatAge(totalAgeMonths)}</span>
                        <span><i class="fa-solid fa-${animal.gender.toLowerCase() === 'male' ? 'mars' : 'venus'}"></i> ${animal.gender}</span>
                    </div>
                    <div class="animal-meta">
                        <span><i class="fa-solid fa-location-dot"></i> ${animal.owner ? animal.owner.city : 'Unknown'}</span>
                    </div>
                    <p class="adoption-fee-tag mb-4">${formattedPrice}</p>
                    <div class="d-grid gap-2">
                        <button class="btn btn-outline-dark rounded-3 fw-bold" onclick="openAnimalModal(${animal.id}, 'view')">More Information</button>
                        <button class="btn btn-brand rounded-3 fw-bold" onclick="openAnimalModal(${animal.id}, 'adopt')">Adopt Now</button>
                    </div>
                </div>
            </div>
        `;
        grid.appendChild(col);
    });
}

/**
 * Modal Management
 */
window.openAnimalModal = async function(animalId, mode) {
    try {
        // 1. Fetch details
        const response = await fetch(`${API_BASE_URL}/animals/${animalId}`);
        if (!response.ok) throw new Error('Animal not found');
        currentAnimal = await response.json();

        // 2. Check Eligibility if adopting
        if (mode === 'adopt') {
            const eligibility = await checkAdoptionEligibility(animalId);
            if (!eligibility.canAdopt) {
                alert(eligibility.message);
                // Even if cannot adopt, we can still show the "About" tab
                mode = 'view'; 
            }
        }

        // 3. Populate
        populateModal(currentAnimal);

        // 4. Switch to correct tab
        const tabTriggerEl = document.querySelector(mode === 'adopt' ? '#form-tab' : '#about-tab');
        bootstrap.Tab.getOrCreateInstance(tabTriggerEl).show();

        // 5. Show modal
        window.animalModal.show();
    } catch (error) {
        alert(error.message);
    }
};

function populateModal(animal) {
    const setT = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val || '-';
    };

    const adIdEl = document.getElementById('adIdInForm');
    if (adIdEl) adIdEl.value = animal.id;
    
    setT('modalName', animal.name);
    setT('modalBreedName', animal.breed || animal.species);
    setT('modalPrice', `₹${animal.adoptionFee}`);
    
    // Details Grid Items
    if (animal.owner) setT('m-owner', animal.owner.fullName);
    setT('m-species', animal.species);
    setT('m-breed', animal.breed);
    setT('m-age', formatAge((animal.ageYears * 12) + (animal.ageMonths || 0)));
    setT('m-gender', animal.gender);
    setT('m-size', animal.size);
    setT('m-color', animal.color);
    if (animal.owner) setT('m-location', animal.owner.city);
    setT('m-vac', animal.vaccinated ? 'Yes' : 'No');
    setT('m-dew', animal.dewormed ? 'Yes' : 'No');
    setT('m-neu', animal.neutered ? 'Yes' : 'No');
    setT('m-desc', animal.description);

    // Badges
    const badgeContainer = document.getElementById('healthBadges');
    if (badgeContainer) {
        badgeContainer.innerHTML = '';
        const addBadge = (text, icon) => {
            const span = document.createElement('span');
            span.className = 'badge-health me-2 mb-2';
            span.innerHTML = `<i class="fa-solid fa-${icon} me-1 text-success"></i> ${text}`;
            badgeContainer.appendChild(span);
        };
        if (animal.vaccinated) addBadge('Vaccinated', 'check-circle');
        if (animal.dewormed) addBadge('Dewormed', 'check-circle');
        if (animal.neutered) addBadge('Neutered', 'check-circle');
        if (animal.goodWithKids) addBadge('Good with Kids', 'child');
        if (animal.goodWithPets) addBadge('Good with Pets', 'paw');
    }

    // Gallery
    const mainImg = document.getElementById('modalMainImg');
    const thumbStrip = document.getElementById('modalThumbnails');
    const statusBadge = document.getElementById('modalStatusBadge');

    if (statusBadge) {
        statusBadge.textContent = animal.status;
        statusBadge.className = `modal-status-badge status-${animal.status.toLowerCase()}`;
    }

    const primaryImg = animal.images && animal.images.length > 0 
        ? animal.images.find(img => img.isPrimary) || animal.images[0]
        : null;

    if (mainImg) {
        mainImg.src = primaryImg ? getFullImageUrl(primaryImg.imageUrl) : 'https://images.unsplash.com/photo-1543852786-1cf6624b9987?q=80&w=800&auto=format&fit=crop';
    }

    if (thumbStrip) {
        thumbStrip.innerHTML = '';
        if (animal.images && animal.images.length > 0) {
            animal.images.forEach((img, idx) => {
                const thumb = document.createElement('img');
                const fullThumbUrl = getFullImageUrl(img.imageUrl);
                thumb.src = fullThumbUrl;
                thumb.alt = `Thumb ${idx}`;
                if (mainImg && fullThumbUrl === mainImg.src) thumb.classList.add('active');
                thumb.onclick = () => {
                    if (mainImg) mainImg.src = fullThumbUrl;
                    document.querySelectorAll('.thumbnail-strip img').forEach(t => t.classList.remove('active'));
                    thumb.classList.add('active');
                };
                thumbStrip.appendChild(thumb);
            });
        }
    }

    // Prefill User Form
    prefillUserData();
}

/**
 * Adoption Eligibility & Prefill
 */
async function checkAdoptionEligibility(animalId) {
    const token = localStorage.getItem('token');
    if (!token) {
        return { canAdopt: false, message: 'Please login to submit an adoption request.' };
    }

    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (currentAnimal.owner && currentAnimal.owner.id === user.id) {
        return { canAdopt: false, message: 'You cannot adopt your own animal.' };
    }

    // In a real app, you'd check API for existing requests
    // For now, we allow.
    return { canAdopt: true };
}

function prefillUserData() {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const form = document.getElementById('adoptionRequestForm');
    const loginMsg = document.getElementById('logged-out-msg');

    if (!user.id) {
        form.classList.add('d-none');
        loginMsg.classList.remove('d-none');
        return;
    }

    form.classList.remove('d-none');
    loginMsg.classList.add('d-none');

    document.getElementById('adName').value = user.fullName || '';
    document.getElementById('adEmail').value = user.email || '';
}

/**
 * Form Handling & Conditional Fields
 */
window.togglePETS = (show) => {
    document.getElementById('petsDetailContainer').classList.toggle('d-none', !show);
};
window.toggleEXP = (show) => {
    document.getElementById('expDetailContainer').classList.toggle('d-none', !show);
};

document.getElementById('adoptionRequestForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = e.target.querySelector('button[type="submit"]');
    
    // Simple Validation
    const phone = e.target.adopterContactPhone.value;
    if (!/^\d{10}$/.test(phone)) {
        alert('Please enter a valid 10-digit phone number.');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Submitting...';

    const formData = new FormData(e.target);
    const payload = Object.fromEntries(formData.entries());
    
    // Casting
    payload.hasYard = payload.hasYard === 'true';
    payload.hasOtherPets = payload.hasOtherPets === 'true';
    payload.hasExperience = payload.hasExperience === 'true';
    payload.animalId = parseInt(payload.animalId);
    payload.dailyHoursAlone = parseInt(payload.dailyHoursAlone);

    try {
        const response = await fetch(`${API_BASE_URL}/adoptions/request`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.message || 'Failed to submit request');
        }

        window.animalModal.hide();
        new bootstrap.Modal(document.getElementById('successModal')).show();
    } catch (error) {
        alert(error.message);
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'SUBMIT ADOPTION REQUEST';
    }
});

/**
 * Utilities & Listeners
 */
function setupEventListeners() {
    document.getElementById('searchBtn').addEventListener('click', () => {
        currentPage = 0;
        loadAnimals();
    });

    document.getElementById('sortBy').addEventListener('change', () => {
        currentPage = 0;
        loadAnimals();
    });

    // Date Restriction
    const dateInput = document.getElementById('meetingDate');
    if (dateInput) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        dateInput.min = tomorrow.toISOString().split('T')[0];
    }
}

window.resetSearch = () => {
    document.getElementById('searchInput').value = '';
    document.getElementById('locationInput').value = '';
    currentPage = 0;
    loadAnimals();
};

window.changePage = (p) => {
    currentPage = p;
    loadAnimals();
    window.scrollTo({ top: 0, behavior: 'smooth' });
};

function updatePagination(totalPages, current) {
    const container = document.getElementById('pagination');
    if (!container || totalPages <= 1) {
        if (container) container.innerHTML = '';
        return;
    }

    let html = `<ul class="pagination">
        <li class="page-item ${current === 0 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="changePage(${current - 1})">Previous</a>
        </li>`;

    for (let i = 0; i < totalPages; i++) {
        html += `<li class="page-item ${i === current ? 'active' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="changePage(${i})">${i + 1}</a>
        </li>`;
    }

    html += `<li class="page-item ${current === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="changePage(${current + 1})">Next</a>
        </li></ul>`;

    container.innerHTML = html;
}

function formatAge(ageInMonths) {
    if (ageInMonths < 1) return 'Newborn';
    if (ageInMonths < 12) return `${ageInMonths} Mo`;
    const years = Math.floor(ageInMonths / 12);
    const months = ageInMonths % 12;
    return months > 0 ? `${years}y ${months}m` : `${years}y`;
}

function showLoadingSkeleton() {
    const grid = document.getElementById('animalGrid');
    if (!grid) return;
    grid.innerHTML = '';
    for (let i = 0; i < 8; i++) {
        const col = document.createElement('div');
        col.className = 'col-xl-3 col-lg-4 col-md-6 col-12 mb-4';
        col.innerHTML = `
            <div class="animal-card">
                <div class="card-img-wrapper skeleton"></div>
                <div class="card-content">
                    <div class="skeleton mb-3" style="height: 24px; width: 60%"></div>
                    <div class="skeleton mb-2" style="height: 16px; width: 40%"></div>
                    <div class="skeleton mb-4" style="height: 20px; width: 30%"></div>
                    <div class="d-grid gap-2">
                        <div class="skeleton" style="height: 40px; border-radius: 8px"></div>
                        <div class="skeleton" style="height: 40px; border-radius: 8px"></div>
                    </div>
                </div>
            </div>
        `;
        grid.appendChild(col);
    }
}

function showEmptyState() {
    const grid = document.getElementById('animalGrid');
    if (grid) grid.innerHTML = '';
    document.getElementById('emptyState').classList.remove('d-none');
    document.getElementById('pagination').innerHTML = '';
}

function updateHeaderAuth() {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const authSect = document.getElementById('auth-section');
    const userSect = document.getElementById('user-section');
    
    if (token && user.id) {
            if (authSect) authSect.classList.add('d-none');
            if (userSect) {
                userSect.classList.remove('d-none');
                document.getElementById('user-name').textContent = user.fullName || 'User';
            }
    }

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            localStorage.clear();
            window.location.reload();
        });
    }
}
