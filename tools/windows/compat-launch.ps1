param(
    [string]$GameDir = '',
    [switch]$WithoutOneConfig,
    [int]$Width = 854,
    [int]$Height = 480,
    [string]$LogName = 'compat-test-console.log'
)

$versionRoot = 'E:\game\minecraft pcl\.minecraft\versions\GlideClient-Forge'
$libraryRoot = 'E:\game\minecraft pcl\.minecraft\libraries'
$gameRoot = if ([string]::IsNullOrWhiteSpace($GameDir)) { $versionRoot } else { $GameDir }
$versionJson = Get-Content -Raw (Join-Path $versionRoot 'GlideClient-Forge.json') | ConvertFrom-Json

$classpath = New-Object System.Collections.Generic.List[string]
$seen = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($library in $versionJson.libraries) {
    $parts = $library.name.Split(':')
    if ($parts.Count -ne 3) { continue }
    $groupPath = $parts[0].Replace('.', '\')
    $artifact = $parts[1]
    $version = $parts[2]
    $path = Join-Path $libraryRoot "$groupPath\$artifact\$version\$artifact-$version.jar"
    if ((Test-Path $path) -and $seen.Add($path)) {
        $classpath.Add($path)
    }
}
$classpath.Add((Join-Path $versionRoot 'GlideClient-Forge.jar'))
if (-not $WithoutOneConfig) {
    $classpath.Add((Join-Path $versionRoot 'OneConfig\launchwrapper\OneConfig-Loader.jar'))
}

$java = 'C:\Program Files\BellSoft\LibericaJDK-8\jre\bin\java.exe'
$log = Join-Path $gameRoot $LogName
$args = @(
    '-XX:-OmitStackTraceInFastThrow',
    '-Djdk.lang.Process.allowAmbiguousCommands=True',
    '-Dfml.ignoreInvalidMinecraftCertificates=True',
    '-Dfml.ignorePatchDiscrepancies=True',
    '-Xmx4096m',
    '-Dlog4j2.formatMsgNoLookups=true',
    "-Djava.library.path=$(Join-Path $versionRoot 'GlideClient-Forge-natives')",
    '-cp', ($classpath -join ';'),
    'net.minecraft.launchwrapper.Launch',
    '--username', 'fish_sq',
    '--version', 'GlideClient-Forge',
    '--gameDir', $gameRoot,
    '--assetsDir', 'E:\game\minecraft pcl\.minecraft\assets',
    '--assetIndex', '1.8',
    '--uuid', '2a8212d75d3642059a3a036a0c41e654',
    '--accessToken', '0',
    '--userProperties', '{}',
    '--userType', 'legacy',
    '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker',
    '--width', $Width.ToString(),
    '--height', $Height.ToString()
)

Set-Location $gameRoot
& $java @args *> $log
exit $LASTEXITCODE
