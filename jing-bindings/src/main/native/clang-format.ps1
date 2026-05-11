$ErrorActionPreference = "Stop"

$clangFormat = Get-Command clang-format -ErrorAction SilentlyContinue
if (-not $clangFormat) {
    Write-Host "clang-format not found" -ForegroundColor Red
    exit 1
}

$files = Get-ChildItem -Recurse -Path "src" -Include *.c, *.h, *.cpp

if ($files.Count -eq 0) {
    exit 0
}

foreach ($file in $files) {
    & $clangFormat -i --style=file $file.FullName
    Write-Host "Formatting : $($file.FullName)"
}

Write-Host "Formatting successfully." -ForegroundColor Green