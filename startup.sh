#!/bin/sh
x-terminal-emulator -e java -jar coordinator.jar 9395
x-terminal-emulator -e java -jar replica.jar localhost 9395 replica1
x-terminal-emulator -e java -jar replica.jar localhost 9395 replica2
x-terminal-emulator -e java -jar client.jar localhost 9395
