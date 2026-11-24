document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchInput');
    let searchDebounceTimer;

    // Load initial data
    fetchIssues();

    // Add event listener for the search input
    searchInput.addEventListener('input', () => {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => {
            fetchIssues(0, searchInput.value); // Reset to page 0 on new search
        }, 300); // Debounce for 300ms
    });
});

function fetchIssues(page = 0, search = '') {
    const tableBody = document.getElementById('issuesTableBody');
    const loadingIndicator = document.getElementById('loadingIndicator');
    const errorDisplay = document.getElementById('errorDisplay');
    const paginationControls = document.getElementById('paginationControls');

    // Show loading state
    tableBody.innerHTML = '';
    loadingIndicator.style.display = 'block';
    errorDisplay.style.display = 'none';
    paginationControls.innerHTML = '';

    // Construct the API URL
    const url = new URL('/citas/admin/issues/data', window.location.origin);
    url.searchParams.append('page', page);
    if (search.trim()) {
        url.searchParams.append('search', search.trim());
    }

    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Network response was not ok: ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            loadingIndicator.style.display = 'none';
            if (data.content.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 20px;">No open issues found.</td></tr>';
                return;
            }
            renderTable(data.content);
            renderPagination(data);
        })
        .catch(error => {
            loadingIndicator.style.display = 'none';
            errorDisplay.style.display = 'block';
            errorDisplay.textContent = `Failed to load issues: ${error.message}`;
            console.error('Fetch error:', error);
        });
}

function renderTable(issues) {
    const tableBody = document.getElementById('issuesTableBody');
    tableBody.innerHTML = issues.map(issue => `
        <tr>
            <td>${issue.id}</td>
            <td>${issue.category}</td>
            <td>${issue.summary}</td>
            <td>${issue.entityType}: ${issue.entityId || 'N/A'}</td>
            <td>${new Date(issue.timestamp).toLocaleString()}</td>
            <td>
                <span class="status-badge status-${issue.status.toLowerCase()}">
                    ${issue.status}
                </span>
            </td>
        </tr>
    `).join('');
}

function renderPagination(pageData) {
    const paginationControls = document.getElementById('paginationControls');
    let html = '';

    // Previous Button
    if (pageData.pageNumber > 0) {
        html += `<button onclick="fetchIssues(${pageData.pageNumber - 1})">&laquo; Prev</button>`;
    }

    // Page Numbers (simplified)
    for (let i = 0; i < pageData.totalPages; i++) {
        const isActive = i === pageData.pageNumber ? 'active' : '';
        html += `<button class="${isActive}" onclick="fetchIssues(${i})">${i + 1}</button>`;
    }

    // Next Button
    if (pageData.pageNumber < pageData.totalPages - 1) {
        html += `<button onclick="fetchIssues(${pageData.pageNumber + 1})">Next &raquo;</button>`;
    }

    paginationControls.innerHTML = html;
}