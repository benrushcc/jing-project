#include "common.h"

static const int MAJOR_VERSION = 0;
static const int MINOR_VERSION = 0;
static const int PATCH_VERSION = 1;
static const char VERSION_STRING[] = "0.0.1";

const char *jing_version(void) { return VERSION_STRING; }

int jing_major_version(void) { return MAJOR_VERSION; }

int jing_minor_version(void) { return MINOR_VERSION; }

int jing_patch_version(void) { return PATCH_VERSION; }