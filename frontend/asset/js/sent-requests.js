/* Sent Requests Page Logic */

document.addEventListener('DOMContentLoaded', () => {
    let currentStatus = 'ALL';
    let currentPage = 0;
    const pageSize = 10;

    const filterTabs = document.getElementById('filterTabs');
    const requestsBody = document.getElementById('requestsBody');
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const listContent = document.getElementById('listContent');
    const paginationContainer = document.getElementById('paginationContainer');
    const pagination = document.getElementById('pagination');
    const paginationInfo = document.getElementById('paginationInfo');
    
    // Category Tabs
    const adoptionTab = document.getElementById('adoptionTab');
    const rescueTab = document.getElementById('rescueTab');
    let currentCategory = 'ADOPTIONS'; // 'ADOPTIONS' or 'RESCUE'

    // Modals
    const viewDetailsModal = new bootstrap.Modal(document.getElementById('viewDetailsModal'));
    const cancelModal = new bootstrap.Modal(document.getElementById('cancelModal'));
    const contactModal = new bootstrap.Modal(document.getElementById('contactModal'));
    const reviewModal = new bootstrap.Modal(document.getElementById('reviewModal'));

    // Category Switching Logic
    adoptionTab.addEventListener('click', () => {
        if (currentCategory === 'ADOPTIONS') return;
        currentCategory = 'ADOPTIONS';
        setActiveCategoryTab(adoptionTab, rescueTab);
        resetFilters();
        fetchRequests();
    });

    rescueTab.addEventListener('click', () => {
        if (currentCategory === 'RESCUE') return;
        currentCategory = 'RESCUE';
        setActiveCategoryTab(rescueTab, adoptionTab);
        resetFilters();
        fetchRequests();
    });

    function setActiveCategoryTab(activeBtn, inactiveBtn) {
        activeBtn.classList.add('active', 'bg-white', 'shadow-sm');
        activeBtn.classList.remove('bg-transparent', 'text-muted');
        inactiveBtn.classList.remove('active', 'bg-white', 'shadow-sm');
        inactiveBtn.classList.add('bg-transparent', 'text-muted');
    }

    function resetFilters() {
        currentStatus = 'ALL';
        currentPage = 0;
        document.querySelectorAll('.filter-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.status === 'ALL');
        });
        
        // Hide filter tabs if on Rescue (MVP: Rescue doesn't have status filtering on backend yet)
        filterTabs.classList.toggle('d-none', currentCategory === 'RESCUE');
    }

    // Filter Logic
    filterTabs.addEventListener('click', (e) => {
        if (e.target.classList.contains('filter-btn')) {
            document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
            e.target.classList.add('active');
            currentStatus = e.target.dataset.status;
            currentPage = 0;
            fetchRequests();
        }
    });

    const API_BASE_URL = 'http://localhost:8080/api';
    const IMAGE_BASE_URL = 'http://localhost:8080/api/animals/images/';
    const token = localStorage.getItem('token');

    if (!token) {
        window.location.href = '../auth/login.html';
        return;
    }

    // API Integration
    async function fetchRequests() {
        toggleLoading(true);
        try {
            const statusParam = currentStatus === 'ALL' ? '' : `&status=${currentStatus}`;
            const endpoint = currentCategory === 'ADOPTIONS' 
                ? `${API_BASE_URL}/adoptions/my-requests?page=${currentPage}&size=${pageSize}${statusParam}`
                : `${API_BASE_URL}/rescue/my-reports`; // Rescue my-reports doesn't have paging yet in controller
            
            const response = await fetch(endpoint, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            
            if (!response.ok) throw new Error('Failed to fetch data');
            
            const data = await response.json();
            const items = Array.isArray(data) ? data : (data.content || []);
            
            updateTableHeaders();
            renderItems(items);
            
            if (data.totalPages && currentCategory === 'ADOPTIONS') {
                renderPagination(data);
                paginationContainer.classList.remove('d-none');
            } else {
                paginationContainer.classList.add('d-none');
            }
            
            toggleLoading(false);
        } catch (error) {
            console.error('Error:', error);
            toggleLoading(false);
            showEmptyState();
        }
    }

    function updateTableHeaders() {
        const headers = document.querySelectorAll('.request-table th');
        if (currentCategory === 'ADOPTIONS') {
            headers[0].textContent = 'Animal';
            headers[1].textContent = 'Request Date';
        } else {
            headers[0].textContent = 'Rescue Case';
            headers[1].textContent = 'Report Date';
        }
    }

    function renderItems(items) {
        if (!items || items.length === 0) {
            showEmptyState();
            return;
        }

        emptyState.classList.add('d-none');
        listContent.classList.remove('d-none');
        requestsBody.innerHTML = '';
        
        if (currentCategory === 'ADOPTIONS') {
            renderAdoptionRequests(items);
        } else {
            renderRescueReports(items);
        }
    }

    function renderAdoptionRequests(requests) {
        requests.forEach(req => {
            const tr = document.createElement('tr');
            const animalImg = getFullImageUrl(req.animalImage) || '../asset/Images/placeholder-animal.png';
            tr.innerHTML = `
                <td data-label="Animal">
                    <div class="animal-info-small">
                        <img src="${animalImg}" class="animal-img-small" alt="${req.animalName}">
                        <div>
                            <div class="animal-name-small">${req.animalName}</div>
                            <div class="animal-meta-small">${req.animalBreed || '-'} • ${req.animalAge || '-'}</div>
                        </div>
                    </div>
                </td>
                <td data-label="Request Date">
                    <div class="fw-medium">${formatDate(req.createdAt)}</div>
                    <div class="small text-muted">${req.ownerName}</div>
                </td>
                <td data-label="Status">
                    <span class="status-badge badge-${req.requestStatus.toLowerCase()}">${req.requestStatus}</span>
                </td>
                <td data-label="Actions" class="text-end">
                    <div class="d-flex justify-content-end gap-2">
                        <button class="btn btn-sm btn-light border fw-bold rounded-3 px-3" onclick="viewDetails(${req.id})">Details</button>
                        ${getAdoptionActions(req)}
                    </div>
                </td>
            `;
            requestsBody.appendChild(tr);
        });
    }

    function renderRescueReports(reports) {
        reports.forEach(report => {
            const tr = document.createElement('tr');
            const images = report.images ? report.images.split(',') : [];
            const rawImg = images[0];
            const animalImg = getFullImageUrl(rawImg) || '../asset/Images/placeholder-animal.png';
            
            tr.innerHTML = `
                <td data-label="Rescue Case">
                    <div class="animal-info-small">
                        <img src="${animalImg}" class="animal-img-small" alt="${report.animalType}">
                        <div>
                            <div class="animal-name-small">${report.animalType}</div>
                            <div class="animal-meta-small">Level: ${report.emergencyLevel}</div>
                        </div>
                    </div>
                </td>
                <td data-label="Report Date">
                    <div class="fw-medium">${formatDate(report.createdAt)}</div>
                    <div class="small text-muted">${report.locationAddress}</div>
                </td>
                <td data-label="Status">
                    <span class="status-badge badge-${report.status.toLowerCase()}">${report.status}</span>
                </td>
                <td data-label="Actions" class="text-end">
                    <div class="d-flex justify-content-end gap-2">
                         <button class="btn btn-sm btn-brand fw-bold rounded-3 px-3" onclick="viewRescueDetails(${report.id})">Update</button>
                    </div>
                </td>
            `;
            requestsBody.appendChild(tr);
        });
    }

    function getAdoptionActions(req) {
        if (req.requestStatus === 'PENDING') {
            return `<button class="btn btn-sm btn-outline-danger fw-bold rounded-3 px-3" onclick="confirmCancel(${req.id}, '${req.animalName}')">Cancel</button>`;
        } else if (req.requestStatus === 'APPROVED') {
            return `<button class="btn btn-sm btn-brand fw-bold rounded-3 px-3" onclick="contactOwner(${req.id})">Contact</button>`;
        } else if (req.requestStatus === 'COMPLETED') {
            return `<button class="btn btn-sm btn-outline-primary fw-bold rounded-3 px-3" onclick="writeReview(${req.id})">Review</button>`;
        }
        return '';
    }

    // Modal Action Handlers
    window.viewRescueDetails = (id) => {
        alert("Rescue update details coming soon! (ID: " + id + ")");
    };

    window.viewDetails = (id) => {
        const req = allRequests.find(r => r.id === id);
        if (!req) return;

        document.getElementById('modalAnimalImg').src = req.animalImage ? `${IMAGE_BASE_URL}${req.animalImage}` : '../asset/Images/placeholder-animal.png';
        document.getElementById('modalAnimalName').innerText = req.animalName;
        const badge = document.getElementById('modalStatusBadge');
        badge.innerText = req.requestStatus;
        badge.className = `status-badge badge-${req.requestStatus.toLowerCase()}`;

        // Animal Info
        document.getElementById('m-breed').innerText = req.animalBreed || '-';
        document.getElementById('m-age').innerText = req.animalAge || '-';
        document.getElementById('m-gender').innerText = req.animalGender || '-';
        document.getElementById('m-size').innerText = req.animalSize || '-';
        document.getElementById('m-color').innerText = req.animalColor || '-';
        document.getElementById('m-location').innerText = req.meetingAddress || 'See Owner Details';
        document.getElementById('m-fee').innerText = 'Free'; // Adoption fee not in DTO yet

        // Owner Section
        const ownerSection = document.getElementById('modalOwnerSection');
        if (req.requestStatus === 'APPROVED' || req.requestStatus === 'COMPLETED') {
            ownerSection.classList.remove('d-none');
            document.getElementById('m-owner').innerText = req.ownerName;
        } else {
            ownerSection.classList.add('d-none');
        }

        // Request Info
        document.getElementById('m-req-date').innerText = formatDate(req.createdAt);
        document.getElementById('m-living').innerText = req.livingSituation || '-';
        document.getElementById('m-yard').innerText = req.hasYard ? 'Yes' : 'No';
        document.getElementById('m-other-pets').innerText = req.hasOtherPets ? 'Yes' : 'No';
        document.getElementById('m-hours').innerText = req.dailyHoursAlone || '-';
        document.getElementById('m-pref-meeting').innerText = req.preferredMeetingDate || '-';
        document.getElementById('m-reason').innerText = req.reasonForAdoption || '-';
        document.getElementById('m-notes').innerText = '-';

        // Rejection Reason
        const rejectionRow = document.getElementById('m-rejected-reason-row');
        if (req.requestStatus === 'REJECTED') {
            rejectionRow.classList.remove('d-none');
            document.getElementById('m-rejected-reason').innerText = req.rejectionReason || 'No reason provided.';
        } else {
            rejectionRow.classList.add('d-none');
        }

        const modalActions = document.getElementById('modalActions');
        modalActions.innerHTML = getActionButtons(req);
        
        viewDetailsModal.show();
    };

    window.confirmCancel = (id, name) => {
        document.getElementById('cancelAnimalName').innerText = name;
        document.getElementById('confirmCancelBtn').onclick = () => performCancel(id);
        cancelModal.show();
    };

    window.contactOwner = (id) => {
        const req = allRequests.find(r => r.id === id);
        if (!req) return;
        
        document.getElementById('contactOwnerName').innerText = req.ownerName;
        document.getElementById('contactPhone').innerText = req.ownerPhone || 'Not available';
        document.getElementById('contactEmail').innerText = req.ownerEmail || 'Not available';
        document.getElementById('contactAddress').innerText = req.ownerAddress || 'Not available';
        
        contactModal.show();
    };

    window.writeReview = (id) => {
        const req = allRequests.find(r => r.id === id);
        if (!req) return;
        
        document.getElementById('reviewAnimalName').innerText = req.animalName;
        document.getElementById('reviewOwnerName').innerText = req.ownerName;
        document.getElementById('reviewRequestId').value = req.id;
        reviewModal.show();
    };

    window.copyText = (id) => {
        const text = document.getElementById(id).innerText;
        navigator.clipboard.writeText(text).then(() => {
            alert('Copied to clipboard!');
        });
    };

    async function performCancel(id) {
        try {
            const response = await fetch(`${API_BASE_URL}/adoptions/${id}/cancel`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            if (response.ok) {
                alert('Request Cancelled Successfully');
                cancelModal.hide();
                fetchRequests();
            } else {
                const err = await response.json();
                alert(err.message || 'Failed to cancel request');
            }
        } catch (e) {
            console.error(e);
            alert('Error connecting to server');
        }
    }

    // Review Stars Logic
    const stars = document.querySelectorAll('.star-btn');
    stars.forEach(star => {
        star.addEventListener('click', () => {
            const val = star.dataset.value;
            document.getElementById('ratingInput').value = val;
            stars.forEach(s => {
                if (s.dataset.value <= val) {
                    s.classList.replace('fa-regular', 'fa-solid');
                } else {
                    s.classList.replace('fa-solid', 'fa-regular');
                }
            });
        });
    });

    document.getElementById('reviewForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);
        const payload = {
            requestId: document.getElementById('reviewRequestId').value,
            rating: document.getElementById('ratingInput').value,
            comment: formData.get('comment')
        };

        try {
            const response = await fetch(`${API_BASE_URL}/reviews`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(payload)
            });
            if (response.ok) {
                alert('Review Submitted! Thank you.');
                reviewModal.hide();
                fetchRequests();
            } else {
                alert('Failed to submit review');
            }
        } catch (err) {
            console.error(err);
            alert('Error connecting to server');
        }
    });

    // Helpers
    function formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
    }

    function toggleLoading(isLoading) {
        if (isLoading) {
            loadingState.classList.remove('d-none');
            listContent.classList.add('d-none');
            emptyState.classList.add('d-none');
            paginationContainer.classList.add('d-none');
        } else {
            loadingState.classList.add('d-none');
        }
    }

    function showEmptyState() {
        listContent.classList.add('d-none');
        paginationContainer.classList.add('d-none');
        emptyState.classList.remove('d-none');
    }

    function formatDate(dateStr) {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    }

    function renderPagination(data) {
        pagination.innerHTML = '';
        if (data.totalPages <= 1) return;

        // Previous
        const prevLi = document.createElement('li');
        prevLi.className = `page-item ${data.first ? 'disabled' : ''}`;
        prevLi.innerHTML = `<a class="page-link" href="#" onclick="changePage(${currentPage - 1})">Previous</a>`;
        pagination.appendChild(prevLi);

        // Numbers
        for (let i = 0; i < data.totalPages; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === currentPage ? 'active' : ''}`;
            li.innerHTML = `<a class="page-link" href="#" onclick="changePage(${i})">${i + 1}</a>`;
            pagination.appendChild(li);
        }

        // Next
        const nextLi = document.createElement('li');
        nextLi.className = `page-item ${data.last ? 'disabled' : ''}`;
        nextLi.innerHTML = `<a class="page-link" href="#" onclick="changePage(${currentPage + 1})">Next</a>`;
        pagination.appendChild(nextLi);

        paginationInfo.innerText = `Showing ${data.numberOfElements} of ${data.totalElements} requests`;
    }

    window.changePage = (page) => {
        currentPage = page;
        fetchRequests();
    };

    let allRequests = [];

    // Initial Fetch
    fetchRequests();
    
    // Auth State for Header
    updateHeaderAuth();

    function updateHeaderAuth() {
        const tokenVal = localStorage.getItem('token');
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        const authSect = document.getElementById('auth-section');
        const userSect = document.getElementById('user-section');
        
        if (tokenVal && user.id) {
            if (authSect) authSect.classList.add('d-none');
            if (userSect) {
                userSect.classList.remove('d-none');
                userSect.classList.add('d-flex');
                document.getElementById('user-name').textContent = user.fullName || 'User';
            }
        }

        const logoutBtn = document.getElementById('logout-btn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', (e) => {
                e.preventDefault();
                localStorage.clear();
                window.location.href = '../auth/login.html';
            });
        }
    }
});
