/**
 * Adoptify - Received Requests JS
 * Optimized for Premium Theme and Consistency with Sent Requests
 */

document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = 'http://localhost:8080/api';
    const token = localStorage.getItem('token');
    const userJson = localStorage.getItem('user');
    
    // Auth Guard
    if (!token || !userJson) {
        window.location.href = '../auth/login.html?redirect=animals/received-requests.html';
        return;
    }

    let currentStatus = 'ALL';
    let requestsData = [];

    // Selectors
    const filterBtns = document.querySelectorAll('.filter-btn');
    const requestsBody = document.getElementById('requestsBody');
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const listContent = document.getElementById('listContent');

    // 1. Initialize
    loadRequests();
    setupEventListeners();

    // 2. Setup Listeners
    function setupEventListeners() {
        filterBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                filterBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                currentStatus = btn.dataset.status;
                loadRequests();
            });
        });

        // Rejection Logic
        const confirmRejectBtn = document.getElementById('confirmReject');
        if (confirmRejectBtn) {
            confirmRejectBtn.addEventListener('click', () => {
                const id = document.getElementById('rejectId').value;
                const reason = document.getElementById('rejectionReason').value;
                if (!reason.trim()) {
                    Swal.fire('Error', 'Please provide a feedback reason.', 'error');
                    return;
                }
                processRejection(id, reason);
            });
        }
    }

    // 3. API Actions
    async function loadRequests() {
        toggleLoading(true);
        try {
            const statusParam = currentStatus === 'ALL' ? '' : `?status=${currentStatus}`;
            const response = await fetch(`${API_BASE_URL}/adoptions/received${statusParam}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) throw new Error('Failed to fetch requests');

            requestsData = await response.json();
            renderRequests(requestsData);
        } catch (error) {
            console.error('Error:', error);
            toggleState('empty');
        }
    }

    window.approveAdoption = async (id) => {
        try {
            const response = await fetch(`${API_BASE_URL}/adoptions/${id}/approve`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) throw new Error('Failed to approve request');

            Swal.fire('Approved!', 'The adoption request has been approved!', 'success');
            loadRequests();
            bootstrap.Modal.getInstance(document.getElementById('detailsModal'))?.hide();
        } catch (error) {
            Swal.fire('Error', error.message, 'error');
        }
    }

    async function processRejection(id, reason) {
        try {
            const response = await fetch(`${API_BASE_URL}/adoptions/${id}/reject`, {
                method: 'PUT',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ rejectionReason: reason })
            });

            if (!response.ok) throw new Error('Failed to reject request');

            bootstrap.Modal.getInstance(document.getElementById('rejectModal')).hide();
            bootstrap.Modal.getInstance(document.getElementById('detailsModal'))?.hide();
            Swal.fire('Rejected', 'The request has been rejected successfully.', 'success');
            loadRequests();
        } catch (error) {
            Swal.fire('Error', error.message, 'error');
        }
    }

    window.completeAdoption = async (id) => {
        const result = await Swal.fire({
            title: 'Complete Adoption?',
            text: 'Is the animal successfully with their new family?',
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Yes, Completed',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#F59E0B'
        });

        if (result.isConfirmed) {
            try {
                const response = await fetch(`${API_BASE_URL}/adoptions/${id}/complete`, {
                    method: 'PUT',
                    headers: { 'Authorization': `Bearer ${token}` }
                });

                if (!response.ok) throw new Error('Failed to complete adoption');

                Swal.fire('Brilliant!', 'We wish the animal a happy life!', 'success');
                loadRequests();
                bootstrap.Modal.getInstance(document.getElementById('detailsModal'))?.hide();
            } catch (error) {
                Swal.fire('Error', error.message, 'error');
            }
        }
    }

    // 4. Rendering logic
    function renderRequests(items) {
        if (!items || items.length === 0) {
            toggleState('empty');
            return;
        }

        toggleState('list');
        requestsBody.innerHTML = '';

        items.forEach(req => {
            const tr = document.createElement('tr');
            const animalImg = getFullImageUrl(req.animalImage) || '../asset/Images/placeholder-animal.png';
            const reqDate = new Date(req.createdAt).toLocaleDateString();
            
            tr.innerHTML = `
                <td data-label="Animal & Adopter">
                    <div class="animal-info-small">
                        <img src="${animalImg}" class="animal-img-small" alt="${req.animalName}">
                        <div>
                            <div class="animal-name-small">${req.animalName}</div>
                            <div class="animal-meta-small">
                                Adopter: <strong>${req.adopterName}</strong><br>
                                <span class="text-primary">${req.adopterEmail || '-'}</span>
                            </div>
                        </div>
                    </div>
                </td>
                <td data-label="Date">
                    <div class="fw-medium">${reqDate}</div>
                    <div class="small text-muted">Received</div>
                </td>
                <td data-label="Status">
                    <span class="status-badge badge-${req.requestStatus.toLowerCase()}">${req.requestStatus}</span>
                </td>
                <td data-label="Actions" class="text-end">
                    <div class="d-flex justify-content-end gap-2">
                        <button class="btn btn-sm btn-light border fw-bold rounded-3 px-3" onclick="openDetails(${req.id})">Details</button>
                    </div>
                </td>
            `;
            requestsBody.appendChild(tr);
        });
    }

    // 5. Modals Handlers
    window.openDetails = (id) => {
        const req = requestsData.find(r => r.id === id);
        if (!req) return;

        const content = document.getElementById('detailsModalContent');
        const footer = document.getElementById('detailsModalFooter');

        content.innerHTML = `
            <div class="row g-4">
                <div class="col-md-5">
                    <img src="${getFullImageUrl(req.animalImage) || '../asset/Images/placeholder-animal.png'}" class="img-fluid rounded-4 shadow-sm mb-3">
                    <div class="detail-section-title">Animal Details</div>
                    <div class="p-3 bg-light rounded-4 small">
                        <div class="detail-item">
                            <span class="detail-label">Name</span>
                            <div class="detail-value">${req.animalName}</div>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">Breed</span>
                            <div class="detail-value">${req.animalBreed || '-'}</div>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">Age</span>
                            <div class="detail-value">${req.animalAge}</div>
                        </div>
                    </div>
                </div>
                <div class="col-md-7">
                    <div class="detail-section-title">Adoption Form Information</div>
                    <div class="p-3 border rounded-4 mb-3 small lh-base">
                        <div class="mb-3">
                            <span class="detail-label">Reason for adoption</span>
                            <p class="mb-0 fw-medium">${req.reasonForAdoption || '-'}</p>
                        </div>
                        <div class="mb-3">
                            <span class="detail-label">Living Situation</span>
                            <p class="mb-0 fw-medium">${req.livingSituation || '-'}</p>
                        </div>
                        <div class="row g-3">
                            <div class="col-6">
                                <span class="detail-label">Has Yard</span>
                                <div class="fw-bold text-${req.hasYard ? 'success' : 'danger'}">${req.hasYard ? 'Yes' : 'No'}</div>
                            </div>
                            <div class="col-6">
                                <span class="detail-label">Other Pets</span>
                                <div class="fw-bold">${req.hasOtherPets ? 'Yes' : 'No'}</div>
                            </div>
                            <div class="col-6">
                                <span class="detail-label">Experience</span>
                                <div class="fw-bold">${req.hasExperience ? 'Yes' : 'No'}</div>
                            </div>
                            <div class="col-6">
                                <span class="detail-label">Daily Alone Time</span>
                                <div class="fw-bold">${req.dailyHoursAlone} hrs</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="detail-section-title">Adopter Contact</div>
                    <div class="p-3 bg-light rounded-4 small">
                        <div><span class="detail-label">Full Name:</span> <span class="fw-bold">${req.adopterName}</span></div>
                        <div><span class="detail-label">Email:</span> <span class="fw-bold text-primary">${req.adopterEmail || '-'}</span></div>
                        <div><span class="detail-label">Phone:</span> <span class="fw-bold text-success">${req.adopterPhone || '-'}</span></div>
                    </div>
                </div>
            </div>
            ${req.rejectionReason ? `
                <div class="mt-4 p-3 badge-rejected border-0 rounded-4">
                    <div class="detail-label text-danger mb-1">REJECTION FEEDBACK</div>
                    <div class="fw-bold">${req.rejectionReason}</div>
                </div>
            ` : ''}
        `;

        footer.innerHTML = '';
        if (req.requestStatus === 'PENDING') {
            footer.innerHTML = `
                <button class="btn btn-light fw-bold px-4 rounded-pill" onclick="openRejectModal(${req.id})">Reject</button>
                <button class="btn btn-brand px-4 rounded-pill text-white fw-bold" onclick="approveAdoption(${req.id})">Approve & Confirm</button>
            `;
        } else if (req.requestStatus === 'APPROVED') {
            footer.innerHTML = `
                <button class="btn btn-outline-brand fw-bold px-4 rounded-pill" onclick="openContactModal(${req.id})">Coordinate Info</button>
                <button class="btn btn-brand px-4 rounded-pill text-white fw-bold" onclick="completeAdoption(${req.id})">Mark as Adopted</button>
            `;
        }

        const modal = new bootstrap.Modal(document.getElementById('detailsModal'));
        modal.show();
    };

    window.openRejectModal = (id) => {
        document.getElementById('rejectId').value = id;
        document.getElementById('rejectionReason').value = '';
        new bootstrap.Modal(document.getElementById('rejectModal')).show();
    };

    window.openContactModal = (id) => {
        const req = requestsData.find(r => r.id === id);
        if (!req) return;

        const content = document.getElementById('contactModalContent');
        content.innerHTML = `
            <div class="p-4">
                <div class="mb-4">
                    <div class="bg-light d-inline-block p-4 rounded-circle mb-3">
                        <i class="fa-solid fa-address-book fa-3x text-brand"></i>
                    </div>
                    <h3 class="fw-black">${req.adopterName}</h3>
                    <p class="text-muted">Coordinate the animal's new journey</p>
                </div>
                <div class="d-grid gap-3 mb-4">
                    <div class="bg-light p-3 rounded-4 d-flex justify-content-between align-items-center">
                        <div class="text-start">
                            <div class="detail-label">Email</div>
                            <div class="fw-bold">${req.adopterEmail}</div>
                        </div>
                        <button class="btn btn-sm btn-white border" onclick="copyText('${req.adopterEmail}')">Copy</button>
                    </div>
                    <div class="bg-light p-3 rounded-4 d-flex justify-content-between align-items-center">
                        <div class="text-start">
                            <div class="detail-label">Phone</div>
                            <div class="fw-bold">${req.adopterPhone}</div>
                        </div>
                        <button class="btn btn-sm btn-white border" onclick="copyText('${req.adopterPhone}')">Copy</button>
                    </div>
                </div>
            </div>
        `;
        new bootstrap.Modal(document.getElementById('contactModal')).show();
    };

    window.copyText = (txt) => {
        navigator.clipboard.writeText(txt).then(() => {
            Swal.fire({
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 1500,
                icon: 'success',
                title: 'Copied to clipboard'
            });
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

    function toggleLoading(isLoading) {
        if (isLoading) toggleState('loading');
    }
});
