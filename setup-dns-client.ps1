# Configuration DNS pour MByte - À exécuter UNE FOIS sur chaque PC client
# PowerShell Admin requis

Write-Host "=================================================="
Write-Host "  MByte DNS Setup - Configuration Automatique"
Write-Host "=================================================="
Write-Host ""

$serverIP = "10.11.127.60"

# Check if running as admin
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")

if (-not $isAdmin) {
    Write-Host "ERROR: Please run this script as Administrator!"
    Write-Host "Right-click PowerShell > Run as Administrator"
    exit 1
}

Write-Host "Configuring DNS to use MByte server: $serverIP"
Write-Host ""

# Get all network adapters that are up
$adapters = Get-NetAdapter | Where-Object {$_.Status -eq "Up"}

if ($adapters.Count -eq 0) {
    Write-Host "ERROR: No active network adapters found!"
    exit 1
}

Write-Host "Active network adapters:"
foreach ($adapter in $adapters) {
    Write-Host "  • $($adapter.Name) - $($adapter.InterfaceDescription)"
}
Write-Host ""

# Ask user which adapter to configure
if ($adapters.Count -eq 1) {
    $selectedAdapter = $adapters[0]
    Write-Host "Using: $($selectedAdapter.Name)"
} else {
    Write-Host "Multiple adapters found. Using first Ethernet/Wi-Fi adapter..."
    $selectedAdapter = $adapters | Where-Object {$_.Name -like "*Ethernet*" -or $_.Name -like "*Wi-Fi*"} | Select-Object -First 1
    if (-not $selectedAdapter) {
        $selectedAdapter = $adapters[0]
    }
    Write-Host "Using: $($selectedAdapter.Name)"
}

Write-Host ""
Write-Host "Configuring DNS servers..."

try {
    # Set DNS servers (MByte DNS first, then fallback to Google)
    Set-DnsClientServerAddress -InterfaceAlias $selectedAdapter.Name -ServerAddresses @($serverIP, "8.8.8.8", "8.8.4.4")
    
    Write-Host "  ✓ DNS servers configured:"
    Write-Host "    Primary:   $serverIP (MByte DNS)"
    Write-Host "    Secondary: 8.8.8.8 (Google DNS)"
    Write-Host "    Tertiary:  8.8.4.4 (Google DNS)"
    
    Write-Host ""
    Write-Host "Flushing DNS cache..."
    ipconfig /flushdns | Out-Null
    
    Write-Host ""
    Write-Host "Testing DNS resolution..."
    
    # Test DNS
    try {
        $result = Resolve-DnsName -Name "www.mbyte.fr" -Server $serverIP -ErrorAction SilentlyContinue
        if ($result) {
            Write-Host "  ✓ DNS resolution working: www.mbyte.fr → $($result.IPAddress)"
        }
    } catch {
        Write-Host "  ⚠ DNS test failed - server may not be ready yet"
    }
    
    Write-Host ""
    Write-Host "=================================================="
    Write-Host "  Setup complete!"
    Write-Host "=================================================="
    Write-Host ""
    Write-Host "All *.mbyte.fr domains will now automatically resolve"
    Write-Host "No need to update hosts file or re-run scripts!"
    Write-Host ""
    Write-Host "You can now access:"
    Write-Host "  • Manager:   http://www.mbyte.fr"
    Write-Host "  • Auth:      http://auth.mbyte.fr"
    Write-Host "  • Consul:    http://consul.mbyte.fr"
    Write-Host "  • Any store: http://<username>-<hash>.s.mbyte.fr"
    Write-Host ""
    Write-Host "To revert DNS settings later, run:"
    Write-Host "  Set-DnsClientServerAddress -InterfaceAlias '$($selectedAdapter.Name)' -ResetServerAddresses"
    Write-Host ""
    
} catch {
    Write-Host ""
    Write-Host "ERROR: Failed to configure DNS"
    Write-Host $_.Exception.Message
    exit 1
}
