# PowerShell script to load fake data into PostgreSQL
# Make sure docker-compose services are running first

param(
    [string]$ContainerName = "postgres-db",
    [string]$DbUser = "user",
    [string]$DbName = "mydb",
    [string]$SqlFile = ".\db-init\03-seed-fake-data.sql"
)

Write-Host "Loading fake data into PostgreSQL database..."

# Check if container is running
$containerStatus = docker inspect -f '{{.State.Running}}' $ContainerName 2>$null
if ($containerStatus -ne "true") {
    Write-Host "ERROR: Container is not running"
    Write-Host "Start with: docker-compose up -d postgres"
    exit 1
}

# Check if SQL file exists
if (-not (Test-Path $SqlFile)) {
    Write-Host "ERROR: SQL file not found: $SqlFile"
    exit 1
}

# Execute SQL file
Write-Host "Executing SQL file in container..."
$sqlContent = Get-Content $SqlFile -Raw
$sqlContent | docker exec -i $ContainerName psql -U $DbUser -d $DbName

if ($LASTEXITCODE -eq 0) {
    Write-Host "Success: Data loaded successfully!"
    Write-Host "You can now query with:"
    Write-Host "  docker exec -it $ContainerName psql -U $DbUser -d $DbName -c '\dt'"
    Write-Host "  docker exec -it $ContainerName psql -U $DbUser -d $DbName -c 'SELECT COUNT(*) FROM evaluations;'"
} else {
    Write-Host "ERROR: Failed to load data"
    exit 1
}


