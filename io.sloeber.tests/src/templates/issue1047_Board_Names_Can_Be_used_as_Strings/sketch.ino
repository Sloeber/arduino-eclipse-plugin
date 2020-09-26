#include "Arduino.h"

char mychar[] = ARDUINO_BOARD;


#ifdef USB_MANUFACTURER
char mychar2[] = USB_MANUFACTURER;
#endif
#ifdef USB_PRODUCT
char mychar3[] = USB_PRODUCT;
#endif




//The setup function is with spaces between the curly braces
void setup(){ }

// The loop function is without content between curly braces
void loop(){}

