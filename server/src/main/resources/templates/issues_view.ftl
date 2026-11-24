<#-- 1. Import the master layout -->
<#import "admin_layout.ftl" as layout>

<#-- 2. Define extra content for the <head> -->
<#assign head_extra>
    <link rel="stylesheet" href="static/css/issues_style.css">
</#assign>

<#-- 3. Call the layout, passing the title and head content -->
<@layout.mainLayout title="System Issues" head_extra=head_extra>

    <div class="header-container">
        <h2>System Issues</h2>
        <div class="filter-controls">
            <input id="searchInput" type="text" placeholder="Search by Entity ID...">
        </div>
    </div>

    <div class="table-container">
        <table id="issuesTable">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Category</th>
                    <th>Summary</th>
                    <th>Entity</th>
                    <th>Timestamp</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody id="issuesTableBody">
                <!-- JavaScript will populate this section -->
            </tbody>
        </table>
    </div>

    <div id="paginationControls" class="pagination-controls">
        <!-- JavaScript will manage pagination buttons -->
    </div>

    <div id="loadingIndicator" class="loading-indicator">
        <p>Loading issues...</p>
    </div>

    <div id="errorDisplay" class="error-display" style="display: none;"></div>

    <#-- The script that powers this page -->
    <script src="/static/js/issues.js"></script>

</@layout.mainLayout>