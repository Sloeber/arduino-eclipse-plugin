#include "Arduino.h"
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

//simba compiles fine but does not provide a main
#ifdef __SIMBA_H__
int main( void )
{
	return 0;
}
#endif
