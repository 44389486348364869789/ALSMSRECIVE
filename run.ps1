Write-Host "Building and installing the app..." -ForegroundColor Green
.\gradlew installDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "App installed successfully. Launching app on your phone..." -ForegroundColor Green
    & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.alsmsrecive.dev/.MainActivity
    
    Write-Host "Showing logs (Press Ctrl+C to stop)..." -ForegroundColor Yellow
    & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat | Select-String "com.alsmsrecive.dev"
} else {
    Write-Host "Build failed. Please check the errors above." -ForegroundColor Red
}
