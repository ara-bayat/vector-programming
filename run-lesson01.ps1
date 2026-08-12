# Run Lesson 01 with the required incubator module flag.
# Usage:
#   .\run-lesson01.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

mvn -q compile exec:exec "-Dexec.mainClass=ir.vector.lesson01.ScalarVsVectorAdd"
