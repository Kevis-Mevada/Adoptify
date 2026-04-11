// NGO Dashboard Logic
document.addEventListener('DOMContentLoaded', () => {
    // Check Auth
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user'));

    if (!token || !user || user.role !== 'NGO') {
        Swal.fire({
            icon: 'error',
            title: 'Unauthorized',
            text: 'You do not have access to this area.'
        }).then(() => {
            window.location.href = '../index.html';
        });
        return;
    }

    const fetchProfile = async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/auth/profile`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (res.ok) {
                const latestUser = await res.json();
                localStorage.setItem('user', JSON.stringify(latestUser));
                updateVerificationUI(latestUser);
            }
        } catch (e) { console.error("Profile refresh failed", e); }
    };

    const updateVerificationUI = (u) => {
        const vStatus = document.getElementById('verification-status');
        const vMsg = document.getElementById('verification-msg');
        
        if (u.isVerified) {
            if (vStatus) vStatus.classList.remove('d-none');
            if (vMsg) vMsg.classList.add('d-none');
        } else {
            if (vStatus) vStatus.classList.add('d-none');
            if (vMsg) vMsg.classList.remove('d-none');
        }
    };

    // Initial UI check
    updateVerificationUI(user);
    // Refresh from server
    fetchProfile();

    // Initial Load
    loadStats();
    loadNearbyAlerts();
    loadAcceptedRescues();
    loadHistory();

    // Tab Listeners
    document.getElementById('alerts-tab').addEventListener('click', loadNearbyAlerts);
    document.getElementById('accepted-tab').addEventListener('click', loadAcceptedRescues);
    document.getElementById('history-tab').addEventListener('click', loadHistory);
});

const API_BASE_URL = 'http://localhost:8080/api';
const token = localStorage.getItem('token');
const user = JSON.parse(localStorage.getItem('user'));

// 1. STATS
async function loadStats() {
    try {
        const response = await fetch(`${API_BASE_URL}/rescue/stats`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const stats = await response.json();
        
        document.getElementById('stat-total-accepted').innerText = stats.totalAccepted || 0;
        document.getElementById('stat-completed').innerText = stats.completedCount || 0;
        document.getElementById('stat-pending').innerText = stats.pendingAlerts || 0;
        document.getElementById('stat-rating').innerText = `${stats.averageRating || '0.0'} ★`;
    } catch (error) {
        console.error("Stats load error", error);
    }
}

// 2. NEARBY ALERTS
async function loadNearbyAlerts() {
    const container = document.getElementById('alertsContainer');
    container.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-warning"></div></div>';

    try {
        // Use 0 as fallback to trigger global/all alerts if profile location is missing
        const lat = user.latitude || 0;
        const lng = user.longitude || 0;

        const response = await fetch(`${API_BASE_URL}/rescue/nearby?lat=${lat}&lng=${lng}&radius=50`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const alerts = await response.json();

        if (alerts.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5 bg-white rounded-4 border">
                    <i class="fa-solid fa-bell-slash fs-1 text-muted mb-3"></i>
                    <p class="text-muted fw-bold">No rescue alerts in your area</p>
                </div>`;
            return;
        }

        container.innerHTML = alerts.map(a => `
            <div class="rescue-card animate-in">
                <span class="emergency-badge badge-${a.emergencyLevel.toLowerCase()}">${a.emergencyLevel}</span>
                <div class="row g-4">
                    <div class="col-md-8">
                        <div class="d-flex align-items-center gap-3 mb-3">
                            <div class="bg-warning-subtle p-3 rounded-circle text-warning fs-4">
                                <i class="fa-solid fa-dog"></i>
                            </div>
                            <div>
                                <h5 class="fw-bold m-0">${a.animalType} (${a.animalCount})</h5>
                                <p class="text-muted small m-0"><i class="fa-solid fa-location-dot me-1"></i> ${a.locationAddress}</p>
                            </div>
                        </div>
                        <p class="small text-muted mb-3">${a.description}</p>
                        <div class="d-flex gap-4 small fw-bold text-muted">
                            <span><i class="fa-solid fa-house-medical me-1"></i> ${a.animalCondition}</span>
                            <span><i class="fa-solid fa-user me-1"></i> ${a.reporterName}</span>
                            <span><i class="fa-solid fa-calendar me-1"></i> ${new Date(a.createdAt).toLocaleDateString()}</span>
                        </div>
                    </div>
                    <div class="col-md-4 d-flex flex-column justify-content-center gap-2">
                        <button class="btn btn-warning py-2 fw-bold" onclick="openAcceptModal(${a.id}, '${a.animalType}')" ${!user.isVerified ? 'disabled' : ''}>ACCEPT MISSION</button>
                        <button class="btn btn-outline-danger py-2 fw-bold" onclick="openRejectModal(${a.id}, '${a.animalType}')">REJECT</button>
                        <button class="btn btn-light py-2 fw-bold" onclick="viewDetails(${a.id})">VIEW DETAILS</button>
                    </div>
                </div>
            </div>
        `).join('');

    } catch (error) {
        container.innerHTML = '<p class="text-danger text-center py-5">Failed to load alerts.</p>';
    }
}

// 3. ACCEPTED MISSIONS
async function loadAcceptedRescues() {
    const container = document.getElementById('acceptedContainer');
    container.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-warning"></div></div>';

    try {
        const response = await fetch(`${API_BASE_URL}/rescue/my-accepted?status=ACCEPTED`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const accepted = await response.json();

        if (accepted.length === 0) {
            container.innerHTML = '<div class="text-center py-5 bg-white rounded-4 border"><p class="text-muted fw-bold">No active missions</p></div>';
            return;
        }

        container.innerHTML = accepted.map(res => {
            const a = res.rescueReport;
            if (!a) return '';

            const statusClass = a.status === 'EN_ROUTE' ? 'bg-primary' : (a.status === 'COMPLETED' ? 'bg-success' : 'bg-info');
            return `
                <div class="rescue-card border-warning">
                    <div class="row g-4">
                        <div class="col-md-8">
                            <div class="d-flex gap-2 mb-2">
                                <span class="status-badge ${statusClass} text-white">${a.status}</span>
                                <span class="status-badge bg-light text-dark">ETA: ${new Date(res.estimatedArrivalTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                            </div>
                            <h5 class="fw-bold mb-2">${a.animalType} Rescue</h5>
                            <p class="small text-muted mb-2"><i class="fa-solid fa-location-dot text-danger me-1"></i> ${a.locationAddress}</p>
                            <p class="x-small text-muted mb-0"><b>Reporter:</b> ${a.reporterName} | <b>Phone:</b> ${a.reporterPhone}</p>
                        </div>
                        <div class="col-md-4 d-flex flex-column justify-content-center gap-2">
                            ${a.status === 'ASSIGNED' ? 
                                `<button class="btn btn-primary btn-sm fw-bold py-2" onclick="updateStatus(${a.id}, 'EN_ROUTE')">START RESCUE <i class="fa-solid fa-truck-fast ms-1"></i></button>` : 
                                `<button class="btn btn-success btn-sm fw-bold py-2" onclick="updateStatus(${a.id}, 'COMPLETED')">MARK AS COMPLETED <i class="fa-solid fa-check-circle ms-1"></i></button>`
                            }
                            <div class="d-flex gap-2">
                                <a href="https://www.google.com/maps/dir/?api=1&destination=${a.latitude},${a.longitude}" target="_blank" class="btn btn-light btn-sm flex-fill fw-bold"><i class="fa-solid fa-map-location-dot"></i> MAP</a>
                                <button class="btn btn-light btn-sm flex-fill fw-bold" onclick="viewDetails(${a.id})">DETAILS</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

    } catch (error) {
        container.innerHTML = '<p class="text-danger text-center py-5">Failed to load missions.</p>';
    }
}

// 4. HISTORY
async function loadHistory() {
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4"><div class="spinner-border spinner-border-sm text-warning"></div></td></tr>';

    try {
        const response = await fetch(`${API_BASE_URL}/rescue/my-accepted?status=COMPLETED`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const history = await response.json();

        if (history.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted">No history available</td></tr>';
            return;
        }

        tbody.innerHTML = history.map(res => {
            const r = res.rescueReport;
            if (!r) return '';
            return `
            <tr>
                <td class="ps-3 fw-bold">#${r.id}</td>
                <td><div class="fw-bold">${r.animalType}</div><div class="x-small text-muted">${r.animalCondition}</div></td>
                <td class="text-truncate" style="max-width: 200px;">${r.locationAddress}</td>
                <td>${new Date(res.updatedAt).toLocaleDateString()}</td>
                <td><span class="text-warning">${r.rating ? '★'.repeat(r.rating) : 'Unrated'}</span></td>
                <td class="pe-3 text-center">
                    <button class="btn btn-light btn-sm rounded-pill px-3 py-1 fw-bold fs-x-small" onclick="viewDetails(${r.id})">VIEW</button>
                </td>
            </tr>
        `;
        }).join('');

    } catch (error) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Failed to load history</td></tr>';
    }
}

// ACTION LOGIC
let selectedRescueId = null;
const acceptModal = new bootstrap.Modal(document.getElementById('acceptModal'));
const rejectModal = new bootstrap.Modal(document.getElementById('rejectModal'));
const detailsModal = new bootstrap.Modal(document.getElementById('detailsModal'));

function openAcceptModal(id, type) {
    selectedRescueId = id;
    document.getElementById('acceptSummary').innerText = `Accepting mission for: ${type}`;
    acceptModal.show();
}

async function confirmAccept() {
    const eta = document.getElementById('etaInput').value;
    if (!eta) return Swal.fire('Error', 'Please provide estimated arrival time', 'error');

    try {
        const response = await fetch(`${API_BASE_URL}/rescue/${selectedRescueId}/accept`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ estimatedArrivalTime: eta })
        });
        if (response.ok) {
            Swal.fire('Mission Accepted!', 'Good luck with the rescue.', 'success');
            acceptModal.hide();
            loadNearbyAlerts();
            loadAcceptedRescues();
            loadStats();
        }
    } catch (error) {
        Swal.fire('Error', 'Failed to accept mission', 'error');
    }
}

function openRejectModal(id, type) {
    selectedRescueId = id;
    document.getElementById('rejectReason').value = '';
    rejectModal.show();
}

async function confirmReject() {
    const reason = document.getElementById('rejectReason').value;
    if (!reason) return Swal.fire('Error', 'Please provide a reason', 'error');

    try {
        const response = await fetch(`${API_BASE_URL}/rescue/${selectedRescueId}/reject`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ reason })
        });
        if (response.ok) {
            Swal.fire('Mission Rejected', 'Alert removed from your list', 'info');
            rejectModal.hide();
            loadNearbyAlerts();
        }
    } catch (error) {
        Swal.fire('Error', 'Failed to reject mission', 'error');
    }
}

async function updateStatus(id, status) {
    try {
        const response = await fetch(`${API_BASE_URL}/rescue/${id}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ status })
        });
        if (response.ok) {
            Swal.fire('Status Updated', `Rescue marked as ${status}`, 'success');
            loadAcceptedRescues();
            if (status === 'COMPLETED') {
                loadHistory();
                loadStats();
            }
        }
    } catch (error) {
        Swal.fire('Error', 'Failed to update status', 'error');
    }
}

async function viewDetails(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/rescue/${id}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const r = await response.json();
        
        const content = `
            <div class="row g-4">
                <div class="col-md-6 border-end">
                    <h6 class="fw-bold mb-3">Rescue Information</h6>
                    <p class="mb-2 small"><b>Type:</b> ${r.animalType}</p>
                    <p class="mb-2 small"><b>Count:</b> ${r.animalCount}</p>
                    <p class="mb-2 small"><b>Condition:</b> ${r.animalCondition}</p>
                    <p class="mb-2 small"><b>Emergency:</b> <span class="badge bg-warning text-dark">${r.emergencyLevel}</span></p>
                    <hr>
                    <h6 class="fw-bold mb-2">Description:</h6>
                    <p class="small text-muted">"${r.description}"</p>
                </div>
                <div class="col-md-6">
                    <h6 class="fw-bold mb-3">Reporter & Location</h6>
                    <p class="mb-2 small"><b>Name:</b> ${r.reporterName}</p>
                    <p class="mb-2 small"><b>Phone:</b> ${r.reporterPhone}</p>
                    <p class="mb-2 small"><b>Address:</b> ${r.locationAddress}</p>
                    <p class="mb-2 small"><b>Landmark:</b> ${r.landmark || 'N/A'}</p>
                    <div class="mt-4">
                        <a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(r.locationAddress)}" target="_blank" class="btn btn-outline-primary btn-sm w-100 rounded-pill"><i class="fa-solid fa-location-arrow me-1"></i> Open in Maps</a>
                    </div>
                </div>
            </div>
            ${r.imageUrls && r.imageUrls.length > 0 ? `
            <div class="mt-4">
                <h6 class="fw-bold mb-3">Reported Photos</h6>
                <div class="d-flex gap-2 overflow-auto pb-2">
                    ${r.imageUrls.map(img => {
                        const url = img.startsWith('http') ? img : `http://localhost:8080${img}`;
                        return `<img src="${url}" class="rounded-3 border" style="height: 150px; width: 150px; object-fit: cover; cursor: pointer;" onclick="window.open('${url}')">`;
                    }).join('')}
                </div>
            </div>` : '<p class="text-muted small">No photos provided.</p>'}
        `;
        
        document.getElementById('detailsContent').innerHTML = content;
        detailsModal.show();
    } catch (error) {
        Swal.fire('Error', 'Failed to load details', 'error');
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '../index.html';
}
