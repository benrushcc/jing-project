#include "jing_posix_net.h"

#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
int jing_posix_bind(int sockfd, const struct sockaddr* addr,
                    socklen_t addrlen) {
	int v = bind(sockfd, addr, addrlen);
	if (v == -1) {
		int err = errno;
		return err;
	}
	return 0;
}

#endif