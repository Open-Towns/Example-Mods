$ErrorActionPreference = "Stop"
Write-Host ""
Write-Host "Open Towns Mod Setup"
Write-Host "--------------------"
Write-Host ""

$modName = Read-Host "Mod name"
$modId = Read-Host "Mod ID"
$modAuthor = Read-Host "Author"


if ([string]::IsNullOrWhiteSpace($modName)) {
    Write-Error "Mod name cannot be empty."
    exit 1
}

if ([string]::IsNullOrWhiteSpace($modId)) {
    Write-Error "Mod ID cannot be empty."
    exit 1
}

if ([string]::IsNullOrWhiteSpace($modAuthor)) {
    $modAuthor = "Unknown"
}

$modId = $modId.ToLower()

if ($modId -notmatch '^[a-z][a-z0-9_.-]*$') {
    Write-Error "Mod ID must start with a letter and contain only lowercase letters, numbers, _, . or -"
    exit 1
}

# Java package names cannot contain hyphens,
# Generate a Java-safe package name separately from the mod ID.
$packageName = $modId `
    -replace '[^a-z0-9.]', ''

if ([string]::IsNullOrWhiteSpace($packageName)) {
    Write-Error "Could not generate a valid Java package name."
    exit 1
}

# Build a Java class name from the mod name.
# Example:
#   Better Dwarves -> BetterDwarvesMod
$className = (($modName -replace '[^a-zA-Z0-9 ]', '') `
    -split '\s+' |
    ForEach-Object {
        if ($_.Length -gt 0) {
            $_.Substring(0, 1).ToUpper() + $_.Substring(1)
        }
    }) -join ''

$className = "${className}Mod"

$mainClass = "${packageName}.${className}"

Write-Host ""
Write-Host "Creating mod:"
Write-Host "  Name:       $modName"
Write-Host "  ID:         $modId"
Write-Host "  Author:     $modAuthor"
Write-Host "  Package:    $packageName"
Write-Host "  Main class: $mainClass"
Write-Host ""

# ------------------------------------------------------------
# Update gradle.properties
# ------------------------------------------------------------

$propertiesPath = Join-Path $PSScriptRoot "gradle.properties"

$properties = Get-Content $propertiesPath -Raw

$properties = $properties -replace '(?m)^modId=.*$', "modId=$modId"
$properties = $properties -replace '(?m)^modName=.*$', "modName=$modName"
$properties = $properties -replace '(?m)^modVersion=.*$', "modVersion=1.0.0"
$properties = $properties -replace '(?m)^modAuthor=.*$', "modAuthor=$modAuthor"
$properties = $properties -replace '(?m)^modPackage=.*$', "modPackage=$packageName"
$properties = $properties -replace '(?m)^modMainClass=.*$', "modMainClass=$mainClass"

Set-Content $propertiesPath $properties


# ------------------------------------------------------------
# Replace starter Java source
# ------------------------------------------------------------

$javaRoot = Join-Path $PSScriptRoot "src\main\java"
$oldPackage = Join-Path $javaRoot "myfirstmod"

if (Test-Path $oldPackage) {
    Remove-Item $oldPackage -Recurse -Force
}

$newPackagePath = Join-Path $javaRoot (
    $packageName.Replace(".", "\")
)

New-Item `
    -ItemType Directory `
    -Path $newPackagePath `
    -Force |
    Out-Null

$javaFile = Join-Path $newPackagePath "$className.java"

$javaSource = @"
package $packageName;

import xaos.api.mods.ModContext;
import xaos.api.mods.TownsMod;

public final class $className implements TownsMod {

    @Override
    public void onLoad(ModContext context) {
        context.logger().info(
                context.metadata().name() + " has loaded!"
        );
    }

    @Override
    public void onWorldLoaded(ModContext context) {
        context.api()
                .ui()
                .showMessage(
                        context.metadata().name()
                                + " is running!"
                );
    }
}
"@

Set-Content $javaFile $javaSource


Write-Host ""
Write-Host "Mod setup complete."
Write-Host ""
Write-Host "Next steps:"
Write-Host "  .\gradlew.bat install"
Write-Host "or"
Write-Host "  .\gradlew.bat dev"
Write-Host ""