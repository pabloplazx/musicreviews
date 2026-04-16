# Extrae los artistas que más aparecen en el CSV (los más escuchados).
# Input:  CSV exportado de Exportify (columna "Artist Name(s)")
# Output: top_artistas.txt con los 80 artistas más frecuentes, uno por línea

$counts = @{}
Import-Csv 'C:/Users/plaza/Desktop/hopecore.csv' | ForEach-Object {
    $col = $_.'Artist Name(s)'
    $col.Split(';') | ForEach-Object {
        $name = $_.Trim()
        if ($name) {
            if ($counts.ContainsKey($name)) { $counts[$name]++ }
            else { $counts[$name] = 1 }
        }
    }
}
$top = $counts.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 80
Write-Host "Top 80 artistas por apariciones:"
$top | ForEach-Object { Write-Host "$($_.Value) - $($_.Key)" }
$top | Select-Object -ExpandProperty Key | Out-File 'C:/Users/plaza/Desktop/top_artistas.txt' -Encoding utf8
Write-Host "Guardado en top_artistas.txt"
