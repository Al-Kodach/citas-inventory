<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width,initial-scale=1" />
    <title>Citas Admin Panel - Login</title>
    <style>
        :root { --bg: #f5f7fb; --card: #ffffff; --brand: #214E78; --muted: #6b7280; }
        html, body { height: 100%; margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: var(--bg); color: #111; }
        .center-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px; }
        .card { width: 100%; max-width: 400px; background: var(--card); border-radius: 12px; box-shadow: 0 10px 30px rgba(17, 24, 39, 0.08); overflow: hidden; }
        .card-header { background: var(--brand); color: #fff; padding: 24px 28px; }
        .card-header h1 { margin: 0; font-size: 22px; font-weight: 600; }
        .card-body { padding: 28px; }
        label { display: block; margin-bottom: 8px; font-size: 14px; color: var(--muted); font-weight: 500; }
        input[type="text"], input[type="password"] { width: 100%; padding: 12px; border: 1px solid #e6e9ef; border-radius: 8px; font-size: 16px; box-sizing: border-box; }
        input:focus { border-color: var(--brand); outline: none; box-shadow: 0 0 0 2px rgba(33, 78, 120, 0.2); }
        .actions { margin-top: 24px; }
        .btn-container { display: flex; justify-content: center; }
        button { background: var(--brand); color: #fff; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: 600; }
        .hint { font-size: 13px; color: var(--muted); margin-top: 16px; text-align: center; }
        .error { color: #b91c1c; margin-top: 12px; font-size: 14px; font-weight: 500; text-align: center; }
    </style>
</head>
<body>
<div class="center-wrap">
    <div class="card">
        <div class="card-header"><h1>Citas Admin Panel</h1></div>
        <div class="card-body">
            <form action="/citas/admin/login" method="POST">
                <label for="username">Username</label>
                <input id="username" name="username" type="text" required />

                <label for="password" style="margin-top:16px">Password</label>
                <input id="password" name="password" type="password" required />

                <#if error??><div class="error">${error}</div></#if>

                <div class="actions">
                    <div class="btn-container">
                        <button type="submit">Sign In</button>
                    </div>
                </div>

                <div class="hint">Requires admin credentials to continue.</div>
            </form>
        </div>
    </div>
</div>
</body>
</html>