#ifndef JING_SSL_H
#define JING_SSL_H

#include "jing_common.h"
#ifdef JING_USE_SSL
#include <openssl/crypto.h>
#include <openssl/err.h>
#include <openssl/ssl.h>

JING_EXPORT_SYMBOL const char* jing_ssl_version(void);

JING_EXPORT_SYMBOL const char* jing_ssl_cflags(void);

JING_EXPORT_SYMBOL const char* jing_ssl_built_on(void);

JING_EXPORT_SYMBOL uint16_t jing_tls1_2_version(void);

JING_EXPORT_SYMBOL uint16_t jing_tls1_3_version(void);

JING_EXPORT_SYMBOL const SSL_METHOD* jing_tls_method(void);

JING_EXPORT_SYMBOL SSL_CTX* jing_ssl_ctx_new(const SSL_METHOD* method);

JING_EXPORT_SYMBOL void jing_ssl_ctx_free(SSL_CTX* ctx);

JING_EXPORT_SYMBOL int jing_ssl_ctx_set_min_proto_version(SSL_CTX* ctx,
                                                          uint16_t version);

JING_EXPORT_SYMBOL int jing_ssl_ctx_set_max_proto_version(SSL_CTX* ctx,
                                                          uint16_t version);

JING_EXPORT_SYMBOL SSL* jing_ssl_new(SSL_CTX* ctx);

// bio related
JING_EXPORT_SYMBOL BIO_METHOD* jing_ssl_bio_meth_new(int type,
                                                     const char* name);

JING_EXPORT_SYMBOL int
jing_ssl_bio_meth_set_write(BIO_METHOD* biom,
                            int (*write)(BIO*, const char*, int));

JING_EXPORT_SYMBOL int
jing_ssl_bio_meth_set_read(BIO_METHOD* biom, int (*read)(BIO*, char*, int));

JING_EXPORT_SYMBOL int
jing_ssl_bio_meth_set_puts(BIO_METHOD* biom, int (*puts)(BIO*, const char*));

JING_EXPORT_SYMBOL int jing_ssl_bio_meth_set_ctrl(BIO_METHOD* biom,
                                                  long (*ctrl)(BIO*, int, long,
                                                               void*));

JING_EXPORT_SYMBOL int jing_ssl_bio_meth_set_create(BIO_METHOD* biom,
                                                    int (*create)(BIO*));

JING_EXPORT_SYMBOL int jing_ssl_bio_meth_set_destroy(BIO_METHOD* biom,
                                                     int (*destroy)(BIO*));

#endif

#endif