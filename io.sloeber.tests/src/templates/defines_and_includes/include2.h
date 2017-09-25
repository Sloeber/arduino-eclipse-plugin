#ifndef __INCLUDE_2_H__
#define __INCLUDE_2_H__
#ifndef DEFINE_1
#error "DEFINE_1 should be defined"
#endif
#ifndef DEFINE_2
#error "DEFINE_2 should be defined"
#endif
#ifdef DEFINE_3
#error "DEFINE_3 should not be defined"
#endif
#endif
