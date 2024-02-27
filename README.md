# JDuino

![Static Badge](https://img.shields.io/badge/Version-1.0-8a35da)
![Static Badge](https://img.shields.io/badge/Languages-Java-orange)
![Static Badge](https://img.shields.io/badge/C++-cornflowerblue)
![Static Badge](https://img.shields.io/badge/JDK-21-yellow)

Java based open-source development board interface.

## Features
- Handshake based connection establishing
- Multithreaded event handling
- Console interface
- 2 control LEDs (adjust pins in driver code)
- EEPROM operations handling (tested on Arduino Uno)

## Foreseen Features
- Graphical UI
- Logging

## Contents
1. Java based board handler class and interface (./src/main/)
2. Board driver for Arduino (./src/boardDriver/)

## Defaults
    Handshake in:   tQwAN6ENez1jGjkq&kKT3H3sm4vr31hF
    Handshake out:  OF@ch+SOCYPEacmw4DYwAFb3E!AHZQfQ
Adjust in boardDriver.ino

## Requirements
- JDK version: 21

### Credits
 Fazecast - jSerialComm library: https://github.com/Fazecast/jSerialComm