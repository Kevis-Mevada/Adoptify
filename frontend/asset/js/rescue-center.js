document.addEventListener('DOMContentLoaded', () => {
    // 1. Authentication Check
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user'));

    if (!token || !user) {
        window.location.href = '../auth/login.html?redirect=rescue/rescue.html';
        return;
    }

    const API_BASE_URL = 'http://localhost:8080/api';
    let currentFiles = [];

    // Header Setup
    const authSection = document.getElementById('auth-section');
    const userSection = document.getElementById('user-section');
    const userNameDisplay = document.getElementById('user-name');
    const logoutBtn = document.getElementById('logout-btn');

    if (token && user) {
        if (authSection) authSection.classList.add('d-none');
        if (userSection) userSection.classList.remove('d-none');
        if (userSection) userSection.classList.add('d-flex');
        if (userNameDisplay) userNameDisplay.textContent = user.fullName;
        
        if (logoutBtn) {
            logoutBtn.onclick = (e) => {
                e.preventDefault();
                localStorage.clear();
                window.location.href = '../auth/login.html';
            };
        }
    }

    // Auto-fill Reporter Info
    document.getElementById('reporterName').value = user.fullName;
    document.getElementById('reporterPhone').value = user.phone || '';

    // Trigger Report Tab from Hero
    window.triggerReportTab = () => {
        const reportTab = document.getElementById('report-tab');
        if (reportTab) {
            reportTab.click();
            reportTab.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    };

    // --- TAB 1: REPORT LOGIC ---

    // Geolocation
    window.getCurrentLocation = () => {
        if (!navigator.geolocation) return alert('Geolocation not supported');
        
        const btn = event.target;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Locating...';
        btn.disabled = true;

        navigator.geolocation.getCurrentPosition(async (pos) => {
            const { latitude, longitude } = pos.coords;
            document.getElementById('latitude').value = latitude;
            document.getElementById('longitude').value = longitude;

            try {
                const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}`);
                const data = await res.json();
                document.getElementById('locationAddress').value = data.display_name || `Lat: ${latitude}, Lng: ${longitude}`;
            } catch (e) {
                document.getElementById('locationAddress').value = `Lat: ${latitude}, Lng: ${longitude}`;
            } finally {
                btn.innerHTML = 'Use My Current Location';
                btn.disabled = false;
            }
        }, (err) => {
            alert('Position error: ' + err.message);
            btn.innerHTML = 'Use My Current Location';
            btn.disabled = false;
        });
    };

    // Images
    window.previewImages = (e) => {
        const previewRoot = document.getElementById('imagePreview');
        previewRoot.innerHTML = '';
        currentFiles = Array.from(e.target.files);

        if (currentFiles.length > 5) {
            alert('Max 5 images allowed');
            e.target.value = '';
            currentFiles = [];
            return;
        }

        currentFiles.forEach((file) => {
            const reader = new FileReader();
            reader.onload = (ev) => {
                const div = document.createElement('div');
                div.className = 'position-relative';
                div.innerHTML = `<img src="${ev.target.result}" class="rounded-3 border" style="width: 100px; height: 100px; object-fit: cover;">`;
                previewRoot.appendChild(div);
            };
            reader.readAsDataURL(file);
        });
    };

    // Submit
    const rescueForm = document.getElementById('rescueForm');
    rescueForm.onsubmit = async (e) => {
        e.preventDefault();

        if (currentFiles.length === 0) {
            return Swal.fire('Photo Required', 'Please provide at least one photo of the animal.', 'error');
        }

        const reportData = {
            animalType: document.getElementById('animalType').value,
            animalCount: document.getElementById('animalCount').value,
            animalCondition: document.getElementById('animalCondition').value,
            emergencyLevel: document.getElementById('emergencyLevel').value,
            locationAddress: document.getElementById('locationAddress').value,
            description: document.getElementById('description').value,
            latitude: document.getElementById('latitude').value || 0,
            longitude: document.getElementById('longitude').value || 0,
            reporterName: document.getElementById('reporterName').value,
            reporterPhone: document.getElementById('reporterPhone').value,
            imagesRequired: true
        };

        const formData = new FormData();
        formData.append('report', new Blob([JSON.stringify(reportData)], { type: 'application/json' }));
        currentFiles.forEach(f => formData.append('files', f));

        try {
            Swal.fire({ title: 'Submitting Report...', text: 'Our AI is verifying the images.', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

            const res = await fetch(`${API_BASE_URL}/rescue/report`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });

            const result = await res.json();
            if (res.ok) {
                Swal.fire({ icon: 'success', title: 'Report Submitted!', text: `Rescue ID: #${result.id}. Nearby NGOs have been alerted.`, confirmButtonText: 'Great!' });
                rescueForm.reset();
                document.getElementById('imagePreview').innerHTML = '';
                currentFiles = [];
                // Switch to status tab to see the new report
                document.getElementById('status-tab').click();
            } else {
                throw new Error(result.message || 'Submission failed');
            }
        } catch (err) {
            Swal.fire('Error', err.message, 'error');
        }
    };


    // Auto polling for status when the status tab is visible
    setInterval(() => {
        const statusTab = document.getElementById('tab-status');
        if (statusTab && statusTab.classList.contains('active')) {
            loadStatusRescues(true); // pass true to indicate silk/silent refresh
        }
    }, 15000);

    window.loadStatusRescues = async (isSilient = false) => {
        const root = document.getElementById('statusContainer');
        if (!isSilient) {
            root.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-warning" role="status"></div></div>';
        }

        try {
            const res = await fetch(`${API_BASE_URL}/rescue/my-reports?status=PENDING&status=ASSIGNED&status=EN_ROUTE&status=ARRIVED&status=RESCUED`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const data = await res.json();

            if (!data.content || data.content.length === 0) {
                root.innerHTML = `
                    <div class="text-center py-5 bg-white border rounded-4">
                        <i class="fa-solid fa-circle-check fs-1 text-success mb-3"></i>
                        <p class="text-muted fw-bold">No active rescues. All your reported rescues have been completed.</p>
                        <button class="btn btn-brand rounded-pill px-4 btn-sm" onclick="document.getElementById('report-tab').click()">Report a Rescue</button>
                    </div>
                `;
                return;
            }

            root.innerHTML = data.content.map(report => {
                const steps = ['PENDING', 'ASSIGNED', 'EN_ROUTE', 'ARRIVED', 'RESCUED', 'COMPLETED'];
                const currentIndex = steps.indexOf(report.status);
                const isUrgent = report.emergencyLevel === 'CRITICAL';
                
                return `
                <div class="card card-rescue border p-4 bg-white shadow-sm rounded-4 mb-3 ${report.status === 'ASSIGNED' ? 'border-primary' : ''}">
                    ${report.status === 'ASSIGNED' ? '<div class="badge bg-primary rounded-pill mb-3" style="width: fit-content;"><i class="fa-solid fa-bell me-1 animate-pulse"></i> NGO IS ON THE WAY</div>' : ''}
                    <div class="d-md-flex justify-content-between">
                        <div>
                            <h5 class="fw-black mb-1">#${report.id} - ${report.animalType}</h5>
                            <p class="small text-muted mb-0"><i class="fa-solid fa-location-dot me-2 text-danger"></i>${report.locationAddress}</p>
                            <p class="small text-muted"><strong>Condition:</strong> ${report.animalCondition} | <strong>Level:</strong> <span class="${isUrgent ? 'text-danger fw-bold' : ''}">${report.emergencyLevel}</span></p>
                        </div>
                        <div class="text-md-end mt-3 mt-md-0">
                            <button class="btn btn-sm btn-outline-dark rounded-pill px-4 fw-bold" onclick="openDetailsModal(${report.id})">View Details</button>
                        </div>
                    </div>

                    <div class="status-progress my-3">
                        ${steps.map((step, idx) => `
                            <div class="step-item text-center ${idx <= currentIndex ? 'completed' : ''} ${idx === currentIndex ? 'active' : ''}">
                                <div class="step-circle mx-auto">
                                    ${idx < currentIndex ? '<i class="fa-solid fa-check"></i>' : (idx + 1)}
                                </div>
                                <div class="step-label">${step}</div>
                            </div>
                        `).join('')}
                    </div>

                    ${report.responder ? `
                        <div class="mt-3 pt-3 border-top d-flex flex-wrap gap-4 align-items-center bg-light p-3 rounded-3">
                            <div>
                                <h6 class="small fw-black mb-1 text-primary"><i class="fa-solid fa-building-circle-check me-2"></i>NGO: ${report.responder.organizationName}</h6>
                                <p class="small text-muted mb-0">Contact: <strong>${report.responder.phone}</strong> (Call for updates)</p>
                            </div>
                            <div class="ms-auto">
                                <span class="badge bg-primary text-white border border-primary px-3 py-2 rounded-4 shadow-sm">
                                    <i class="fa-solid fa-truck-fast me-2"></i>ETA: ${new Date(report.responder.estimatedArrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </span>
                            </div>
                        </div>
                    ` : `
                        <div class="mt-3 pt-3 border-top text-center py-2">
                            <p class="small text-muted mb-0"><i class="fa-solid fa-circle-nodes fa-spin me-2 text-warning"></i>Searching for nearby verified NGOs...</p>
                        </div>
                    `}
                </div>
                `;
            }).join('');
        } catch (err) {
            if (!isSilient) root.innerHTML = `<div class="alert alert-danger">Error: ${err.message}</div>`;
        }
    


    // --- TAB 3: HISTORY LOGIC ---

    window.loadRescueHistory = async (page = 0) => {
        const root = document.getElementById('historyTableBody');
        const paginationRoot = document.getElementById('historyPagination');
        
        root.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm me-2"></div>Loading history...</td></tr>';

        try {
            const res = await fetch(`${API_BASE_URL}/rescue/my-reports?page=${page}&size=10&status=RESCUED&status=COMPLETED&status=REJECTED`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const data = await res.json();

            if (!data.content || data.content.length === 0) {
                root.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted">No rescue history found.</td></tr>';
                paginationRoot.innerHTML = '';
                return;
            }

            root.innerHTML = data.content.map(report => `
                <tr class="align-middle">
                    <td class="ps-4 fw-bold">#${report.id}</td>
                    <td><span class="fw-bold">${report.animalType}</span><br><span class="text-muted small">${report.animalCondition}</span></td>
                    <td class="small text-truncate" style="max-width: 250px;">${report.locationAddress}</td>
                    <td>${new Date(report.createdAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}</td>
                    <td>
                        <span class="badge ${report.status === 'REJECTED' ? 'bg-danger-subtle text-danger' : 'bg-success-subtle text-success'} rounded-pill px-3">
                            ${report.status}
                        </span>
                    </td>
                    <td><button class="btn btn-sm btn-light border rounded-pill px-3 fw-bold" onclick="openDetailsModal(${report.id})">Details</button></td>
                </tr>
            `).join('');

            // Pagination
            if (data.totalPages > 1) {
                let pg = `<ul class="pagination pagination-sm mb-0">`;
                pg += `<li class="page-item ${data.first ? 'disabled' : ''}"><button class="page-link rounded-pill me-2 px-3" onclick="loadRescueHistory(${data.number - 1})">Previous</button></li>`;
                
                for(let i=0; i<data.totalPages; i++) {
                    pg += `<li class="page-item ${data.number === i ? 'active' : ''}"><button class="page-link rounded-circle mx-1" onclick="loadRescueHistory(${i})">${i+1}</button></li>`;
                }

                pg += `<li class="page-item ${data.last ? 'disabled' : ''}"><button class="page-link rounded-pill ms-2 px-3" onclick="loadRescueHistory(${data.number + 1})">Next</button></li>`;
                pg += `</ul>`;
                paginationRoot.innerHTML = pg;
            } else {
                paginationRoot.innerHTML = '';
            }

        } catch (err) {
            root.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Error loading history</td></tr>`;
        }
    };


    // --- MODAL LOGIC ---

    const detailsModal = new bootstrap.Modal(document.getElementById('detailsModal'));

    window.openDetailsModal = async (id) => {
        const content = document.getElementById('modalContent');
        content.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-brand"></div></div>';
        detailsModal.show();

        try {
            const res = await fetch(`${API_BASE_URL}/rescue/${id}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const report = await res.json();

            content.innerHTML = `
                <div class="row g-4">
                    <div class="col-md-6 mb-3">
                        <label class="small text-muted fw-bold d-block mb-1">Animal Information</label>
                        <ul class="list-unstyled mb-0">
                            <li><strong>Type:</strong> ${report.animalType} (${report.animalCount})</li>
                            <li><strong>Condition:</strong> ${report.animalCondition}</li>
                            <li><strong>Urgency:</strong> ${report.emergencyLevel}</li>
                        </ul>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="small text-muted fw-bold d-block mb-1">Location Details</label>
                        <p class="small mb-1"><strong>Address:</strong> ${report.locationAddress}</p>
                        <p class="small mb-0"><strong>Landmark:</strong> ${report.landmark || 'N/A'}</p>
                    </div>
                    <div class="col-12 mb-3">
                        <label class="small text-muted fw-bold d-block mb-1">Description</label>
                        <p class="bg-light p-3 rounded-3 small mb-0">${report.description}</p>
                    </div>
                    <div class="col-12 mb-3">
                        <label class="small text-muted fw-bold d-block mb-1">Evidence Summary</label>
                        <div class="d-flex flex-wrap gap-2">
                            ${report.imageUrls.map(url => {
                                const fullUrl = url.startsWith('http') ? url : `http://localhost:8080${url}`;
                                return `
                                    <img src="${fullUrl}" class="rounded-3 border" style="width: 150px; height: 150px; object-fit: cover; cursor: pointer;" onclick="window.open('${fullUrl}')">
                                `;
                            }).join('')}
                        </div>
                    </div>
                    ${report.responder ? `
                        <div class="col-12">
                            <div class="p-3 bg-primary-subtle rounded-4 text-primary">
                                <h6 class="fw-black mb-1">Assigned NGO: ${report.responder.organizationName}</h6>
                                <p class="small mb-0">Contact Person: ${report.responder.name} | Phone: ${report.responder.phone}</p>
                            </div>
                        </div>
                    ` : ''}
                </div>
            `;
        } catch (err) {
            content.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
        }
    };

    // --- TAB SWITCH LOGIC ---
    document.getElementById('status-tab').addEventListener('click', () => window.loadStatusRescues());
    document.getElementById('history-tab').addEventListener('click', () => window.loadRescueHistory());

    // Initial fill for reporter info (just in case)
    if (user) {
        document.getElementById('reporterName').value = user.fullName;
        document.getElementById('reporterPhone').value = user.phone || '';
    }
    }
});
