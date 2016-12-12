#include "Arduino.h"
#ifndef __IN_ECLIPSE__
#error "__IN_ECLIPSE__ is not defined in c file"
#endif
#ifndef TEST_C_CPP
#error "TEST_C_CPP is not defined in c file"
#endif
#ifndef TEST_C
#error "TEST_C is not defined in c file"
#endif
#ifdef TEST_CPP
#error "TEST_CPP is defined in C file"
#endif
