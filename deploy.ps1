Write-Host ">> Building Images..." -ForegroundColor Cyan
docker compose build

Write-Host ">> Tagging Images..." -ForegroundColor Cyan
docker tag catfacts-minecraft:latest h1ggsk/catfacts-rust:latest
docker tag catfacts-discord:latest h1ggsk/catfacts-discord:latest
docker tag catfacts-web:latest h1ggsk/catfacts-web:latest

Write-Host ">> Pushing to Docker Hub..." -ForegroundColor Cyan
docker push h1ggsk/catfacts-rust:latest
docker push h1ggsk/catfacts-discord:latest
docker push h1ggsk/catfacts-web:latest

Write-Host ">> DONE! 🚀" -ForegroundColor Green
Write-Host "Now go to your VPS and run:" -ForegroundColor Yellow
Write-Host "docker compose pull && docker compose up -d --force-recreate" -ForegroundColor White