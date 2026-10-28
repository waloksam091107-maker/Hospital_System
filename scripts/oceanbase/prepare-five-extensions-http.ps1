[CmdletBinding()]
param([string]$SessionFile = '.hospital-local/five-extensions-test-session.json')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Push-Location $projectRoot
try {
$s=Get-Content -LiteralPath $SessionFile -Raw | ConvertFrom-Json
if($s.project -notmatch '^hospital_test_[a-f0-9]{32}$' -or $s.database -notmatch '^hospital_test_[a-f0-9]{32}$'){throw 'Isolated schema required'}
if($s.user -cne 'hospital_test_runner@test' -or [int]$s.port -in @(2881,3306,8080) -or [int]$s.port -lt 1024){throw 'Restricted isolated session required'}
$container=$s.project+'-oceanbase-1'
$labels=docker inspect --format '{{json .Config.Labels}}' $container | ConvertFrom-Json
if($LASTEXITCODE -ne 0 -or $labels.'com.docker.compose.project' -cne $s.project){throw 'Isolated container ownership mismatch'}
$labels=docker network inspect --format '{{json .Labels}}' ($s.project+'_default') | ConvertFrom-Json
if($LASTEXITCODE -ne 0 -or $labels.'com.docker.compose.project' -cne $s.project){throw 'Isolated network ownership mismatch'}
$listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,18081)
try {$listener.Start()} finally {$listener.Stop()}
$null=docker image inspect hospital-system:five-extensions-local
if($LASTEXITCODE -ne 0){throw 'Build local test image first: docker build -t hospital-system:five-extensions-local .'}
$OutputEncoding=New-Object Text.UTF8Encoding($false)
$prefix='http_'+[guid]::NewGuid().ToString('N').Substring(0,10)
$password=[guid]::NewGuid().ToString('N')
function Sql([string]$query){
 $result=$query | docker exec -i --env ('HOSPITAL_TEST_TARGET='+$s.database) $container sh -lc 'MYSQL_PWD="$OB_TENANT_PASSWORD" obclient -h127.0.0.1 -P2881 -uroot@test --database="$HOSPITAL_TEST_TARGET" --batch --skip-column-names'
 if($LASTEXITCODE -ne 0){throw 'Isolated SQL fixture failed'}
 return ($result | Out-String).Trim()
}
$dep=Sql "INSERT INTO departments(department_name) VALUES('$prefix'); SELECT LAST_INSERT_ID();"
$title=Sql "INSERT INTO professional_titles(title_name) VALUES('$prefix'); SELECT LAST_INSERT_ID();"
foreach($v in @($dep,$title)){if($v -notmatch '^\d+$'){throw 'Invalid fixture key'}}
$doctor=Sql "INSERT INTO doctors(job_number,name,password,registration_fee,department_id,professional_title_id) VALUES('$prefix','HTTP doctor','$password',10,$dep,$title); SELECT LAST_INSERT_ID();"
$admin=Sql "INSERT INTO admins(username,password) VALUES('$prefix','$password'); SELECT LAST_INSERT_ID();"
$patient=Sql "INSERT INTO patients(name,password,balance) VALUES('$prefix','$password',100); SELECT LAST_INSERT_ID();"
$other=Sql "INSERT INTO patients(name,password,balance) VALUES('$prefix-other','$password',100); SELECT LAST_INSERT_ID();"
$third=Sql "INSERT INTO patients(name,password,balance) VALUES('$prefix-third','$password',100); SELECT LAST_INSERT_ID();"
foreach($v in @($doctor,$admin,$patient,$other,$third)){if($v -notmatch '^\d+$'){throw 'Invalid fixture key'}}
$today=[TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTime]::UtcNow,'China Standard Time').ToString('yyyy-MM-dd')
$tomorrow=[DateTime]::ParseExact($today,'yyyy-MM-dd',$null).AddDays(1).ToString('yyyy-MM-dd')
$slot=Sql "INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status,capacity) VALUES($doctor,'$today','EVENING','AVAILABLE',2); SELECT LAST_INSERT_ID();"
$futureSlot=Sql "INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status,capacity) VALUES($doctor,'$tomorrow','EVENING','AVAILABLE',2); SELECT LAST_INSERT_ID();"
$pastDoctor=Sql "INSERT INTO doctors(job_number,name,password,registration_fee,department_id,professional_title_id) VALUES('$prefix-past','HTTP past doctor','$password',0,$dep,$title); SELECT LAST_INSERT_ID();"
$yesterday=[DateTime]::ParseExact($today,'yyyy-MM-dd',$null).AddDays(-1).ToString('yyyy-MM-dd')
$pastSlot=Sql "INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status,capacity,occupied_count,next_sequence) VALUES($pastDoctor,'$yesterday','AM','AVAILABLE',2,2,2); SELECT LAST_INSERT_ID();"
$noShowAppointment=Sql "INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,schedule_slot_id,appointment_sequence,status,queue_status) VALUES($patient,$pastDoctor,'$yesterday','AM',$pastSlot,1,'BOOKED','NOT_CHECKED_IN'); SELECT LAST_INSERT_ID();"
$waitingAppointment=Sql "INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,schedule_slot_id,appointment_sequence,status,queue_status,checked_in_at) VALUES($other,$pastDoctor,'$yesterday','AM',$pastSlot,2,'BOOKED','WAITING','$yesterday 08:00:00'); SELECT LAST_INSERT_ID();"
$null=Sql "INSERT INTO queue_history(appointment_id,from_status,to_status,actor_role,actor_id) VALUES($waitingAppointment,'NOT_CHECKED_IN','WAITING','PATIENT',$other);"
$config=[pscustomobject]@{pastDoctor=[int]$pastDoctor;pastSlot=[int]$pastSlot;noShowAppointment=[int]$noShowAppointment;waitingAppointment=[int]$waitingAppointment;yesterday=$yesterday;prefix=$prefix;password=$password;doctor=[int]$doctor;admin=[int]$admin;patient=[int]$patient;other=[int]$other;third=[int]$third;today=$today;tomorrow=$tomorrow;slot=[int]$slot;futureSlot=[int]$futureSlot}
[IO.File]::WriteAllText((Join-Path (Get-Location) '.hospital-local/five-extensions-http-fixture.json'),($config|ConvertTo-Json),(New-Object Text.UTF8Encoding($false)))
$envFile=Join-Path (Get-Location) '.hospital-local/five-extensions-runtime.env'
[IO.File]::WriteAllLines($envFile,@('HOSPITAL_DB_DRIVER=com.oceanbase.jdbc.Driver',('HOSPITAL_DB_URL=jdbc:oceanbase://'+$container+':2881/'+$s.database+'?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'),('HOSPITAL_DB_USERNAME='+$s.user),('HOSPITAL_DB_PASSWORD='+$s.password),('OCEANBASE_HOST='+$container),'OCEANBASE_PORT=2881','HOSPITAL_UPLOAD_DIR=/var/lib/hospital/uploads'),(New-Object Text.UTF8Encoding($false)))
$name=$s.project+'-clinic-'+[guid]::NewGuid().ToString('N').Substring(0,8)
docker run -d --name $name --label hospital.five-extensions.test=true --label ('com.docker.compose.project='+$s.project) --network ($s.project+'_default') --env-file $envFile -p 127.0.0.1:18081:8080 hospital-system:five-extensions-local | Out-Null
if($LASTEXITCODE -ne 0){throw 'Isolated app failed to start'}
[IO.File]::WriteAllText((Join-Path (Get-Location) '.hospital-local/five-extensions-runtime.json'),(@{name=$name;project=$s.project;port=18081}|ConvertTo-Json),(New-Object Text.UTF8Encoding($false)))
Write-Output 'Isolated clinic HTTP fixture and application prepared on 18081.'

} finally { Pop-Location }
