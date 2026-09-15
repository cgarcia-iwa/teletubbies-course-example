Get-Content "$PSScriptRoot\..\local.env" | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
    $name, $value = $_.Split('=', 2)
    Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim()
}