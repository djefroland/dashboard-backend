# 1. Utiliser System.Net.WebClient
Write-Host "`n1. Test avec System.Net.WebClient:" -ForegroundColor Cyan
$webClient = New-Object System.Net.WebClient
try {
    $webClient.Headers.Add("User-Agent", "PowerShell Script")
    $response = $webClient.DownloadString("http://localhost:8080/api/v1/test/token?username=test")
    Write-Host "Succès!" -ForegroundColor Green
    Write-Host $response
} catch {
    Write-Host "Erreur:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 2. Utiliser System.Net.HttpWebRequest
Write-Host "`n2. Test avec System.Net.HttpWebRequest:" -ForegroundColor Cyan
try {
    $request = [System.Net.WebRequest]::Create("http://localhost:8080/api/v1/test/token?username=test")
    $request.Method = "GET"
    $request.UserAgent = "PowerShell Script"
    
    $response = $request.GetResponse()
    $responseStream = $response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($responseStream)
    $responseContent = $reader.ReadToEnd()
    
    Write-Host "Succès!" -ForegroundColor Green
    Write-Host $responseContent
    
    $reader.Close()
    $response.Close()
} catch {
    Write-Host "Erreur:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 3. Utiliser Invoke-WebRequest (plutôt que Invoke-RestMethod)
Write-Host "`n3. Test avec Invoke-WebRequest:" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/test/token?username=test" -Method GET -ErrorAction Stop
    Write-Host "Succès!" -ForegroundColor Green
    Write-Host $response.Content
} catch {
    Write-Host "Erreur:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}
