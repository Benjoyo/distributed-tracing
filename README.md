# distributed-tracing
Non- and minimally-intrusive distributed tracing system for the IoT.

## tracingbackend
The trace server.
Started e.g. as `java -jar server.jar -s mqtt -p resolve -o print`

## tracingclinet
The tracing client (observer).
Started e.g. as `java -jar client.jar -n $HOSTNAME -s server_ip -d NRF52840_XXAA`

## orbuculum-main
orbuculum-main is a fork of orbuculum that contains an additional custom program Src/trace.c, that is built alongside the other orbuculum tools in the top level Makefile. The custom program is responsible for outputting ITM events in a format that the Java program (tracingclient) understands. It is independent of the actual source for orbuculum (SWO/FPGA/...) and needs an orbuculum instance running.

