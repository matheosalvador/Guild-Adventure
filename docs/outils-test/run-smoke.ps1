# Parcours automatique du jeu (fenetre cachee) : joue un scenario complet, prend des captures PNG et affiche
# "CHECK OK / CHECK ECHEC" pour chaque verification. Dernier message attendu : "FIN: AUCUNE ERREUR".
#
# Usage (depuis la racine du depot) :   .\docs\outils-test\run-smoke.ps1 -Label essai1
# Resultats : target\smoke\<label>\shots\*.png (captures) et target\smoke\<label>\log.txt
# Il faut un JDK (javac et java dans le PATH, ou JAVA_HOME) ; Maven n'est pas necessaire.
param([Parameter(Mandatory=$true)][string]$Label)
$ErrorActionPreference = 'Stop'
$P = (Resolve-Path "$PSScriptRoot\..\..").Path
$W = "$P\target\smoke\$Label"
$B = "$W\classes"; $T = "$W\tools"; $R = "$W\run"; $O = "$W\shots"
if (Test-Path $W) { [System.IO.Directory]::Delete($W, $true) }
New-Item -ItemType Directory $B, $T, $R, $O | Out-Null

# classpath minimal : libgdx + lwjgl (natives Windows) + bullet + gson (classpath.txt est la liste complete du projet)
$all = (Get-Content "$P\classpath.txt" -Raw) -split ';' | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }
$cp = $all | Where-Object {
    ($_ -match 'badlogicgames\\(gdx|gdx-backend-lwjgl3|gdx-bullet|gdx-bullet-platform|gdx-platform|gdx-jnigen-loader)\\' -and $_ -notmatch 'sources|javadoc') -or
    ($_ -match 'jlayer-1.0.1-gdx.jar') -or ($_ -match 'gson-2.10.1.jar') -or
    ($_ -match 'org\\lwjgl\\lwjgl[\w-]*\\3.3.3\\lwjgl[\w-]*-3.3.3(-natives-windows)?\.jar$')
}
$cpStr = $cp -join ';'

# 1) le projet
Get-ChildItem "$P\src\main\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Set-Content "$W\srcs.txt" -Encoding ascii
javac -Xlint:-options -encoding UTF-8 -d $B -cp $cpStr "@$W\srcs.txt"
if ($LASTEXITCODE -ne 0) { throw "compilation du projet echouee" }
$full = "$B;$P\assets;$cpStr"   # assets/ sur le classpath, comme Maven

# 2) l'outil de test
javac -Xlint:-options -encoding UTF-8 -d $T -cp $full "$PSScriptRoot\Smoke.java" "$PSScriptRoot\Compare.java"
if ($LASTEXITCODE -ne 0) { throw "compilation du banc de test echouee" }

# 3) execution (dossier de travail isole : le vrai save.json n'est jamais touche)
Set-Location $R
$ErrorActionPreference = 'Continue'
$out = & java -cp "$T;$full" Smoke $O 2>&1
$out | ForEach-Object { "$_" } | Set-Content "$W\log.txt" -Encoding utf8
$out | Where-Object { "$_" -match 'ECHEC|FIN:|Exception|EXCEPTION' } | ForEach-Object { "$_" } | Select-Object -First 60
"--- captures : $((Get-ChildItem $O -Filter *.png -ErrorAction SilentlyContinue).Count) dans $O"
Set-Location $P
