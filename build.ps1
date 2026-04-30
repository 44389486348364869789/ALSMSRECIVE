Write-Host "Building the APK file..." -ForegroundColor Green
.\gradlew assembleDebug

if ($LASTEXITCODE -eq 0) {
    $apkPath = "$PWD\app\build\outputs\apk\debug"
    $apkFile = "$apkPath\app-debug.apk"
    
    if (Test-Path $apkFile) {
        Write-Host "Build Successful! Your APK file is ready at:" -ForegroundColor Green
        Write-Host $apkFile -ForegroundColor Cyan
        
        Write-Host "Opening the folder for you..." -ForegroundColor Yellow
        explorer.exe $apkPath
    } else {
        Write-Host "Build finished, but couldn't find the APK file." -ForegroundColor Yellow
    }
} else {
    Write-Host "Build failed. Please check the errors above." -ForegroundColor Red
}
