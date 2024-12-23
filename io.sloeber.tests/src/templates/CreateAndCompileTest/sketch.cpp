#include "Arduino.h"

#ifdef ARDUINO_BOARD
char mychar[] = ARDUINO_BOARD;
#endif
#ifdef USB_MANUFACTURER
char mychar2[] = USB_MANUFACTURER;
#endif
#ifdef USB_PRODUCT
char mychar3[] = USB_PRODUCT;
#endif
#ifdef ARDUINO_HOST_OS
char mychar4[] = ARDUINO_HOST_OS;
#endif
#ifdef ARDUINO_VARIANT
char mychar5[] = ARDUINO_VARIANT;
#endif




//The setup function is with spaces between the curly braces
void setup(){ }

// The loop function is without content between curly braces
void loop(){}

#ifdef __SIMBA_H__
int main() {
	return 0;
}
#endif
