<#-- This is the MASTER LAYOUT for the entire admin panel -->
<#macro mainLayout title="Citas Admin Panel" head_extra="">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>${title}</title>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <style>
        :root { --brand: #214E78; --bg: #f5f7fb; --card: #ffffff; --muted: #6b7280; }
        html, body {
            height: 100%;
            margin: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg);
            color: #111;
        }
        .page-wrapper {
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }
        header {
            background-color: var(--brand);
            color: white;
            padding: 1rem 1.5rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
            flex-shrink: 0; /* Prevent header from shrinking */
        }
        header h1 { margin: 0; font-size: 1.25rem; font-weight: 600; }
        .header-actions { display: flex; align-items: center; gap: 1rem; }
        .username { font-size: 0.9rem; font-weight: 500; }
        .logout-btn {
            background-color: rgba(255, 255, 255, 0.1);
            color: white;
            border: 1px solid rgba(255, 255, 255, 0.3);
            border-radius: 6px;
            padding: 0.5rem 1rem;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
            transition: background-color 0.2s;
        }
        .logout-btn:hover { background-color: rgba(255, 255, 255, 0.2); }
        main {
            flex: 1; /* Allow main to grow and take all available space */
            display: flex;
            justify-content: center;
            padding: 1.5rem;
            overflow: auto; /* Handle overflow if content is too tall */
        }
        /* --- THE KEY FIX --- */
        .content-card {
            width: 100%;
            max-width: 1400px; /* Increased max-width for log viewer */
            background: var(--card);
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.07);
            padding: 1.75rem;
            display: flex; /* Make the card a flex container */
            flex-direction: column; /* Stack its children vertically */
        }
        h2 { font-size: 1.5rem; font-weight: 600; margin-top: 0; color: #111827; padding-bottom: 0.5rem; border-bottom: 1px solid #e5e7eb; margin-bottom: 1.5rem; }
        footer {
            text-align: center;
            padding: 1rem;
            font-size: 0.8rem;
            color: var(--muted);
            flex-shrink: 0; /* Prevent footer from shrinking */
        }
    </style>
    ${head_extra}
</head>
<body>
<div class="page-wrapper">
    <header>
        <h1>Citas Admin Panel</h1>
        <div class="header-actions">
            <#if session.username??>
                <span class="username">Welcome, ${session.username}</span>
            </#if>
            <form action="/citas/admin/logout" method="get" style="margin:0;">
                <button type="submit" class="logout-btn">Logout</button>
            </form>
        </div>
    </header>
    <main>
        <div class="content-card">
            <#nested>
        </div>
    </main>
    <footer>
        <p>&copy; ${.now?string("yyyy")} Citacom Systems Ltd. All rights reserved.</p>
    </footer>
</div>
</body>
</html>
</#macro>