<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
     <base href="${basePath}">
    <title>Track Your Parcel</title>
    <link rel="stylesheet" href="static/css/tracking_style.css">
</head>
<body class="page-search">
    <div class="card-container">
        <div class="card-header">
            <h1>Citas Logistics</h1>
            <p>Track Your Parcel</p>
        </div>
        <div class="card-body">
            <h2>Track Your Parcel</h2>
            <p>Enter your tracking code below to see its status and history.</p>

            <!-- This form now submits to the correct relative path -->
            <form onsubmit="event.preventDefault(); window.location.href = document.getElementById('trackingCode').value.trim();">
                <div class="input-group">
                    <input type="text" id="trackingCode" name="trackingCode" placeholder="e.g., ACCDEMO001" required>
                    <button type="submit">Track</button>
                </div>
            </form>
        </div>
        <div class="card-footer">
            <p>&copy; ${currentYear} Citacom Systems LTD. All rights reserved.</p>
        </div>
    </div>
</body>
</html>