#include "jing_common.h"

static const int MAJOR_VERSION = 0;
static const int MINOR_VERSION = 0;
static const int PATCH_VERSION = 1;

int jing_major_version(void) {
	return MAJOR_VERSION;
}

int jing_minor_version(void) {
	return MINOR_VERSION;
}

int jing_patch_version(void) {
	return PATCH_VERSION;
}

uintptr_t jing_ptr_err_flag(void) {
	return JING_PTR_ERR_FLAG;
}