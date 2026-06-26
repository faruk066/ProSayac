Set-Location $PSScriptRoot

Write-Host '=========================================='
Write-Host '  APK Build Basliyor...'
Write-Host '=========================================='

Write-Host '[1/3] Gradle daemon durduruluyor...'
.\gradlew --stop

Write-Host '[2/3] Proje temizleniyor...'
.\gradlew clean

Write-Host '[3/3] Release APK olusturuluyor...'
.\gradlew assembleRelease

Write-Host ''
Write-Host '=========================================='
Write-Host '  ISLEM TAMAMLANDI'
Write-Host "  APK: $PSScriptRoot\app\build\outputs\apk\release\"
Write-Host '=========================================='
Read-Host 'Cikis icin Enter'