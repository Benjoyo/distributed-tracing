#!/usr/bin/env python

# Simon Duquennoy (simonduq@sics.se)

import sys
import os
import subprocess
sys.path.insert(1,'/usr/testbed/scripts')
from psshlib import *
import socket

REMOTE_LOGS_PATH = "/home/user/logs"
REMOTE_SCRIPTS_PATH = "/home/user/scripts"
REMOTE_JN_SCRIPTS_PATH = os.path.join(REMOTE_SCRIPTS_PATH, "nrf52")
REMOTE_TMP_PATH = "/home/user/tmp"
REMOTE_FIRMWARE_PATH = os.path.join(REMOTE_TMP_PATH, "firmware.nrf52.hex")

def get_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # doesn't even have to be reachable
        s.connect(('10.255.255.255', 1))
        IP = s.getsockname()[0]
    except Exception:
        IP = '127.0.0.1'
    finally:
        s.close()
    return IP

if __name__=="__main__":

  if len(sys.argv)<2:
    print "Job directory parameter not found!"
    sys.exit(1)
    
  # The only parameter contains the job directory
  job_dir = sys.argv[1]

  # Look for the firmware
  firmware_path = None
  if os.path.isdir(job_dir):
   for f in os.listdir(job_dir):
    if f.endswith(".nrf52.hex"):
      firmware_path = os.path.join(job_dir, f)
      break
       
  if firmware_path == None:
    print "No nrf52 firmware found!"
    sys.exit(2)

  hosts_path = os.path.join(job_dir, "hosts")

  # Copy firmware to the nodes
  if pscp(hosts_path, firmware_path, REMOTE_FIRMWARE_PATH, "Copying nrf52 firmware to the PI nodes") != 0:
    sys.exit(3)
  # Program the nodes
  if pssh(hosts_path, "%s %s"%(os.path.join(REMOTE_JN_SCRIPTS_PATH, "install.sh"), REMOTE_FIRMWARE_PATH), "Installing nrf52 firmware") != 0:
    sys.exit(4)


  # Look for the trace config file and trace server jar (only required for tracing)
  trace_server_path = None
  if os.path.isdir(job_dir):
    p = os.path.join(job_dir, "server.jar")
    if os.path.isfile(p):
      trace_server_path = p

  # Start tracing observers and server if tracing config exists
  if trace_server_path:
    # start server in background
    subprocess.Popen(["java", "-jar", trace_server_path, "-s", "mqtt", "-p", "resolve", "-o", "print"], cwd=job_dir, close_fds=True)
    # start observers
    if pssh(hosts_path, "%s %s %d"%(os.path.join(REMOTE_SCRIPTS_PATH, "trace.sh"), get_ip(), 20), "Start trace observers") != 0:
      sys.exit(6)


  # Start serialdump
  remote_log_dir = os.path.join(REMOTE_LOGS_PATH, os.path.basename(job_dir), "log.txt")
  if pssh(hosts_path, "%s %s"%(os.path.join(REMOTE_JN_SCRIPTS_PATH, "serialdump.sh"), remote_log_dir), "Starting serialdump") != 0:
    sys.exit(5)
