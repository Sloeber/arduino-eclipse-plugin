#include "Arduino.h"
#ifndef __IN_ECLIPSE__
#error "__IN_ECLIPSE__ is not defined in cpp file"
#endif
#ifndef TEST_C_CPP
#error "TEST_C_CPP is not defined in cpp file"
#endif
#ifndef TEST_CPP
#error "TEST_CPP is not defined in cpp file"
#endif
#ifdef TEST_C
#error "TEST_C is defined in cpp file"
#endif

//The setup function is called once at startup of the sketch
void setup()
{
// Add your initialization code here
}

// The loop function is called in an endless loop
void loop()
{
//Add your repeated code here
}
