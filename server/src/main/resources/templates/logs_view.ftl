<#import "admin_layout.ftl" as layout>

<#assign head_extra>
    <link rel="stylesheet" href="static/css/logs_style.css">
</#assign>

<@layout.mainLayout title="Device Log Viewer" head_extra=head_extra>

    <div class="header-container">
        <h2>Device Log Viewer</h2>
        <div class="filter-controls">
            <input id="deviceFilter" type="text" placeholder="Filter by Device ID..." oninput="filterLogsDebounced()" />
        </div>
    </div>

    <div class="logs-container" id="logsContainer">
        <!-- Left Panel: File List -->
        <aside class="logs-sidebar" id="logsSidebar">
            <div id="logList" class="log-list">
                <div class="log-empty">Loading logs...</div>
            </div>
        </aside>

        <!-- Right Panel: Log Content -->
        <main class="logs-content" id="logsContent">
            <!-- Placeholder shown by default -->
            <div class="content-placeholder" id="contentPlaceholder">
                <div class="placeholder-icon">📄</div>
                <p>Select a log file from the left panel to view its content.</p>
            </div>

            <!-- This entire block is hidden until a file is selected -->
            <div class="content-viewer" id="contentViewer" style="display:none;">
                <div class="content-header">
                    <h3 id="fileNameHeader"></h3>
                    <div class="actions">
                        <input type="text" id="searchInput" placeholder="Search in file..." oninput="searchLogsDebounced()">
                        <a id="exportBtn" class="action-btn" target="_blank">Export</a>
                        <button id="deleteBtn" class="action-btn danger">Delete</button>
                    </div>
                </div>

                <!-- --- THE FINAL FIX --- -->
                <!-- The <pre> tag is now INSIDE the content-viewer div -->
                <pre id="logContent"></pre>
            </div>
        </main>
    </div>

    <script src="/static/js/logs.js"></script>

</@layout.mainLayout>