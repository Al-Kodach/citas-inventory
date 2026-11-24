<#-- 1. Import the master layout file -->
<#import "admin_layout.ftl" as layout>

<#-- 2. Call the mainLayout macro. All content inside this block will be injected into the layout. -->
<@layout.mainLayout title="Admin Hub">

    <#-- These styles are specific to this hub page -->
    <style>
        .hub-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
            gap: 1.5rem;
        }
        .action-card {
            background-color: #f9fafb;
            border: 1px solid #e5e7eb;
            border-radius: 10px;
            padding: 1.5rem;
            text-align: center;
            text-decoration: none;
            color: #111;
            transition: all 0.2s ease-in-out;
        }
        .action-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);
            border-color: var(--brand);
        }
        .action-card-icon {
            font-size: 2.5rem; /* Larger icon */
            line-height: 1;
            color: var(--brand);
        }
        .action-card h3 {
            margin: 0.75rem 0 0.25rem 0;
            font-size: 1.1rem;
            font-weight: 600;
        }
        .action-card p {
            margin: 0;
            font-size: 0.9rem;
            color: var(--muted);
            line-height: 1.4;
        }
    </style>

    <h2>Admin Tools</h2>
    <div class="hub-grid">
        <a href="/citas/admin/logs" class="action-card">
            <div class="action-card-icon">📄</div>
            <h3>Device Logs</h3>
            <p>View and debug logs uploaded from POS devices.</p>
        </a>

        <a href="/citas/admin/issues" class="action-card">
            <div class="action-card-icon">⚠️</div>
            <h3>System Issues</h3>
            <p>Review and resolve system-generated issues.</p>
        </a>

        <div class="action-card" style="opacity: 0.5; cursor: not-allowed; background-color: #f3f4f6;">
            <div class="action-card-icon">📈</div>
            <h3>Analytics</h3>
            <p>Coming soon...</p>
        </div>
    </div>

</@layout.mainLayout>