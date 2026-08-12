# Run Lesson 03 with the required incubator module flag.
# Usage:
#   .\run-lesson03.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

mvn -q compile exec:exec "-Dexec.mainClass=ir.vector.lesson03.ByteKernelBenchmark"
