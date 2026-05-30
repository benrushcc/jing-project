#include "jing_ssl.h"

#ifdef JING_USE_WEPOLL
#include <openssl/crypto.h>
#include <openssl/err.h>
#include <openssl/ssl.h>

const char* jing_ssl_version(void) {
	return OpenSSL_version(OPENSSL_VERSION);
}

const char* jing_ssl_cflags(void) {
	return OpenSSL_version(OPENSSL_CFLAGS);
}

const char* jing_ssl_built_on(void) {
	return OpenSSL_version(OPENSSL_BUILT_ON);
}

uint16_t jing_tls1_2_version(void) {
	return TLS1_2_VERSION;
}

uint16_t jing_tls1_3_version(void) {
	return TLS1_3_VERSION;
}

const SSL_METHOD* jing_tls_method(void) {
	return TLS_method();
}

const SSL_METHOD* jing_dtls_method(void) {
	return DTLS_method();
}

SSL_CTX* jing_ssl_ctx_new(const SSL_METHOD* method) {
	return SSL_CTX_new(method);
}

void jing_ssl_ctx_free(SSL_CTX* ctx) {
	SSL_CTX_free(ctx);
}

int jing_ssl_ctx_set_min_proto_version(SSL_CTX* ctx, uint16_t version) {
	return SSL_CTX_set_min_proto_version(ctx, version);
}

int jing_ssl_ctx_set_max_proto_version(SSL_CTX* ctx, uint16_t version) {
	return SSL_CTX_set_max_proto_version(ctx, version);
}

SSL* jing_ssl_new(SSL_CTX* ctx) {
	return SSL_new(ctx);
}
#endif