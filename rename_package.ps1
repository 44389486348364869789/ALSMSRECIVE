$oldPackage = "com.alsmsrecive.dev"
$newPackage = "com.alsmsrecive.dev"
$oldPath = "com\example\alsmsrecive"
$newPath = "com\alsmsrecive\dev"

$basePaths = @(
    "app\src\main\java",
    "app\src\androidTest\java",
    "app\src\test\java"
)

Write-Host "1. Moving directories..."
foreach ($basePath in $basePaths) {
    $fullOldPath = Join-Path $PWD $basePath\$oldPath
    $fullNewPath = Join-Path $PWD $basePath\$newPath
    
    if (Test-Path $fullOldPath) {
        Write-Host "Processing $basePath"
        # Create the parent directory of the new path if it doesn't exist
        $parentNewPath = Split-Path $fullNewPath
        if (-not (Test-Path $parentNewPath)) {
            New-Item -ItemType Directory -Force -Path $parentNewPath | Out-Null
        }
        
        # Rename the deepest folder 'alsmsrecive' to 'dev' and move it
        # Actually, it's safer to just move the contents.
        New-Item -ItemType Directory -Force -Path $fullNewPath | Out-Null
        
        # Move all contents from old to new
        Get-ChildItem -Path $fullOldPath | Move-Item -Destination $fullNewPath -Force
        
        # Remove old directories if empty
        Remove-Item -Path $fullOldPath -Force -Recurse
        
        # Check if 'example' is empty and remove it
        $examplePath = Join-Path $PWD $basePath\com\example
        if (Test-Path $examplePath) {
            $contents = Get-ChildItem -Path $examplePath
            if ($contents.Count -eq 0) {
                Remove-Item -Path $examplePath -Force -Recurse
            }
        }
    }
}

Write-Host "2. Replacing strings in files..."
$extensions = @("*.kt", "*.xml", "*.kts", "*.ps1", "*.md")
foreach ($ext in $extensions) {
    $files = Get-ChildItem -Path $PWD -Filter $ext -Recurse -File | Where-Object { $_.FullName -notmatch "\\build\\" -and $_.FullName -notmatch "\\\.git\\" }
    foreach ($file in $files) {
        $content = Get-Content $file.FullName -Raw
        if ($content -match $oldPackage) {
            $content = $content -replace [regex]::Escape($oldPackage), $newPackage
            Set-Content -Path $file.FullName -Value $content -NoNewline
            Write-Host "Updated: $($file.Name)"
        }
    }
}

Write-Host "Done!"
