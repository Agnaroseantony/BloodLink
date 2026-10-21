param(
    [switch]$Test,
    [switch]$PostgresTest,
    [switch]$Package,
    [ValidateRange(1024,65535)][int]$Port = 8080
)
$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    $portableJdk = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '.tools') -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($portableJdk) {
        $env:JAVA_HOME = $portableJdk.FullName
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    } elseif ($env:JAVA_HOME -and (Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) {
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    }
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install JDK 21 and set JAVA_HOME. See README.md.' }
    $portableMaven = Join-Path $PSScriptRoot '.tools\apache-maven-3.9.11\bin\mvn.cmd'
    if (Test-Path -LiteralPath $portableMaven) { $mavenCommand = $portableMaven }
    elseif (Get-Command mvn -ErrorAction SilentlyContinue) { $mavenCommand = 'mvn' }
    else { throw 'Install Maven 3.9.11 (or Maven 3.6.3+) and add its bin folder to PATH. See README.md.' }
    $mavenArgs = @('-B', '-ntp')
    if (Test-Path -LiteralPath '.tools\m2') { $mavenArgs += '-Dmaven.repo.local=.tools/m2' }
    if ($PostgresTest) { $mavenArgs += @('-Ppostgres-test', 'test') }
    elseif ($Test) { $mavenArgs += 'test' }
    elseif ($Package) { $mavenArgs += 'package' }
    else {
        $mavenArgs += @('spring-boot:run', "-Dspring-boot.run.arguments=--server.port=$Port")
        Write-Host "Starting BloodLink at http://localhost:$Port (Ctrl+C stops the app)." -ForegroundColor Magenta
    }
    & $mavenCommand @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven finished with exit code $LASTEXITCODE." }
} finally { Pop-Location }
