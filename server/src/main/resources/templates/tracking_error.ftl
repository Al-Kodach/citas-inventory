<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="${basePath}">
    <title>Parcel Not Found</title>
    <link rel="stylesheet" href="static/css/tracking_style.css">
</head>
<body class="page-error">
    <div class="card-container">
        <div class="card-header">
            <h1>Citas Logistics</h1>
            <p>Parcel Tracking</p>
        </div>
        <div class="card-body error-state">
            <h2>Parcel Not Found</h2>
            <p>We could not find any parcel with the tracking code: <strong>${trackingCode}</strong></p>
            <p>Please check the code and try again.</p>
            <a href="." class="button-link">Track Another Parcel</a>
        </div>
        <div class="card-footer">
            <p>&copy; ${currentYear} Citacom Systems LTD. All rights reserved.</p>
        </div>
    </div>
</body>
</html>