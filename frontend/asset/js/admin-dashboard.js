// Admin Dashboard Logic
document.addEventListener('DOMContentLoaded', () => {
    // Check Auth
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user'));

    if (!token || !user || user.role !== 'ADMIN') {
        Swal.fire({
            icon: 'error',
            title: 'Unauthorized',
            text: 'You do not have access to this area.'
        }).then(() => {
            window.location.href = '../index.html';
        });
        return;
    }

    // Set UI details
    document.getElementById('admin-name').innerText = user.fullName || 'Admin';
    document.getElementById('current-date').innerText = new Date().toLocaleDateString('en-US', { day: 'numeric', month: 'long', year: 'numeric' });

    // Initial Load
    loadStats();
    loadUsers();
    loadRescueReports();
    loadAdoptionReports();
    updateRecentActivity();

    // Auto-refresh stats every 60s
    setInterval(() => {
        loadStats();
        updateRecentActivity();
    }, 60000);

    // Tab Event Listeners for refresh
    document.getElementById('adminTabs').addEventListener('shown.bs.tab', (e) => {
        const tabId = e.target.id;
        if (tabId === 'nav-dashboard') loadStats();
        if (tabId === 'nav-users') loadUsers();
        if (tabId === 'nav-ngo') loadUsers(); // Also loads users to filter NGOs
        if (tabId === 'nav-rescue') loadRescueReports();
        if (tabId === 'nav-adoption') loadAdoptionReports();
        if (tabId === 'nav-rate') loadRescueReports(); // Use rescue reports for rating
        updateRecentActivity();
    });
});

async function refreshData() {
    const icon = document.getElementById('refresh-icon');
    if (icon) icon.classList.add('fa-spin');
    
    await Promise.all([
        loadStats(),
        loadUsers(),
        loadRescueReports(),
        loadAdoptionReports()
    ]);
    
    updateRecentActivity();
    
    if (icon) {
        setTimeout(() => icon.classList.remove('fa-spin'), 1000);
    }
}

const API_BASE_URL = 'http://localhost:8080/api';
const token = localStorage.getItem('token');

// Utility for formatting dates
function fDate(dateStr) {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('en-US', { day: 'numeric', month: 'short' });
}

// 1. STATS
async function loadStats() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/stats`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const stats = await response.json();
        
        Object.keys(stats).forEach(key => {
            const el = document.getElementById(`stat-${key}`);
            if (el) el.innerText = stats[key];
        });
    } catch (error) {
        console.error("Stats load error", error);
    }
}

// 2. USERS MANAGEMENT
let allUsers = [];
async function loadUsers() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/users`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        allUsers = await response.json();
        renderUsers(allUsers);
        renderPendingNGOs(allUsers);
    } catch (error) {
        console.error("Users load error", error);
    }
}

function renderUsers(users) {
    const tbody = document.getElementById('usersTableBody');
    tbody.innerHTML = users.map(u => `
        <tr>
            <td>#${u.id}</td>
            <td class="fw-bold">${u.fullName}</td>
            <td class="text-muted">${u.email}</td>
            <td><span class="badge ${u.role === 'NGO' ? 'bg-secondary' : 'bg-light text-dark'}">${u.role}</span></td>
            <td>${u.isVerified ? '<span class="text-success small fw-bold"><i class="fa-solid fa-circle-check"></i> YES</span>' : '<span class="text-muted small">NO</span>'}</td>
            <td>${fDate(u.createdAt)}</td>
            <td class="text-center">
                <div class="d-flex justify-content-center gap-2">
                    ${u.role === 'NGO' && !u.isVerified ? `<button class="btn-action bg-success-subtle text-success" onclick="openVerifyModal(${u.id}, '${u.organizationName}')">VERIFY</button>` : ''}
                    <button class="btn-action bg-danger-subtle text-danger" onclick="confirmDeleteUser(${u.id})"><i class="fa-solid fa-trash"></i></button>
                </div>
            </td>
        </tr>
    `).join('');
}

function filterUsers() {
    const q = document.getElementById('userSearch').value.toLowerCase();
    const role = document.getElementById('userRoleFilter').value;
    
    const filtered = allUsers.filter(u => {
        const matchQ = u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q);
        const matchRole = role === 'ALL' || u.role === role;
        return matchQ && matchRole;
    });
    renderUsers(filtered);
}

function renderPendingNGOs(users) {
    const pendingList = document.getElementById('ngoPendingList');
    const pending = users.filter(u => u.role === 'NGO' && !u.isVerified);
    
    if (pending.length === 0) {
        pendingList.innerHTML = '<div class="col-12 text-center py-5 text-muted">No pending NGO verification requests</div>';
        return;
    }

    pendingList.innerHTML = pending.map(n => {
        const safeName = n.organizationName.replace(/'/g, "\\'");
        return `
            <div class="col-md-6 col-xl-4 animate-in">
                <div class="table-card p-4 h-100 d-flex flex-column">
                    <div class="d-flex justify-content-between mb-3 align-items-start">
                        <h6 class="fw-bold m-0" style="color: #0f172a;">${n.organizationName}</h6>
                        <span class="badge bg-warning">PENDING</span>
                    </div>
                    <div class="mt-auto">
                        <div class="small mb-2 d-flex align-items-center gap-2"><i class="fa-solid fa-id-card text-muted"></i> License: ${n.licenseNumber}</div>
                        <div class="small mb-2 d-flex align-items-center gap-2"><i class="fa-solid fa-phone text-muted"></i> ${n.phone}</div>
                        <div class="small mb-2 d-flex align-items-center gap-2"><i class="fa-solid fa-envelope text-muted"></i> ${n.email}</div>
                        <div class="small mb-4 text-muted"><i class="fa-solid fa-location-dot"></i> ${n.address}, ${n.city}</div>
                        
                        <div class="d-flex gap-2">
                            <button class="btn btn-brand btn-sm flex-fill fw-bold" onclick="openVerifyModal(${n.id}, '${safeName}')">VERIFY Partner</button>
                            <button class="btn btn-outline-danger btn-sm flex-fill fw-bold" onclick="openRejectModal(${n.id}, '${safeName}')">REJECT</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// Verification Logic
const adminModal = new bootstrap.Modal(document.getElementById('adminModal'));

function openVerifyModal(id, name) {
    document.getElementById('adminModalTitle').innerText = 'Verify NGO';
    document.getElementById('adminModalBody').innerHTML = `
        <p>Are you sure you want to verify <strong>${name}</strong>? This will enable their rescue operations.</p>
        <div class="mb-3">
            <label class="form-label small fw-bold">Verification Notes (Optional)</label>
            <textarea id="verifyNotes" class="form-control" rows="3"></textarea>
        </div>
    `;
    document.getElementById('adminModalFooter').innerHTML = `
        <button class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Cancel</button>
        <button class="btn btn-success rounded-pill px-4" onclick="confirmVerify(${id})">Confirm Verification</button>
    `;
    adminModal.show();
}

async function confirmVerify(id) {
    const notes = document.getElementById('verifyNotes').value;
    try {
        const response = await fetch(`${API_BASE_URL}/admin/users/${id}/verify`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ notes })
        });
        if (response.ok) {
            Swal.fire({ icon: 'success', title: 'Verified!', showConfirmButton: false, timer: 1500 });
            adminModal.hide();
            
            // Optimistic local update - use == for flexible ID matching
            const idx = allUsers.findIndex(u => u.id == id);
            if (idx !== -1) {
                allUsers[idx].isVerified = true;
                renderUsers(allUsers);
                renderPendingNGOs(allUsers);
            }
            
            await loadUsers(); // Sync with backend
            loadStats();
        } else {
            const data = await response.json();
            Swal.fire('Error', data.message || 'Verification failed on server', 'error');
        }
    } catch (error) {
        console.error("Verification error", error);
        Swal.fire('Error', 'Connection failed or server error', 'error');
    }
}

function openRejectModal(id, name) {
    document.getElementById('adminModalTitle').innerText = 'Reject NGO Request';
    document.getElementById('adminModalBody').innerHTML = `
        <p class="text-danger fw-bold">Warning: You are about to reject the registration for ${name}.</p>
        <div class="mb-3">
            <label class="form-label small fw-bold">Rejection Reason (Required)</label>
            <textarea id="rejectReason" class="form-control border-danger" rows="3" required></textarea>
        </div>
    `;
    document.getElementById('adminModalFooter').innerHTML = `
        <button class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Cancel</button>
        <button class="btn btn-danger rounded-pill px-4" onclick="confirmReject(${id})">Confirm Rejection</button>
    `;
    adminModal.show();
}

async function confirmReject(id) {
    const reason = document.getElementById('rejectReason').value;
    if (!reason) {
        Swal.fire('Required', 'Please provide a rejection reason', 'warning');
        return;
    }
    try {
        const response = await fetch(`${API_BASE_URL}/admin/users/${id}/reject`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ reason })
        });
        if (response.ok) {
            Swal.fire({ icon: 'info', title: 'Rejected', showConfirmButton: false, timer: 1500 });
            adminModal.hide();
            
            // Optimistic local update - use == for flexible ID matching
            allUsers = allUsers.filter(u => u.id != id);
            renderUsers(allUsers);
            renderPendingNGOs(allUsers);
            
            await loadUsers(); // Sync with backend
            loadStats();
        } else {
            const data = await response.json();
            Swal.fire('Error', data.message || 'Rejection failed on server', 'error');
        }
    } catch (error) {
        console.error("Rejection error", error);
        Swal.fire('Error', 'Connection failed or server error', 'error');
    }
}

function confirmDeleteUser(id) {
    Swal.fire({
        title: 'Are you sure?',
        text: "This action cannot be undone. User and their data will be removed.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        confirmButtonText: 'Yes, delete it!'
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                const response = await fetch(`${API_BASE_URL}/admin/users/${id}`, {
                    method: 'DELETE',
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    Swal.fire('Deleted!', 'User has been removed.', 'success');
                    loadUsers();
                    loadStats();
                } else {
                    const data = await response.json();
                    Swal.fire('Failed', data.message || 'Error occurred', 'error');
                }
            } catch (error) {
                Swal.fire('Error', 'Connection failed', 'error');
            }
        }
    });
}

// 3. RESCUE REPORTS
let allRescues = [];
async function loadRescueReports() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/rescue-reports`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        allRescues = await response.json();
        renderRescues(allRescues);
        renderRateList(allRescues);
    } catch (error) {
        console.error("Rescue load error", error);
    }
}

function renderRescues(rescues) {
    const tbody = document.getElementById('rescueTableBody');
    tbody.innerHTML = rescues.map(r => `
        <tr>
            <td>#${r.id}</td>
            <td class="fw-bold">${r.reporterName}</td>
            <td>${r.animalType}</td>
            <td class="small text-muted text-truncate" style="max-width: 15rem;">${r.locationAddress}</td>
            <td><span class="badge ${r.status === 'COMPLETED' ? 'bg-success' : 'bg-warning'}">${r.status}</span></td>
            <td>${fDate(r.createdAt)}</td>
            <td class="text-center">
                <button class="btn-action bg-primary-subtle text-primary" onclick="viewRescueDetails(${r.id})">VIEW</button>
            </td>
        </tr>
    `).join('');
}

function filterRescues() {
    const q = document.getElementById('rescueSearch').value.toLowerCase();
    const status = document.getElementById('rescueStatusFilter').value;
    const filtered = allRescues.filter(r => {
        return (r.reporterName.toLowerCase().includes(q) || r.animalType.toLowerCase().includes(q)) && (status === 'ALL' || r.status === status);
    });
    renderRescues(filtered);
}

// 4. ADOPTION REPORTS
let allAdoptions = [];
async function loadAdoptionReports() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/adoption-requests`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        allAdoptions = await response.json();
        renderAdoptions(allAdoptions);
    } catch (error) {
        console.error("Adoption load error", error);
    }
}

function renderAdoptions(adoptions) {
    const tbody = document.getElementById('adoptionTableBody');
    tbody.innerHTML = adoptions.map(a => `
        <tr>
            <td>#${a.id}</td>
            <td class="fw-bold">${a.animal.name}</td>
            <td>${a.adopter.fullName}</td>
            <td>${a.owner.fullName}</td>
            <td><span class="badge ${a.requestStatus === 'COMPLETED' ? 'bg-success' : a.requestStatus === 'APPROVED' ? 'bg-info' : 'bg-warning'}">${a.requestStatus}</span></td>
            <td>${fDate(a.createdAt)}</td>
            <td class="text-center">
                <button class="btn-action bg-primary-subtle text-primary" onclick="viewAdoptionDetails(${a.id})">VIEW</button>
            </td>
        </tr>
    `).join('');
}

function filterAdoptions() {
    const q = document.getElementById('adoptionSearch').value.toLowerCase();
    const status = document.getElementById('adoptionStatusFilter').value;
    const filtered = allAdoptions.filter(a => {
        return (a.animal.name.toLowerCase().includes(q) || a.adopter.fullName.toLowerCase().includes(q)) && (status === 'ALL' || a.requestStatus === status);
    });
    renderAdoptions(filtered);
}

// 5. RATING
function renderRateList(rescues) {
    const tbody = document.getElementById('rateTableBody');
    const completed = rescues.filter(r => r.status === 'COMPLETED');
    
    tbody.innerHTML = completed.map(r => `
        <tr>
            <td>#${r.id}</td>
            <td class="fw-bold">NGO Partner</td>
            <td>${r.animalType}</td>
            <td>${fDate(r.createdAt)}</td>
            <td>${r.rating ? `<span class="text-warning">${'★'.repeat(r.rating)}${'☆'.repeat(5-r.rating)}</span>` : '<span class="text-muted italic">Not Rated</span>'}</td>
            <td class="text-center">
                <button class="btn-action bg-warning text-dark" onclick="openRateModal(${r.id})">${r.rating ? 'EDIT RATE' : 'RATE NGO'}</button>
            </td>
        </tr>
    `).join('');
}

let selectedRating = 0;
function openRateModal(id) {
    const rescue = allRescues.find(r => r.id === id);
    selectedRating = rescue.rating || 0;
    
    document.getElementById('adminModalTitle').innerText = 'Rate NGO Performance';
    document.getElementById('adminModalBody').innerHTML = `
        <div class="text-center mb-4">
            <h6 class="text-muted mb-2">Rescue ID #${id}</h6>
            <h5>${rescue.animalType} Rescue Mission</h5>
        </div>
        <div class="text-center mb-4 fs-1" id="star-container">
            ${[1,2,3,4,5].map(i => `<i class="fa-star ${i <= selectedRating ? 'fa-solid text-warning' : 'fa-regular text-muted'} cursor-pointer" onclick="setRating(${i})"></i>`).join('')}
        </div>
        <div class="mb-3">
            <label class="form-label small fw-bold">Admin Remarks</label>
            <textarea id="ratingRemarks" class="form-control" rows="3" placeholder="Performance feedback...">${rescue.ratingRemarks || ''}</textarea>
        </div>
    `;
    document.getElementById('adminModalFooter').innerHTML = `
        <button class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Cancel</button>
        <button class="btn btn-brand rounded-pill px-4" onclick="submitRating(${id})">Submit Rating</button>
    `;
    adminModal.show();
}

function setRating(r) {
    selectedRating = r;
    const container = document.getElementById('star-container');
    container.innerHTML = [1,2,3,4,5].map(i => `<i class="fa-star ${i <= selectedRating ? 'fa-solid text-warning' : 'fa-regular text-muted'} cursor-pointer" onclick="setRating(${i})"></i>`).join('');
}

async function submitRating(id) {
    const remarks = document.getElementById('ratingRemarks').value;
    try {
        const response = await fetch(`${API_BASE_URL}/admin/rescue/${id}/rate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ rating: selectedRating, remarks })
        });
        if (response.ok) {
            Swal.fire('Success', 'NGO has been rated.', 'success');
            adminModal.hide();
            loadRescueReports();
        }
    } catch (error) {
        Swal.fire('Error', 'Rating failed', 'error');
    }
}

// 6. CSV EXPORT
function exportRescues() {
    const headers = "Rescue ID,Reporter Name,Reporter Phone,Animal Type,Condition,Emergency Level,Location,Status,Reported Date,Completed Date,Responding NGO,Rating\n";
    const rows = allRescues.map(r => {
        return `"${r.id}","${r.reporterName}","${r.reporterPhone}","${r.animalType}","${r.animalCondition}","${r.emergencyLevel}","${r.locationAddress.replace(/"/g, '""')}","${r.status}","${r.createdAt}","${r.resolvedAt || ''}","NGO","${r.rating || 0}"`;
    }).join("\n");
    
    downloadCSV(headers + rows, `rescue_reports_${new Date().toISOString().split('T')[0]}.csv`);
}

function exportAdoptions() {
    const headers = "Request ID,Animal Name,Species,Adopter Name,Adopter Contact,Owner Name,Status,Request Date,Approval Date\n";
    const rows = allAdoptions.map(a => {
        return `"${a.id}","${a.animal.name}","${a.animal.species || 'N/A'}","${a.adopter.fullName}","${a.adopter.phone}","${a.owner.fullName}","${a.requestStatus}","${a.createdAt}","${a.updatedAt}"`;
    }).join("\n");
    
    downloadCSV(headers + rows, `adoption_reports_${new Date().toISOString().split('T')[0]}.csv`);
}

function updateRecentActivity() {
    const activityLog = document.getElementById('activity-log');
    if (!activityLog) return;

    // Combine some recent data
    const recentRescues = allRescues.slice(0, 3).map(r => ({
        icon: 'fa-truck-medical',
        color: 'text-danger',
        bg: 'bg-danger-subtle',
        title: `New Rescue: ${r.animalType}`,
        time: fDate(r.createdAt)
    }));
    
    const recentAdoptions = allAdoptions.slice(0, 2).map(a => ({
        icon: 'fa-heart',
        color: 'text-success',
        bg: 'bg-success-subtle',
        title: `Adoption: ${a.animal.name}`,
        time: fDate(a.createdAt)
    }));

    const activities = [...recentRescues, ...recentAdoptions];
    if (activities.length === 0) return;

    activityLog.innerHTML = activities.map(act => `
        <div class="p-3 bg-light rounded-4 d-flex align-items-center gap-3 border border-white">
            <div class="${act.bg} ${act.color} p-2 rounded-3 text-center" style="width:35px;"><i class="fa-solid ${act.icon}"></i></div>
            <div class="lh-1">
                <div class="x-small fw-bold">${act.title}</div>
                <div class="x-small text-muted mt-1">${act.time}</div>
            </div>
        </div>
    `).join('');
}

function downloadCSV(csvContent, filename) {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// DETAILS MODALS
function viewRescueDetails(id) {
    const r = allRescues.find(x => x.id === id);
    document.getElementById('adminModalTitle').innerText = `Rescue Details - #${id}`;
    document.getElementById('adminModalBody').innerHTML = `
        <div class="row g-4">
            <div class="col-md-6 border-end">
                <h6 class="fw-bold text-primary mb-3">Reporter Info</h6>
                <p class="mb-1"><strong>Name:</strong> ${r.reporterName}</p>
                <p class="mb-1"><strong>Phone:</strong> ${r.reporterPhone}</p>
                <h6 class="fw-bold text-primary mt-4 mb-3">Animal Info</h6>
                <p class="mb-1"><strong>Type:</strong> ${r.animalType}</p>
                <p class="mb-1"><strong>Condition:</strong> ${r.animalCondition}</p>
                <p class="mb-1"><strong>Count:</strong> ${r.animalCount}</p>
            </div>
            <div class="col-md-6">
                <h6 class="fw-bold text-primary mb-3">Location & description</h6>
                <p class="mb-1"><strong>Address:</strong> ${r.locationAddress}</p>
                <p class="mb-1"><strong>Landmark:</strong> ${r.landmark || 'N/A'}</p>
                <p class="mt-3 text-muted small italic">"${r.description}"</p>
            </div>
        </div>
    `;
    document.getElementById('adminModalFooter').innerHTML = '<button class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Close</button>';
    adminModal.show();
}

function viewAdoptionDetails(id) {
    const a = allAdoptions.find(x => x.id === id);
    document.getElementById('adminModalTitle').innerText = `Adoption Application - #${id}`;
    document.getElementById('adminModalBody').innerHTML = `
        <div class="row g-4">
            <div class="col-md-6 border-end">
                <h6 class="fw-bold text-primary mb-3">Adopter Details</h6>
                <p class="mb-1"><strong>Name:</strong> ${a.adopter.fullName}</p>
                <p class="mb-1"><strong>Phone:</strong> ${a.adopter.phone || 'N/A'}</p>
                <h6 class="fw-bold text-primary mt-4 mb-3">Living Situation</h6>
                <p class="mb-1"><strong>Status:</strong> ${a.livingSituation}</p>
                <p class="mb-1"><strong>Experience:</strong> ${a.hasExperience ? 'Experienced' : 'First-time'}</p>
            </div>
            <div class="col-md-6">
                <h6 class="fw-bold text-primary mb-3">Animal Details</h6>
                <p class="mb-1"><strong>Name:</strong> ${a.animal.name}</p>
                <p class="mb-1"><strong>Breed:</strong> ${a.animal.breed || 'N/A'}</p>
                <h6 class="fw-bold text-primary mt-4 mb-3">Reason for Adoption</h6>
                <p class="text-muted small italic">"${a.reasonForAdoption}"</p>
            </div>
        </div>
    `;
    document.getElementById('adminModalFooter').innerHTML = '<button class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Close</button>';
    adminModal.show();
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '../index.html';
}
