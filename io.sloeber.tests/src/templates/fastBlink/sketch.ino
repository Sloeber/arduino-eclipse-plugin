#include "Arduino.h"

#ifdef ARDUINO_ESP8266_WEMOS_D1MINI
#undef LED_BUILTIN
#define LED_BUILTIN D4
#elif ARDUINO_ESP8266_NODEMCU
#undef LED_BUILTIN
#define LED_BUILTIN 2
#elif  ARDUINO_AVR_LILYPAD_USB
#undef LED_BUILTIN
#define LED_BUILTIN 5
#elif TEENSYDUINO
#define LED_BUILTIN 6
#else
#undef LED_BUILTIN
#define LED_BUILTIN 13
#endif

//the define below makes sure we can use other serial ports than Serial
//like serialUSB for rocketScream
//and still can use the open srial monitor toolbutton
//finding the correct serial baud rate in the code
#define Serial {SerialMonitorSerial}

#ifndef INTERVAL
#define INTERVAL 100
#endif
const int ledPin =  LED_BUILTIN;      // the number of the LED pin

const long interval = INTERVAL;           // interval at which to blink (milliseconds)
#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)

void setup() {
	pinMode(ledPin, OUTPUT);
	Serial.begin(115200);
	Serial.println(STR(SERIAlDUMP));
}

void loop() {
	static unsigned long previousLedMillis = 0; // will store last time LED was updated
	static unsigned long previousLogMillis = 0;
	static int ledState = LOW;

  unsigned long currentMillis = millis();

	if (currentMillis - previousLedMillis >= interval) {
		previousLedMillis = currentMillis;
    ledState = !ledState;
		digitalWrite(ledPin, ledState);

  }
	if (currentMillis - previousLogMillis >= 100) {
		previousLogMillis = currentMillis;
		Serial.println(STR(SERIAlDUMP));
	}
}

