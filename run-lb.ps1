cd "C:\Program Files\ticket_master-master"
$env:LB_PORT="1915"
& "C:\Tools\apache-maven-3.9.12\bin\mvn.cmd" --% -q -f multi/load-balancer/pom.xml exec:java -Dexec.mainClass=edu.upb.lb.LoadBalancerApp
