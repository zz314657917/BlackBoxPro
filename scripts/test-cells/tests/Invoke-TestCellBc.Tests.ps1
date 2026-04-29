$ErrorActionPreference = 'Stop'

$commonPath = Join-Path $PSScriptRoot '..\TestCellBcCommon.ps1'
. $commonPath

function New-TestCellJsonFixture {
    param(
        [string]$Path,
        [object[]]$Cells
    )

    $payload = [ordered]@{
        version = 1
        defaultCellId = $Cells[0].id
        defaults = [ordered]@{
            enabled = $false
        }
        cells = $Cells
    }

    [System.IO.File]::WriteAllText(
        $Path,
        ($payload | ConvertTo-Json -Depth 10),
        [System.Text.UTF8Encoding]::new($false))
}

function New-ServerPropertiesFixture {
    param(
        [string]$ServerDir,
        [string]$ServerIp
    )

    New-Item -ItemType Directory -Path $ServerDir -Force | Out-Null
    $content = @(
        "server-ip=$ServerIp"
        'server-port=0'
    )
    [System.IO.File]::WriteAllLines(
        (Join-Path $ServerDir 'server.properties'),
        $content,
        [System.Text.UTF8Encoding]::new($false))
}

function New-TestCellLeaseFixtureConfig {
    param([string]$Path)

    New-TestCellJsonFixture -Path $Path -Cells @(
        [ordered]@{
            id = 'cell-01'
            enabled = $true
            serverDir = (Join-Path (Split-Path -Parent $Path) 'server-cell-01')
            serverPort = 25570
        },
        [ordered]@{
            id = 'cell-02'
            enabled = $true
            serverDir = (Join-Path (Split-Path -Parent $Path) 'server-cell-02')
            serverPort = 25575
        },
        [ordered]@{
            id = 'cell-03'
            enabled = $true
            serverDir = (Join-Path (Split-Path -Parent $Path) 'server-cell-03')
            serverPort = 25585
        }
    )
}

Describe 'TestCellBcCommon' {
    It 'uses hidden clients for automation and normal windows when ShowClient is requested' {
        (Get-TestCellClientWindowStyle -ShowClient:$false).ToString() | Should Be 'Hidden'
        (Get-TestCellClientWindowStyle -ShowClient:$true).ToString() | Should Be 'Normal'
    }

    It 'builds command-line path patterns that match slash and backslash variants' {
        $pattern = Get-TestCellCommandLinePathPattern -Path 'F:/minecraft/test-cells/server-cell-02'

        'cmd /k cd /d "F:\minecraft\test-cells\server-cell-02"' | Should Match $pattern
        'java -Dblackboxpro.serverDir="F:/minecraft/test-cells/server-cell-02" -jar catserver.jar' | Should Match $pattern
    }

    It 'discovers descendant processes recursively from controller pids' {
        $processes = @(
            [pscustomobject]@{ ProcessId = 100; ParentProcessId = 1; Name = 'cmd.exe' },
            [pscustomobject]@{ ProcessId = 101; ParentProcessId = 100; Name = 'java.exe' },
            [pscustomobject]@{ ProcessId = 102; ParentProcessId = 101; Name = 'conhost.exe' },
            [pscustomobject]@{ ProcessId = 200; ParentProcessId = 1; Name = 'java.exe' }
        )

        $descendants = Get-TestCellDescendantProcesses -RootProcessIds @(100) -AllProcesses $processes

        (@($descendants | Select-Object -ExpandProperty ProcessId) -join ',') | Should Be '101,102'
    }

    It 'discovers enabled backends and falls back to localhost when server-ip is blank' {
        $root = Join-Path $env:TEMP ("bc-fixture-" + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $root -Force | Out-Null

        try {
            $server01 = Join-Path $root 'server-cell-01'
            $server02 = Join-Path $root 'server-cell-02'
            New-ServerPropertiesFixture -ServerDir $server01 -ServerIp ''
            New-ServerPropertiesFixture -ServerDir $server02 -ServerIp '192.168.50.22'

            $configPath = Join-Path $root 'cells.json'
            New-TestCellJsonFixture -Path $configPath -Cells @(
                [ordered]@{
                    id = 'cell-01'
                    enabled = $true
                    serverDir = $server01
                    serverPort = 25570
                },
                [ordered]@{
                    id = 'cell-02'
                    enabled = $true
                    serverDir = $server02
                    serverPort = 25575
                }
            )

            $result = Get-TestCellBcBackends -SourceConfigPaths @($configPath) -DefaultBackendId 'cell-02'

            $result.backends.Count | Should Be 2
            $result.backends[0].id | Should Be 'cell-01'
            $result.backends[0].address | Should Be '127.0.0.1:25570'
            $result.backends[1].id | Should Be 'cell-02'
            $result.backends[1].address | Should Be '192.168.50.22:25575'
            $result.defaultBackendId | Should Be 'cell-02'
        }
        finally {
            if (Test-Path -LiteralPath $root) {
                Remove-Item -LiteralPath $root -Recurse -Force
            }
        }
    }

    It 'uses the first discovered backend when no explicit default is configured' {
        $root = Join-Path $env:TEMP ("bc-fixture-" + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $root -Force | Out-Null

        try {
            $server01 = Join-Path $root 'server-cell-01'
            $server02 = Join-Path $root 'server-cell-02'
            New-ServerPropertiesFixture -ServerDir $server01 -ServerIp ''
            New-ServerPropertiesFixture -ServerDir $server02 -ServerIp ''

            $configPath = Join-Path $root 'cells.json'
            New-TestCellJsonFixture -Path $configPath -Cells @(
                [ordered]@{
                    id = 'cell-01'
                    enabled = $true
                    serverDir = $server01
                    serverPort = 25570
                },
                [ordered]@{
                    id = 'cell-02'
                    enabled = $true
                    serverDir = $server02
                    serverPort = 25575
                }
            )

            $result = Get-TestCellBcBackends -SourceConfigPaths @($configPath)

            $result.defaultBackendId | Should Be 'cell-01'
        }
        finally {
            if (Test-Path -LiteralPath $root) {
                Remove-Item -LiteralPath $root -Recurse -Force
            }
        }
    }

    It 'skips cells that do not expose server.properties and returns no default when nothing is discovered' {
        $root = Join-Path $env:TEMP ("bc-fixture-" + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $root -Force | Out-Null

        try {
            $server03 = Join-Path $root 'server-cell-03'
            New-Item -ItemType Directory -Path $server03 -Force | Out-Null

            $configPath = Join-Path $root 'cells.json'
            New-TestCellJsonFixture -Path $configPath -Cells @(
                [ordered]@{
                    id = 'cell-03'
                    enabled = $true
                    serverDir = $server03
                    serverPort = 25585
                }
            )

            $result = Get-TestCellBcBackends -SourceConfigPaths @($configPath)

            $result.backends.Count | Should Be 0
            $result.defaultBackendId | Should Be $null
            $result.skipped.Count | Should Be 1
            $result.skipped[0].reason | Should Be 'missing-server-properties'
        }
        finally {
            if (Test-Path -LiteralPath $root) {
                Remove-Item -LiteralPath $root -Recurse -Force
            }
        }
    }

    It 'renders a Waterfall config that includes discovered servers and priorities' {
        $text = New-TestCellBcWaterfallConfigText -Config @{
            listenHost = '127.0.0.1'
            listenPort = 25645
            onlineMode = $false
            ipForward = $true
            forgeSupport = $false
            motd = '&6BlackBoxPro BC'
            maxPlayers = 20
        } -Backends @(
            [pscustomobject]@{
                id = 'cell-01'
                address = '127.0.0.1:25570'
            },
            [pscustomobject]@{
                id = 'cell-02'
                address = '127.0.0.1:25575'
            }
        ) -DefaultBackendId 'cell-01'

        $text | Should Match 'host: 127\.0\.0\.1:25645'
        $text | Should Match 'servers:'
        $text | Should Match 'cell-01:'
        $text | Should Match 'address: 127\.0\.0\.1:25570'
        $text | Should Match '- cell-01'
    }

    It 'can restrict discovery to an explicit backend subset' {
        $root = Join-Path $env:TEMP ("bc-fixture-" + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $root -Force | Out-Null

        try {
            $server01 = Join-Path $root 'server-cell-01'
            $server02 = Join-Path $root 'server-cell-02'
            $server03 = Join-Path $root 'server-cell-03'
            New-ServerPropertiesFixture -ServerDir $server01 -ServerIp ''
            New-ServerPropertiesFixture -ServerDir $server02 -ServerIp ''
            New-ServerPropertiesFixture -ServerDir $server03 -ServerIp ''

            $configPath = Join-Path $root 'cells.json'
            New-TestCellJsonFixture -Path $configPath -Cells @(
                [ordered]@{ id = 'cell-01'; enabled = $true; serverDir = $server01; serverPort = 25570 },
                [ordered]@{ id = 'cell-02'; enabled = $true; serverDir = $server02; serverPort = 25575 },
                [ordered]@{ id = 'cell-03'; enabled = $true; serverDir = $server03; serverPort = 25585 }
            )

            $result = Get-TestCellBcBackends -SourceConfigPaths @($configPath) -IncludeCellIds @('cell-02', 'cell-03')

            $result.backends.Count | Should Be 2
            $result.backends[0].id | Should Be 'cell-02'
            $result.backends[1].id | Should Be 'cell-03'
            $result.defaultBackendId | Should Be 'cell-02'
        }
        finally {
            if (Test-Path -LiteralPath $root) {
                Remove-Item -LiteralPath $root -Recurse -Force
            }
        }
    }

    It 'updates the spigot bungeecord flag in existing yaml content' {
        $content = @(
            'settings:'
            '  debug: false'
            '  bungeecord: false'
            'world-settings:'
            '  default:'
            '    verbose: true'
        ) -join "`r`n"

        $updated = Set-TestCellBcSpigotBungeecordContent -Content $content -Enabled $true

        $updated | Should Match 'bungeecord: true'
    }

    It 'updates backend plugin config to point at the shared bot mod-http port' {
        $content = @(
            'debug: true'
            'http-port: 38080'
            'mod-http-address: "http://localhost:38081"'
        ) -join "`r`n"

        $updated = Set-TestCellBcPluginConfigContent -Content $content -PluginHttpPort 38090 -SharedBotModHttpPort 38091

        $updated | Should Match 'debug: false'
        $updated | Should Match 'http-port: 38090'
        $updated | Should Match 'test-mode: dual'
        $updated | Should Match 'mod-http-address: "http://localhost:38091"'
    }

    It 'round-trips BC backend prep state files' {
        $root = Join-Path $env:TEMP ("bc-fixture-" + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $root -Force | Out-Null

        try {
            $configPath = Join-Path $root 'cells.json'
            New-TestCellLeaseFixtureConfig -Path $configPath

            $state = [pscustomobject]@{
                owner = 'owner-01'
                botCellId = 'cell-02'
                backendCellIds = @('cell-02', 'cell-03')
                leasedCellIds = @('cell-02', 'cell-03')
                preparedAt = '2026-04-25T12:00:00'
                backends = @(
                    [pscustomobject]@{
                        id = 'cell-02'
                        spigotPath = 'a'
                        spigotContent = 'b'
                        pluginConfigPath = 'c'
                        pluginConfigContent = 'd'
                        pluginHttpPort = 38090
                        sharedBotModHttpPort = 38091
                    }
                )
            }

            $path = Write-TestCellBcPrepState -Owner 'owner-01' -State $state -CellConfigPath $configPath
            $loaded = Read-TestCellBcPrepState -Owner 'owner-01' -CellConfigPath $configPath

            (Test-Path -LiteralPath $path) | Should Be $true
            $loaded.owner | Should Be 'owner-01'
            $loaded.botCellId | Should Be 'cell-02'
            $loaded.backendCellIds[1] | Should Be 'cell-03'

            Remove-TestCellBcPrepState -Owner 'owner-01' -CellConfigPath $configPath
            (Test-Path -LiteralPath $path) | Should Be $false
        }
        finally {
            if (Test-Path -LiteralPath $root) {
                Remove-Item -LiteralPath $root -Recurse -Force
            }
        }
    }

    It 'does not leave partial leases behind when batch acquire fails' {
        $root = Join-Path $env:TEMP ("bc-fixture-" + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $root -Force | Out-Null

        try {
            $configPath = Join-Path $root 'cells.json'
            New-TestCellLeaseFixtureConfig -Path $configPath
            $config = Load-TestCellConfig -ConfigPath $configPath

            $null = Acquire-TestCellLease -Config $config -CellId 'cell-03' -Owner 'other-owner' -LeaseMinutes 30

            { Acquire-TestCellBcLeases -Config $config -CellIds @('cell-02', 'cell-03') -Owner 'test-owner' -LeaseMinutes 30 } | Should Throw

            $lease02 = Read-TestCellLease -CellId 'cell-02' -ConfigPath $config.path
            $lease03 = Read-TestCellLease -CellId 'cell-03' -ConfigPath $config.path
            $lease02 | Should Be $null
            $lease03.owner | Should Be 'other-owner'
        }
        finally {
            if (Test-Path -LiteralPath $root) {
                Remove-Item -LiteralPath $root -Recurse -Force
            }
        }
    }
}
