# Extrae todos los artistas únicos de un CSV exportado desde Exportify.
# Input:  CSV exportado de Exportify (columna "Artist Name(s)")
# Output: artistas.txt con un artista por línea

$artists = @()
Import-Csv 'C:/Users/plaza/Desktop/hopecore.csv' | ForEach-Object {
    $col = $_.'Artist Name(s)'
    $col.Split(';') | ForEach-Object {
        $artists += $_.Trim()
    }
}
$unique = $artists | Sort-Object -Unique
Write-Host "Total artistas unicos: $($unique.Count)"
$unique | Out-File 'C:/Users/plaza/Desktop/artistas.txt' -Encoding utf8
Write-Host "Guardado en artistas.txt"
