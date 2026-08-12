# Run Lesson 02 with the required incubator module flag.
# Usage:
#   .\run-lesson02.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

mvn -q compile exec:exec "-Dexec.mainClass=ir.vector.lesson02.ComplexKernelBenchmark"
