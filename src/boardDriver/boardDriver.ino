#include <Adafruit_NeoPixel.h>
#include <EEPROM.h>

// Set handshakes as needed.
const String HANDSHAKE_IN = "OF@ch+SOCYPEacmw4DYwAFb3E!AHZQfQ";
const String HANDSHAKE_OUT = "tQwAN6ENez1jGjkq&kKT3H3sm4vr31hF";

// Adjust LED pins as needed.
const Adafruit_NeoPixel LED0(1, 12, NEO_GRB);
const Adafruit_NeoPixel LED1(1, 13, NEO_GRB);

class EEPROMHandler {
  public:
    static void wipeEEPROM() {
      for (int i = 0; i < EEPROM.length(); i++) {
        EEPROM.write(i, 0);
      }
    }

    static void writeToEEPROM(byte writeBuffer[], int size) {
      for (int i = 0; i < size; i++) {
        EEPROM.put(i, writeBuffer[i]);
      }
    }
    static String getEEPROM() {
      String buffer = "";

    }
};

class LEDHandler {
  public:
    static Adafruit_NeoPixel* leds[2];

    static void setLED(int LEDid, int R, int G, int B) {
      leds[LEDid]->setPixelColor(0, Adafruit_NeoPixel::Color(R, G, B));
      leds[LEDid]->show();
    }

    static void initLEDs() {
      for (Adafruit_NeoPixel* led : leds) {
        led->begin();
        led->clear();
        led->setPixelColor(0, Adafruit_NeoPixel::Color(0, 0, 0));
        led->setBrightness(30);
        led->show();
      }
    }

    static void clearLEDs() {
      for (Adafruit_NeoPixel* led : leds) {
        led->clear();
        led->show();
      }
    }
};

Adafruit_NeoPixel* LEDHandler::leds[2] = {
  &LED0,
  &LED1
};

class commHandler {
  public:
    bool isConnected = false;

    commHandler() {
      LEDHandler::setLED(0, 0, 0, 0);
      LEDHandler::setLED(1, 255, 0, 0);

      waitForConnection();
    }

    void waitForConnection() {
      String incoming; 
      LEDHandler::setLED(1, 255, 255, 0);

      while (!isConnected) {
        Serial.println(HANDSHAKE_OUT);
        incoming = Serial.readString();
        
        if (incoming.equals(HANDSHAKE_IN)) {
          Serial.println("$CONNECTION_START");
          isConnected = true;
          LEDHandler::setLED(1, 0, 255, 0);
          break;
        }
      }
    }

    void listen() {
      String incoming;

      while (isConnected) {
        waitForInput();
        incoming = Serial.readString();
        incoming.trim();

        if (incoming.equals("ping")) {
          Serial.println("pong");
        }

        if (incoming.equals("$CONNECTION_END")) {
          isConnected = false;
          LEDHandler::setLED(1, 255, 0, 0);
          waitForConnection();
          break;
        }

        if (incoming.equals("$EEPROM_WIPE")) {
          LEDHandler::setLED(0, 255, 255, 0);
          EEPROMHandler::wipeEEPROM();
          LEDHandler::setLED(0, 0, 0, 0);
        }

        if (incoming.equals("$EEPROM_WRITE")) {
          waitForInput();
          int packetSize = Serial.parseInt();
          byte writeBuffer[packetSize];

          waitForInput();
          Serial.readBytes(writeBuffer, packetSize);
          EEPROMHandler::writeToEEPROM(writeBuffer, packetSize);
        }

        if (incoming.equals("$EEPROM_READ")) {
          Serial.println("$BYTES_START");
          Serial.flush();

          delay(100);
          for (int i = 0; i < EEPROM.length(); i++) {
            byte data = EEPROM.read(i);
            Serial.write(data);
          }
          Serial.println();
          delay(100);

          Serial.println("$BYTES_END");
          Serial.flush();
        }
      }
    }

    private:
      void waitForInput() {
        while (Serial.available() == 0) {}
      }
};

commHandler* _commHandler;

void setup() {
  Serial.begin(9600);
  LEDHandler::initLEDs();
  LEDHandler::clearLEDs();

  _commHandler = new commHandler();
}

void loop() {
  _commHandler->listen();
}