# distributed-tracing
Non- and minimally-intrusive distributed tracing system for the IoT.

## tracingbackend
The trace server.
Started e.g. as `java -jar server.jar -s mqtt -p resolve -o print`

## tracingclient
The tracing client (observer).
Started e.g. as `java -jar client.jar -n $HOSTNAME -s server_ip -d NRF52840_XXAA`

### FPGA
Using the option `-fpga`, the observer will use an FPGA connected via USB to /dev/ttyUSB1 for receiving trace data. However, during the thesis I only used the FPGA manually and stand-alone for evaluation. To make it fully work with the complete tracing system, the swo.gdbinit.template file has to be modified to enable parallel tracing and not use SWO (enabling SWO will overwrite any parallel tracing config). The functions enableNRF52TRACE and enableNRF53TRACE in https://github.com/orbcode/orbuculum/blob/Devel/Support/gdbtrace.init contain the necessary set-up that needs to be run instead of the current SWO set-up.

On the hardware level, it is important to have good grounding for the FPGA. Connect multiple jumper wires from GND on the FPGA to GND on the board. Or use a real tracing connector.

## orbuculum-main
orbuculum-main is a fork of orbuculum that contains an additional custom program Src/trace.c, that is built alongside the other orbuculum tools in the top level Makefile. The custom program is responsible for outputting ITM events in a format that the Java program (tracingclient) understands. It is independent of the actual source for orbuculum (SWO/FPGA/...) and needs an orbuculum instance running.

