#ifndef JING_COMP_H
#define JING_COMP_H
#include "jing_common.h"

#if defined(JING_ENABLE_ZLIB_NG)
#include "zlib.h"

const char* jing_zlib_ng_version(void);

#endif

#endif