$ErrorActionPreference = "Stop"

.\compile.ps1

$JAVAFX_LIB = "C:\javafx-sdk-26.0.1\lib"
$OUT_DIR = "out"

Write-Host "Running MainFX..."
java `
  --module-path $JAVAFX_LIB `
  --add-modules javafx.controls `
  -cp $OUT_DIR `
  MainFX