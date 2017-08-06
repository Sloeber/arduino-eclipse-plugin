#include "Arduino.h"
#ifndef LED_BUILTIN
#ifdef ARDUINO_ESP8266_WEMOS_D1MINI
#define LED_BUILTIN D4
#elif ARDUINO_ESP8266_NODEMCU
#define LED_BUILTIN 2
#endif
#else
#define LED_BUILTIN 13
#endif

#ifdef ARDUINO_AVR_LILYPAD_USB
#undef LED_BUILTIN
#define LED_BUILTIN 5
#endif

#ifndef INTERVAL
#define INTERVAL 100
#endif
const int ledPin =  LED_BUILTIN;      // the number of the LED pin

// Variables will change :
int ledState = LOW;             // ledState used to set the LED

// Generally, you should use "unsigned long" for variables that hold time
// The value will quickly become too large for an int to store
unsigned long previousMillis = 0;        // will store last time LED was updated

// constants won't change :
const long interval = INTERVAL;           // interval at which to blink (milliseconds)

void setup() {
  // set the digital pin as output:
  pinMode(ledPin, OUTPUT);
}

void loop() {

  unsigned long currentMillis = millis();

  if (currentMillis - previousMillis >= interval) {
    // save the last time you blinked the LED
    previousMillis = currentMillis;

    // if the LED is off turn it on and vice-versa:
    ledState = !ledState;

    // set the LED with the ledState of the variable:
    digitalWrite(ledPin, ledState);
  }
}

