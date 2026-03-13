cd "C:\Program Files\ticket_master-master"
$env:LB_UI_BASE_URL="http://localhost:1915/tm"
$env:TM_GRPC_HOST="localhost"
$env:TM_GRPC_TARGET_PORT="9201"
& "C:\Tools\apache-maven-3.9.12\bin\mvn.cmd" --% -q -f multi/desktop-client/pom.xml exec:java -Dexec.mainClass=edu.upb.desktop.MainUI
