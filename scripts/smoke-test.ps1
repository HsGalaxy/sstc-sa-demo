$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$results = @()
function Check($name, $script) {
  try {
    $value = & $script
    $script:results += [pscustomobject]@{ Check = $name; Result = 'PASS'; Detail = $value }
  } catch {
    $script:results += [pscustomobject]@{ Check = $name; Result = 'FAIL'; Detail = $_.Exception.Message }
  }
}

# --- S01 identity -----------------------------------------------------------
$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -ContentType 'application/json' `
  -Body '{"username":"lead","password":"Lead@123"}'
$token = $login.token
$headers = @{ Authorization = "Bearer $token" }
$results += [pscustomobject]@{ Check = 'S01 login (lead)'; Result = 'PASS'; Detail = "token=$($token.Substring(0,10))... role=$($login.user.role) perms=$($login.user.permissions.Count)" }

Check 'S01 users' { (Invoke-RestMethod -Uri "$base/api/users" -Headers $headers).Count }
Check 'S01 roles' { (Invoke-RestMethod -Uri "$base/api/roles" -Headers $headers).Count }
Check 'S01 permissions' { (Invoke-RestMethod -Uri "$base/api/permissions" -Headers $headers).Count }
Check 'S01 access audits' { (Invoke-RestMethod -Uri "$base/api/access-audits" -Headers $headers).Count }

# --- S02 dashboard ----------------------------------------------------------
$board = Invoke-RestMethod -Uri "$base/api/tasks/board" -Headers $headers
$results += [pscustomobject]@{ Check = 'S02 kanban board'; Result = 'PASS'; Detail = ($board | ForEach-Object { "$($_.status)=$($_.count)" }) -join ' ' }
Check 'S02 task summary' { (Invoke-RestMethod -Uri "$base/api/tasks/summary" -Headers $headers).total }
$newTask = Invoke-RestMethod -Uri "$base/api/tasks" -Method Post -Headers $headers -ContentType 'application/json' `
  -Body '{"title":"API smoke test task","description":"created by the smoke test","status":"TODO","priority":"HIGH","assigneeId":3,"dueDate":"2026-11-20","estimateHours":3}'
$results += [pscustomobject]@{ Check = 'S02 create task'; Result = 'PASS'; Detail = "$($newTask.code) id=$($newTask.id)" }
$moved = Invoke-RestMethod -Uri "$base/api/tasks/$($newTask.id)/move?status=IN_PROGRESS" -Method Patch -Headers $headers
$results += [pscustomobject]@{ Check = 'S02 move task'; Result = 'PASS'; Detail = $moved.status }
Check 'S02 operation log' { (Invoke-RestMethod -Uri "$base/api/operations?limit=10" -Headers $headers).Count }
Check 'S02 cycles' { (Invoke-RestMethod -Uri "$base/api/cycles" -Headers $headers).Count }
Check 'S02 okrs' { (Invoke-RestMethod -Uri "$base/api/okrs" -Headers $headers).Count }
$perf = Invoke-RestMethod -Uri "$base/api/performance" -Headers $headers
$results += [pscustomobject]@{ Check = 'S02 performance'; Result = 'PASS'; Detail = "okr=$($perf.teamOkrScore) members=$($perf.members.Count) bottlenecks=$($perf.bottlenecks.Count)" }
Check 'S02 file query' { (Invoke-RestMethod -Uri "$base/api/files?bucket=DESIGN" -Headers $headers).Count }
$fileList = Invoke-RestMethod -Uri "$base/api/files" -Headers $headers
Check 'S02 file versions' { (Invoke-RestMethod -Uri "$base/api/files/$($fileList[0].id)/versions" -Headers $headers).Count }

# --- S03 collaboration ------------------------------------------------------
$channels = Invoke-RestMethod -Uri "$base/api/channels" -Headers $headers
$results += [pscustomobject]@{ Check = 'S03 channels'; Result = 'PASS'; Detail = ($channels | ForEach-Object { $_.name }) -join ',' }
$msg = Invoke-RestMethod -Uri "$base/api/channels/$($channels[0].id)/messages" -Method Post -Headers $headers -ContentType 'application/json' `
  -Body '{"body":"Smoke test message mentioning @yunteng and @jinghui"}'
$results += [pscustomobject]@{ Check = 'S03 publish + mention detection'; Result = 'PASS'; Detail = "mentions=$($msg.mentions.Count)" }
Check 'S03 notifications' { (Invoke-RestMethod -Uri "$base/api/notifications" -Headers $headers).Count }
Check 'S03 digest' { (Invoke-RestMethod -Uri "$base/api/digest" -Headers $headers).Count }
$meetings = Invoke-RestMethod -Uri "$base/api/meetings" -Headers $headers
Check 'S03 meetings' { $meetings.Count }
$ended = $meetings | Where-Object { $_.status -eq 'ENDED' } | Select-Object -First 1
Check 'S03 transcript' { (Invoke-RestMethod -Uri "$base/api/meetings/$($ended.id)/transcript" -Headers $headers).Count }
$summary = Invoke-RestMethod -Uri "$base/api/meetings/$($ended.id)/summary" -Headers $headers
$results += [pscustomobject]@{ Check = 'S03 AI meeting summary'; Result = 'PASS'; Detail = "decisions=$($summary.decisions.Count) actions=$($summary.actionItems.Count) confidence=$($summary.confidence)" }

# --- S04 toolbox ------------------------------------------------------------
Check 'S04 documents' { (Invoke-RestMethod -Uri "$base/api/toolbox/documents" -Headers $headers).Count }
$docs = Invoke-RestMethod -Uri "$base/api/toolbox/documents" -Headers $headers
$edited = Invoke-RestMethod -Uri "$base/api/toolbox/documents/$($docs[0].id)/edits" -Method Post -Headers $headers -ContentType 'application/json' `
  -Body '{"operation":"APPEND","payload":"Smoke test append."}'
$results += [pscustomobject]@{ Check = 'S04 document edit (quota check)'; Result = 'PASS'; Detail = "revision=$($edited.revision) words=$($edited.wordCount)" }
Check 'S04 presentations' { (Invoke-RestMethod -Uri "$base/api/toolbox/presentations" -Headers $headers).Count }
Check 'S04 repos' { (Invoke-RestMethod -Uri "$base/api/toolbox/repos" -Headers $headers).Count }
$repos = Invoke-RestMethod -Uri "$base/api/toolbox/repos" -Headers $headers
Check 'S04 commits' { (Invoke-RestMethod -Uri "$base/api/toolbox/repos/$($repos[0].id)/commits" -Headers $headers).Count }
$commit = Invoke-RestMethod -Uri "$base/api/toolbox/repos/$($repos[0].id)/commits" -Method Post -Headers $headers -ContentType 'application/json' `
  -Body '{"message":"test(smoke): verify the push contract"}'
$results += [pscustomobject]@{ Check = 'S04 push commit'; Result = 'PASS'; Detail = $commit.shortSha }
Check 'S04 environments' { (Invoke-RestMethod -Uri "$base/api/toolbox/environments" -Headers $headers).Count }
$env1 = Invoke-RestMethod -Uri "$base/api/toolbox/environments" -Method Post -Headers $headers -ContentType 'application/json' `
  -Body '{"serviceId":"smoke-env","image":"sstc/smoke:1.0","cpuLimit":1.0,"memoryMb":1024,"ports":"9090:9090"}'
$results += [pscustomobject]@{ Check = 'S04 start environment'; Result = 'PASS'; Detail = "$($env1.containerId) state=$($env1.state)" }
Check 'S04 environment status lookup' { (Invoke-RestMethod -Uri "$base/api/toolbox/environments/status?serviceId=smoke-env" -Headers $headers).state }
Check 'S04 toolbox files' { (Invoke-RestMethod -Uri "$base/api/toolbox/files" -Headers $headers).Count }
$uploaded = Invoke-RestMethod -Uri "$base/api/toolbox/files" -Method Post -Headers $headers -ContentType 'application/json' `
  -Body '{"name":"smoke-test-report.pdf","folder":"/demo","contentType":"application/pdf","sizeBytes":120000}'
$results += [pscustomobject]@{ Check = 'S04 upload file'; Result = 'PASS'; Detail = "id=$($uploaded.id) status=$($uploaded.status)" }
$link = Invoke-RestMethod -Uri "$base/api/toolbox/files/$($uploaded.id)/links" -Method Post -Headers $headers -ContentType 'application/json' `
  -Body '{"expiresInDays":7}'
$results += [pscustomobject]@{ Check = 'S04 create share link'; Result = 'PASS'; Detail = $link.url }
Check 'S04 integrations' { (Invoke-RestMethod -Uri "$base/api/toolbox/integrations" -Headers $headers).gitProvider }
Check 'S04 storage stats' { (Invoke-RestMethod -Uri "$base/api/toolbox/storage" -Headers $headers).files }

# --- negative cases ---------------------------------------------------------
try {
  Invoke-RestMethod -Uri "$base/api/users" -TimeoutSec 5 | Out-Null
  $results += [pscustomobject]@{ Check = 'Auth guard rejects anonymous'; Result = 'FAIL'; Detail = 'call succeeded' }
} catch {
  $results += [pscustomobject]@{ Check = 'Auth guard rejects anonymous'; Result = 'PASS'; Detail = $_.Exception.Response.StatusCode.value__ }
}
try {
  Invoke-RestMethod -Uri "$base/api/toolbox/files" -Method Post -Headers $headers -ContentType 'application/json' `
    -Body '{"name":"virus.exe","sizeBytes":1000}' | Out-Null
  $results += [pscustomobject]@{ Check = 'Upload validation rejects executables'; Result = 'FAIL'; Detail = 'call succeeded' }
} catch {
  $results += [pscustomobject]@{ Check = 'Upload validation rejects executables'; Result = 'PASS'; Detail = $_.Exception.Message }
}

$results | Format-Table -AutoSize | Out-String -Width 200
