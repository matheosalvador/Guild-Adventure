# Visualiseur de modeles 3D (outil de conception) : affiche le joueur equipe, les PNJ et les monstres sous plusieurs
# angles (face, 3/4, profil, dos), la marche et le coup, et enregistre des captures PNG.
#
# Usage (depuis la racine du depot) :   .\docs\outils-test\run-viewer.ps1 -Label essai1
# Resultats : target\viewer\<label>\shots\*.png
# ModelViewer.java est dans le package com.lucas.guild.libgdx pour voir les classes internes (CharacterModel...).
param([Parameter(Mandatory=$true)][string]$Label)
$ErrorActionPreference = 'Stop'
$P = (Resolve-Path "$PSScriptRoot\..\..").Path
$W = "$P\target\viewer\$Label"
$B = "$W\classes"; $T = "$W\tools"; $R = "$W\run"; $O = "$W\shots"
if (Test-Path $W) { [System.IO.Directory]::Delete($W, $true) }
New-Item -ItemType Directory $B, $T, $R, $O | Out-Null

$all = (Get-Content "$P\classpath.txt" -Raw) -split ';' | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }
$cp = $all | Where-Object {
    ($_ -match 'badlogicgames\\(gdx|gdx-backend-lwjgl3|gdx-bullet|gdx-bullet-platform|gdx-platform|gdx-jnigen-loader)\\' -and $_ -notmatch 'sources|javadoc') -or
    ($_ -match 'jlayer-1.0.1-gdx.jar') -or ($_ -match 'gson-2.10.1.jar') -or
    ($_ -match 'org\\lwjgl\\lwjgl[\w-]*\\3.3.3\\lwjgl[\w-]*-3.3.3(-natives-windows)?\.jar$')
}
$cpStr = $cp -join ';'
Get-ChildItem "$P\src\main\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Set-Content "$W\srcs.txt" -Encoding ascii
javac -Xlint:-options -encoding UTF-8 -d $B -cp $cpStr "@$W\srcs.txt"
if ($LASTEXITCODE -ne 0) { throw "compilation du projet echouee" }
$full = "$B;$P\assets;$cpStr"
javac -Xlint:-options -encoding UTF-8 -d $T -cp $full "$PSScriptRoot\ModelViewer.java"
if ($LASTEXITCODE -ne 0) { throw "compilation du visualiseur echouee" }
Set-Location $R
$ErrorActionPreference = 'Continue'
$out = & java -cp "$T;$full" com.lucas.guild.libgdx.ModelViewer $O 2>&1
$out | ForEach-Object { "$_" } | Select-Object -Last 8
"--- captures : $((Get-ChildItem $O -Filter *.png -ErrorAction SilentlyContinue).Count) dans $O"
Set-Location $P
