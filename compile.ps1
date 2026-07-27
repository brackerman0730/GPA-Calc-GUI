$ErrorActionPreference = "Stop"

$JAVAFX_LIB = "C:\javafx-sdk-26.0.1\lib"
$OUT_DIR = "out"

if (!(Test-Path $JAVAFX_LIB)) {
    Write-Error "JavaFX lib folder not found: $JAVAFX_LIB"
}

if (Test-Path $OUT_DIR) {
    Remove-Item "$OUT_DIR\*" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $OUT_DIR | Out-Null
}

$JAVA_FILES = Get-ChildItem -Path "src" -Filter "*.java" | ForEach-Object { $_.FullName }

Write-Host "Compiling Java source files..."
javac `
  --module-path $JAVAFX_LIB `
  --add-modules javafx.controls `
  -d $OUT_DIR `
  $JAVA_FILES

Write-Host "Compilation complete."