$ErrorActionPreference = "Stop"

$repo = Get-Location
$target = Join-Path $repo "app\src\main\java\com\simple\medai\screens\StudySessionScreen.kt"
$source = Join-Path $repo "UPDATE_23_FILES\StudySessionScreen.kt"

if (!(Test-Path $target)) {
    throw "Ejecuta este script desde la carpeta raiz de SIMPLE."
}
if (!(Test-Path $source)) {
    throw "Falta UPDATE_23_FILES\StudySessionScreen.kt. Extrae TODO el ZIP dentro de SIMPLE."
}

Copy-Item $source $target -Force

$ai = Join-Path $repo "app\src\main\java\com\simple\medai\data\SimpleAiRepository.kt"
$text = Get-Content $ai -Raw -Encoding UTF8

if ($text -notmatch "data class AiSource") {
    $old = "data class SimpleAiResult(`r`n    val answer: String,"
    $new = "data class AiSource(`r`n    val type: String,`r`n    val title: String,`r`n    val url: String? = null`r`n)`r`n`r`ndata class SimpleAiResult(`r`n    val answer: String,"
    if (-not $text.Contains($old)) {
        $old = "data class SimpleAiResult(`n    val answer: String,"
        $new = "data class AiSource(`n    val type: String,`n    val title: String,`n    val url: String? = null`n)`n`ndata class SimpleAiResult(`n    val answer: String,"
    }
    $text = $text.Replace($old, $new)
}

if ($text -notmatch "val sources: List<AiSource>") {
    $text = $text -replace 'val artifactType: String\? = null\s*\)', 'val artifactType: String? = null,`n    val sources: List<AiSource> = emptyList()`n)'
}

if ($text -notmatch "val sourceList = mutableListOf<AiSource>") {
    $insert = @"
        val sourceList = mutableListOf<AiSource>()
        val sourcesJson = json.optJSONArray("sources")
        if (sourcesJson != null) {
            for (i in 0 until sourcesJson.length()) {
                val item = sourcesJson.optJSONObject(i) ?: continue
                val title = item.optString("title").trim()
                if (title.isBlank()) continue
                sourceList += AiSource(
                    type = item.optString("type", "web"),
                    title = title,
                    url = item.optString("url").takeIf { it.isNotBlank() }
                )
            }
        }

"@
    $text = $text.Replace("        SimpleAiResult(", $insert + "        SimpleAiResult(")
}

if ($text -notmatch "sources = sourceList") {
    $text = $text.Replace(
        '            artifactType = json.optString("artifact_type").takeIf { it.isNotBlank() }',
        '            artifactType = json.optString("artifact_type").takeIf { it.isNotBlank() },' + "`r`n" + '            sources = sourceList'
    )
}

Set-Content $ai $text -Encoding UTF8

$app = Join-Path $repo "app\src\main\java\com\simple\medai\screens\SimpleApp.kt"
$appText = Get-Content $app -Raw -Encoding UTF8

if ($appText -notmatch "rememberSaveable") {
    $appText = $appText.Replace(
        "import androidx.compose.runtime.*",
        "import androidx.compose.runtime.*`r`nimport androidx.compose.runtime.saveable.rememberSaveable"
    )
}

$appText = $appText.Replace(
    "    var screen by remember {",
    "    var screen by rememberSaveable {"
)

Set-Content $app $appText -Encoding UTF8

$manifest = Join-Path $repo "app\src\main\AndroidManifest.xml"
$m = Get-Content $manifest -Raw -Encoding UTF8

if ($m -notmatch "android:configChanges=") {
    $m = $m.Replace(
        '            android:windowSoftInputMode="adjustResize">',
        '            android:windowSoftInputMode="adjustResize"' + "`r`n" + '            android:configChanges="orientation|screenSize|keyboardHidden|uiMode">'
    )
}

Set-Content $manifest $m -Encoding UTF8

Write-Host ""
Write-Host "UPDATE 23 APLICADO" -ForegroundColor Green
Write-Host "- Zoom de respuestas"
Write-Host "- Fuentes reales debajo de las respuestas"
Write-Host "- Conservacion del chat al rotar/cambiar configuracion"
Write-Host ""
Write-Host "Ahora ejecuta: .\gradlew.bat assembleDebug" -ForegroundColor Yellow
