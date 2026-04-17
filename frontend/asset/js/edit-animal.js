/**
 * Adoptify - Edit Animal JS
 */

document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = 'http://localhost:8080/api/animals';
    const token = localStorage.getItem('token');
    
    // Get animal ID from URL
    const urlParams = new URLSearchParams(window.location.search);
    const animalId = urlParams.get('id');

    if (!token) {
        window.location.href = '../auth/login.html';
        return;
    }

    if (!animalId) {
        window.location.href = 'listed-animals.html';
        return;
    }

    const editForm = document.getElementById('editAnimalForm');

    // 1. Load Animal Data
    async function loadAnimalData() {
        try {
            const response = await fetch(`${API_BASE_URL}/${animalId}`);
            if (!response.ok) throw new Error('Animal not found');
            
            const animal = await response.json();
            
            // Fill form fields
            document.getElementById('e-name').value = animal.name;
            document.getElementById('e-species').value = animal.species;
            document.getElementById('e-breed').value = animal.breed || '';
            document.getElementById('e-age').value = animal.age || '';
            document.getElementById('e-gender').value = animal.gender || 'UNKNOWN';
            document.getElementById('e-size').value = animal.size || 'MEDIUM';
            document.getElementById('e-color').value = animal.color || '';
            document.getElementById('e-status').value = animal.status;
            document.getElementById('e-desc').value = animal.description || '';
            document.getElementById('e-health').value = animal.healthInfo || '';
            document.getElementById('e-loc').value = animal.location || '';

        } catch (error) {
            Swal.fire('Error', 'Could not load animal data.', 'error').then(() => {
                window.location.href = 'listed-animals.html';
            });
        }
    }

    loadAnimalData();

    // 2. Handle Update
    editForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const formData = new FormData(editForm);
        const data = Object.fromEntries(formData.entries());
        
        // Add ID and clean up
        data.id = parseInt(animalId);

        try {
            const response = await fetch(`${API_BASE_URL}/${animalId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.message || 'Update failed');
            }

            Swal.fire({
                icon: 'success',
                title: 'Listing Updated!',
                text: 'All changes have been saved.',
                confirmButtonColor: '#ffc107'
            }).then(() => {
                window.location.href = 'listed-animals.html';
            });

        } catch (error) {
            Swal.fire('Error', error.message, 'error');
        }
    });

});
