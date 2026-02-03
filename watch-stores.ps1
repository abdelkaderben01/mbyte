# Script qui surveille automatiquement les nouveaux stores et ajoute/supprime leur DNS
# Lancer en PowerShell Admin : .\watch-stores.ps1

$hostsFile = "C:\Windows\System32\drivers\etc\hosts"
$processedStores = @()
$deletedStores = @()

Write-Host "Surveillance des nouveaux stores et suppressions MByte..."
Write-Host "Appuyez sur Ctrl+C pour arreter"
Write-Host ""

while ($true) {
    try {
        # Récupérer les logs récents
        $logs = docker logs mbyte_manager --tail 100 2>&1 | Out-String
        
        # Chercher les nouveaux stores créés
        $createMatches = [regex]::Matches($logs, "Creating Docker container with identifier: ([a-z0-9-]+)")
        
        foreach ($match in $createMatches) {
            $containerName = $match.Groups[1].Value
            $shortId = $containerName.Substring($containerName.Length - 8)
            
            # Vérifier si déjà traité
            if ($processedStores -contains $shortId) {
                continue
            }
            
            # Le containerName a le format: username-shortId
            $dnsEntry = "127.0.0.1 $containerName.s.mbyte.fr"
            
            # Vérifier si l'entrée existe déjà
            $existingEntry = Get-Content $hostsFile -ErrorAction SilentlyContinue | Select-String -Pattern ([regex]::Escape($dnsEntry)) -Quiet
            
            if (-not $existingEntry) {
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Nouveau store detecte: $shortId"
                Add-Content -Path $hostsFile -Value $dnsEntry
                ipconfig /flushdns | Out-Null
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] DNS ajoute: http://$containerName.s.mbyte.fr"
                Write-Host ""
            }
            
            $processedStores += $shortId
        }
        
        # Détection des stores supprimés : on regarde tous les stores déjà traités
        foreach ($shortId in $processedStores) {
            $containerName = (docker ps -a --format "{{.Names}}" 2>&1 | Where-Object { $_.EndsWith($shortId) })
            if (-not $containerName -and -not ($deletedStores -contains $shortId)) {
                # Le conteneur n'existe plus, on supprime l'entrée DNS
                $dnsEntryPattern = "127.0.0.1 .*${shortId}\.s\.mbyte\.fr"
                $hostsContent = Get-Content $hostsFile -ErrorAction SilentlyContinue
                $dnsLine = $hostsContent | Where-Object { $_ -match $dnsEntryPattern }
                if ($dnsLine) {
                    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Store supprime detecte: $shortId"
                    $newContent = $hostsContent | Where-Object { $_ -ne $dnsLine }
                    Set-Content -Path $hostsFile -Value $newContent
                    ipconfig /flushdns | Out-Null
                    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] DNS supprime: $dnsLine"
                    Write-Host ""
                    $deletedStores += $shortId
                }
            }
        }
    }
    catch {
        Write-Host "[ERREUR] $($_.Exception.Message)"
    }
    
    Start-Sleep -Seconds 5
}
