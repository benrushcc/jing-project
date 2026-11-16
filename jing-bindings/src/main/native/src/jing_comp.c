#include "jing_comp.h"
#if defined(JING_ENABLE_ZLIB_NG) && JING_ENABLE_ZLIB_NG
#include "zlib.h"

const char* jing_zlib_ng_version(void) {
	return zlibVersion();
}
#endif