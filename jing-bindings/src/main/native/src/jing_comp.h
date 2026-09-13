#ifndef JING_COMP_H
#define JING_COMP_H
#include "jing_common.h"

#if defined(JING_ENABLE_ZLIB_NG)
#include "zlib.h"

JING_EXPORT_SYMBOL const char* jing_zlib_ng_version(void);

#endif

#endif