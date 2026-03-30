/**
 * Adoptify - Listed Animals Management JS
 */

document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = 'http://localhost:8080/api';
    const token = localStorage.getItem('token');
    const userJson = localStorage.getItem('user');

    // Auth Guard
    if (!token || !userJson) {
        window.location.href = '../auth/login.html?redirect=animals/listed-animals.html';
        return;
    }

    let currentFilter = 'ALL';
    let animalsData = [];

    // Selectors
    const filterBtns = document.querySelectorAll('.filter-btn');
    const animalsBody = document.getElementById('animalsBody');
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const listContent = document.getElementById('listContent');

    // 1. Init
    loadMyAnimals();
    setupEventListeners();

    // 2. Event Listeners
    function setupEventListeners() {
        filterBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                filterBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                currentFilter = btn.dataset.status;
                renderAnimals(animalsData);
            });
        });
    }

    // 3. API Actions
    async function loadMyAnimals() {
        toggleState('loading');
        try {
            const response = await fetch(`${API_BASE_URL}/animals/my-animals`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.status === 401) {
                localStorage.removeItem('token');
                window.location.href = '../auth/login.html?redirect=animals/listed-animals.html';
                return;
            }

            if (!response.ok) throw new Error('Failed to fetch your animals');

            animalsData = await response.json();
            renderAnimals(animalsData);
        } catch (error) {
            console.error('Error:', error);
            toggleState('empty');
        }
    }

    window.deleteAnimal = async (id) => {
        const result = await Swal.fire({
            title: 'Delete listing?',
            text: 'Are you sure? This animal will be removed from Adoptify permanently.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, Delete',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#dc3545'
        });

        if (result.isConfirmed) {
            try {
                const response = await fetch(`${API_BASE_URL}/animals/${id}`, {
                    method: 'DELETE',
                    headers: { 'Authorization': `Bearer ${token}` }
                });

                if (!response.ok) throw new Error('Deletion failed');

                Swal.fire('Deleted', 'Listing removed successfully.', 'success');
                loadMyAnimals();
            } catch (error) {
                Swal.fire('Error', error.message, 'error');
            }
        }
    }

    // 4. Rendering logic
    function renderAnimals(data) {
        let filtered = data;
        if (currentFilter !== 'ALL') {
            filtered = data.filter(a => a.status === currentFilter);
        }

        if (filtered.length === 0) {
            toggleState('empty');
            return;
        }

        toggleState('list');
        animalsBody.innerHTML = '';

        filtered.forEach(animal => {
            const tr = document.createElement('tr');
            const primaryImg = animal.images && animal.images.find(i => i.isPrimary) || (animal.images && animal.images[0]);
            const imgUrl = getFullImageUrl(primaryImg ? primaryImg.imageUrl : null) || '../asset/Images/placeholder-animal.png';
            const date = new Date(animal.createdAt).toLocaleDateString();

            tr.innerHTML = `
                <td data-label="Animal Details">
                    <div class="animal-info-small">
                        <img src="${imgUrl}" class="animal-img-small" alt="${animal.name}">
                        <div>
                            <div class="animal-name-small">${animal.name}</div>
                            <div class="animal-meta-small">${animal.species} • ${animal.breed || 'Rescue'}</div>
                        </div>
                    </div>
                </td>
                <td data-label="Date Posted">
                    <div class="small fw-medium text-muted">${date}</div>
                </td>
                <td data-label="Status">
                    <span class="status-badge badge-${animal.status.toLowerCase()}">${animal.status}</span>
                </td>
                <td data-label="Views">
                    <div class="view-stat"><i class="fa-regular fa-eye me-1"></i> ${animal.viewsCount || 0}</div>
                </td>
                <td data-label="Actions" class="text-end">
                    <div class="d-flex justify-content-end gap-2">
                        <a href="received-requests.html" class="btn btn-sm btn-white border fw-bold rounded-pill px-3">Requests</a>
                        <button class="btn btn-sm btn-light border text-danger fw-bold rounded-pill px-3" onclick="deleteAnimal(${animal.id})"><i class="fa-solid fa-trash-can"></i></button>
                    </div>
                </td>
            `;
            animalsBody.appendChild(tr);
        });
    }

    // Helpers
    function toggleState(state) {
        loadingState.classList.add('d-none');
        emptyState.classList.add('d-none');
        listContent.classList.add('d-none');

        if (state === 'loading') loadingState.classList.remove('d-none');
        else if (state === 'empty') emptyState.classList.remove('d-none');
        else if (state === 'list') listContent.classList.remove('d-none');
    }
});
