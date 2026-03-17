cd "C:\Program Files\ticket_master-master"
$env:TM_SERVICE_PORT="9302"
$env:TM_GRPC_PORT="9402"
$env:TM_SERVICE_HOST="localhost"
$env:LB_REGISTER_URL="http://localhost:1915/register"
$env:TM_DB_URL="jdbc:mysql://localhost:3306/sis_distribuidos"
$env:TM_DB_USER="root"
$env:TM_DB_PASS="12345689"
& "C:\Tools\apache-maven-3.9.12\bin\mvn.cmd" --% -q -f multi/tm-service/pom.xml exec:java -Dexec.mainClass=edu.upb.tmservice.TmServiceApp
