$ErrorActionPreference = "Stop"

foreach ($line in Get-Content .env) {
    $trimmed = $line.Trim()

    if ($trimmed -eq "" -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) {
        continue
    }

    $parts = $line.Split("=", 2)
    Set-Item -Path ("Env:" + $parts[0].Trim()) -Value $parts[1].Trim().Trim('"')
}

$dbUrl = "postgresql+psycopg://$($env:DB_USERNAME):$($env:DB_PASSWORD)@$($env:DB_HOST):8085/$($env:DB_NAME)"

& ".\.venv\Scripts\python.exe" `
    ".\analytics\etl_pipeline.py" `
    --db-url $dbUrl `
    --output-dir ".\analytics\output-real" `
    --save-clean-csv `
    --load-reporting



