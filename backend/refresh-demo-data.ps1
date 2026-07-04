param(
  [string]$BaseUrl = 'http://localhost:8080',
  [string]$Container = 'tourlog-postgres'
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlPath = Join-Path $scriptDir 'demo-data.sql'

docker cp $sqlPath "${Container}:/tmp/demo-data.sql"
docker exec $Container psql -U tourlog_user -d tourlogdb -f /tmp/demo-data.sql

$loginBody = @{
  email = 'demo@tourplanner.local'
  password = 'demo1234'
} | ConvertTo-Json

$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.token)" }
$tours = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/tours" -Headers $headers

foreach ($tour in $tours) {
  $body = @{
    id = $tour.id
    name = $tour.name
    description = $tour.description
    from = $tour.from
    to = $tour.to
    transportType = $tour.transportType
    fromLat = $tour.fromLat
    fromLng = $tour.fromLng
    toLat = $tour.toLat
    toLng = $tour.toLng
    distance = $tour.distance
    estimatedTime = $tour.estimatedTime
    childFriendliness = $tour.childFriendliness
    routeImagePath = $tour.routeImagePath
    routeGeometry = $tour.routeGeometry
    createdAt = $tour.createdAt
  } | ConvertTo-Json -Depth 8

  Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/tours/$($tour.id)" -Headers $headers -ContentType 'application/json' -Body $body | Out-Null
}

$updatedTours = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/tours" -Headers $headers
$updatedTours | ForEach-Object {
  $points = 0
  if ($_.routeGeometry) {
    $points = (($_.routeGeometry | ConvertFrom-Json).Count)
  }
  [pscustomobject]@{
    id = $_.id
    distance = $_.distance
    durationMinutes = [math]::Round($_.estimatedTime / 60, 1)
    routePoints = $points
  }
} | Sort-Object id
