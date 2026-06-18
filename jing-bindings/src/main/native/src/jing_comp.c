#include "jing_comp.h"
#include "zlib.h"

const char* jing_zlib_ng_version(void) {
    return zlibVersion();
}