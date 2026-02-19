$ErrorActionPreference = "Stop"

# Always run java from the SAME JDK as javac (prevents Java 8 vs 17 mismatch)
$javacCmd = Get-Command javac -ErrorAction Stop
$jdkBin = Split-Path $javacCmd.Source
$javaExe = Join-Path $jdkBin "java.exe"

if (!(Test-Path $javaExe)) {
  # Fallback (shouldn't happen), use whatever 'java' resolves to
  $javaExe = (Get-Command java -ErrorAction Stop).Source
}

# Fresh build output
if (Test-Path -Path "./out") { Remove-Item -Recurse -Force "./out" }
New-Item -ItemType Directory -Path "./out" | Out-Null

Write-Host "Compiling Java files..."

# Only compile the plain-Java server (no Maven/Spring)
$files = @()
Get-ChildItem -Recurse -Filter *.java -Path "./src/com" | ForEach-Object { $files += $_.FullName }

if ($files.Count -eq 0) {
  throw "No Java source files found under .\\src\\com"
}

# Detect javac major version
$javacVer = & $javacCmd.Source -version 2>&1
$major = 0
if ($javacVer -match "javac\s+([0-9]+)\.") { $major = [int]$Matches[1] }
elseif ($javacVer -match "javac\s+([0-9]+)") { $major = [int]$Matches[1] }

if ($major -ge 9) {
  # JDK 9+ supports --release
  & $javacCmd.Source --release 8 -d .\out $files
} else {
  # JDK 8: use source/target flags
  & $javacCmd.Source -source 1.8 -target 1.8 -d .\out $files
}

Write-Host "Starting server on http://localhost:8080 ..."
& $javaExe -cp .\out com.tzstudies.api.Main
