
#include "Arduino.h"

#include "include0.h"
#define DEFINE_1
#include "include1.h"
#define DEFINE_2 something
#include "include2.h"
#define DEFINE_3
int t=3;
#include "include3.h"
#ifdef NOT_DEFINED
//test #736
#include "This should not be in the .ino.cpp file.h"
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
