# Chạy backend với cấu hình email Gmail
# Đọc biến từ .env (nếu có), sau đó khởi động Spring Boot

if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim().Trim('"').Trim("'")
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

# Mặc định nếu chưa có .env
if (-not $env:APP_FRONTEND_BASE_URL) { $env:APP_FRONTEND_BASE_URL = "http://localhost:5173" }

mvn spring-boot:run
