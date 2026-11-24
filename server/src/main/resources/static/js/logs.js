// A global state variable to hold the currently selected filename.
let activeFileName = null;

// Caches to hold the data fetched from the server.
let logFilesCache = [];
let logContentCache = [];

// --- Main Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    loadLogs();
});


// --- Debounced Functions for User Input ---
let filterDebounce;
function filterLogsDebounced() {
    clearTimeout(filterDebounce);
    filterDebounce = setTimeout(() => loadLogs(), 300);
}

let searchDebounce;
function searchLogsDebounced() {
    clearTimeout(searchDebounce);
    searchDebounce = setTimeout(() => renderLogContent(logContentCache), 300);
}


// --- Core Data Fetching and State Management ---

/**
 * Fetches the list of log files from the server.
 */
function loadLogs() {
    const deviceId = document.getElementById('deviceFilter').value;
    const logListEl = document.getElementById('logList');
    logListEl.innerHTML = '<div class="log-empty">Loading logs...</div>';

    fetch(`/citas/admin/logs/files?deviceId=${deviceId}`)
        .then(res => res.ok ? res.json() : Promise.reject(res))
        .then(logs => {
            logFilesCache = logs;
            renderLogList();
        })
        .catch(() => {
            logListEl.innerHTML = '<div class="log-empty" style="color: red;">Failed to load logs.</div>';
        });
}

/**
 * Called when a user clicks on a log file in the list.
 * This is the main state-changing function for the UI.
 */
function selectLogFile(fileName) {
    // 1. Update the global state.
    activeFileName = fileName;

    // 2. Re-render the entire file list. This is crucial.
    //    It will now correctly apply the '.selected' class based on the new `activeFileName`.
    renderLogList();

    // 3. Update the UI to show the content panel.
    const container = document.getElementById('logsContainer');
    const placeholder = document.getElementById('contentPlaceholder');
    const viewer = document.getElementById('contentViewer');
    const contentEl = document.getElementById('logContent');

    container.classList.add('detail-visible'); // For compact mode responsiveness
    placeholder.style.display = 'none';
    viewer.style.display = 'flex';
    contentEl.textContent = 'Loading and decrypting content...';
    document.getElementById('fileNameHeader').textContent = fileName;

    // 4. Set up the action buttons for the selected file.
    document.getElementById('deleteBtn').onclick = () => deleteLogFile(fileName);
    document.getElementById('exportBtn').href = `/citas/admin/logs/export/${fileName}`;

    // 5. Fetch the content for the selected file.
    fetch(`/citas/admin/logs/content/${fileName}`)
        .then(res => res.ok ? res.text() : Promise.reject(res))
        .then(content => {
            logContentCache = content.split('\n');
            renderLogContent(logContentCache);
        })
        .catch(() => {
            contentEl.textContent = 'Failed to load log content.';
        });
}


// --- Rendering Functions ---

/**
 * Renders the list of log files into the sidebar.
 * It checks the global `activeFileName` to apply the 'selected' class.
 */
function renderLogList() {
    const logListEl = document.getElementById('logList');
    if (logFilesCache.length === 0) {
        logListEl.innerHTML = '<div class="log-empty">No log files found.</div>';
        return;
    }

    logListEl.innerHTML = logFilesCache.map(log => {
        const date = new Date(log.lastModified);
        const formattedDate = date.toLocaleString(undefined, {
            year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
        const sizeKB = (log.sizeBytes / 1024).toFixed(1);

        // This ternary operator is the key to highlighting.
        const isSelectedClass = log.fileName === activeFileName ? 'selected' : '';

        return `
            <div class="log-list-item ${isSelectedClass}" onclick="selectLogFile('${log.fileName}')">
                <div class="file-name">${log.deviceId}</div>
                <div class="file-meta">${formattedDate} - ${sizeKB} KB</div>
            </div>
        `;
    }).join('');
}

/**
 * Renders the content of a log file into the main panel, applying any search filters.
 */
function renderLogContent(lines) {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const contentEl = document.getElementById('logContent');
    
    let filteredLines = lines;
    if (searchTerm) {
        filteredLines = lines.filter(line => line.toLowerCase().includes(searchTerm));
    }
    
    contentEl.textContent = filteredLines.join('\n');
}

/**
 * In compact mode, this function would be called by a "Back" button
 * to hide the detail view and show the list again.
 */
function showLogList() {
    document.getElementById('logsContainer').classList.remove('detail-visible');
    activeFileName = null;
    renderLogList(); // Re-render to remove selection highlight
}

