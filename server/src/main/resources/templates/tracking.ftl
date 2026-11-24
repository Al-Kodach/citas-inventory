<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Track Parcel: ${trackingCode}</title>
    <link rel="stylesheet" href="static/css/tracking_style.css">
</head>
<body class="page-success">
    <div class="flat-container">
        <header>
            <h1>Citas Logistics</h1>
            <p class="tagline">Your trusted delivery partner</p>
        </header>

        <main class="tracking-details">
            <div class="detail-item">
                <p class="label">Tracking Code</p>
                <p class="value-large">${trackingCode}</p>
            </div>

            <div class="detail-item">
                <p class="label">Current Status</p>
                <p class="value-large status">${status}</p>
            </div>

            <div class="history-section">
                <h2>Tracking History</h2>
                <#if timeline?has_content>
                    <ul class="timeline">
                        <#list timeline as event>
                            <li class="timeline-item">
                                <p class="description">• ${event.label}</p>
                                <p class="metadata">${event.metadata}</p>
                            </li>
                        </#list>
                    </ul>
                <#else>
                    <p>No tracking history available yet.</p>
                </#if>
            </div>
        </main>

        <footer>
            <p>&copy; ${currentYear} Citacom Systems LTD. All rights reserved.</p>
        </footer>
    </div>
</body>
</html>