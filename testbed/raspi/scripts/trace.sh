#!/bin/bash

sudo service ntp stop
sudo ntpdate -s time1.informatik.uni-kiel.de
sudo service ntp start

sleep $2

export PATH=$PATH:/home/user/scripts/nrf52/JLink_Linux_arm

cd tracing

java -jar client.jar -n $HOSTNAME -s $1 -d NRF52840_XXAA -m &
