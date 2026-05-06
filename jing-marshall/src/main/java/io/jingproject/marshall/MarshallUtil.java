package io.jingproject.marshall;

import io.jingproject.common.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class MarshallUtil {

    // byte constants
    private static final byte BYTE_ZERO = (byte) '0';
    private static final byte BYTE_NINE = (byte) '9';
    private static final byte BYTE_MINUS = (byte) '-';
    private static final byte BYTE_PLUS = (byte) '+';
    private static final byte BYTE_PERIOD = (byte) '.';
    private static final byte BYTE_e = (byte) 'e';
    private static final byte BYTE_E = (byte) 'E';

    // integer to string constants
    private static final byte[] MIN_INT_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MIN_LONG_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ITOA_BYTES =
            ("0001020304050607080910111213141516171819"
                    + "2021222324252627282930313233343536373839"
                    + "4041424344454647484950515253545556575859"
                    + "6061626364656667686970717273747576777879"
                    + "8081828384858687888990919293949596979899").getBytes(StandardCharsets.US_ASCII);
    private static final MemorySegment ITOA_SEG = MemorySegment.ofArray(ITOA_BYTES).asReadOnly();
    private static final int[] INT_LEN_TABLE = new int[32];
    private static final int[] INT_POW_TABLE = new int[11];
    private static final int[] LONG_LEN_TABLE = new int[64];
    private static final long[] LONG_POW_TABLE = new long[20];

    // float to string constants
    private static final int MAX_FLOAT_CAPACITY = 16;
    private static final int MAX_DOUBLE_CAPACITY = 25;
    private static final int MIN_DOUBLE_E = -324;
    private static final int MAX_DOUBLE_E = 308;
    private static final int MIN_FLOAT_E = -45;
    private static final int MAX_FLOAT_E = 38;
    private static final short NEG_ZERO = Utils.compact(BYTE_MINUS, BYTE_ZERO);
    private static final short ZERO_PERIOD = Utils.compact(BYTE_ZERO, BYTE_PERIOD);
    private static final long MAX_UINT_64 = 0xFFFFFFFFFFFFFFFFL;
    private static final long DIV_1_E_8_M = 0xc767074b22e90e21L;  // inverse of 5^8
    private static final long DIV_1_E_4_M = 0xd288ce703afb7e91L;  // inverse of 5^4
    private static final long DIV_1_E_2_M = 0x8f5c28f5c28f5c29L;  // inverse of 5^2
    private static final long DIV_1_E_1_M = 0xcccccccccccccccdL;  // inverse of 5
    private static final long DIV_1_E_8_LE = Long.divideUnsigned(MAX_UINT_64, 100_000_000L);
    private static final long DIV_1_E_4_LE = Long.divideUnsigned(MAX_UINT_64, 10_000L);
    private static final long DIV_1_E_2_LE = Long.divideUnsigned(MAX_UINT_64, 100L);
    private static final long DIV_1_E_1_LE = Long.divideUnsigned(MAX_UINT_64, 10L);
    private static final int POW10MIN = -348;
    private static final int POW10MAX = 347;
    //region POW10TAB
    private static final long[] POW10TAB = {
            0xfa8fd5a0081c0289L, 0xe8cd3796329f1bacL, // 1e-348 * 2**1284
            0x9c99e58405118196L, 0xf18042bddfa3714bL, // 1e-347 * 2**1280
            0xc3c05ee50655e1fbL, 0xade0536d578c4d9eL, // 1e-346 * 2**1277
            0xf4b0769e47eb5a79L, 0x19586848ad6f6106L, // 1e-345 * 2**1274
            0x98ee4a22ecf3188cL, 0x6fd7412d6c659ca3L, // 1e-344 * 2**1270
            0xbf29dcaba82fdeafL, 0x8bcd1178c77f03ccL, // 1e-343 * 2**1267
            0xeef453d6923bd65bL, 0xeec055d6f95ec4c0L, // 1e-342 * 2**1264
            0x9558b4661b6565f9L, 0xb53835a65bdb3af8L, // 1e-341 * 2**1260
            0xbaaee17fa23ebf77L, 0xa286430ff2d209b6L, // 1e-340 * 2**1257
            0xe95a99df8ace6f54L, 0x0b27d3d3ef868c23L, // 1e-339 * 2**1254
            0x91d8a02bb6c10595L, 0x86f8e46475b41796L, // 1e-338 * 2**1250
            0xb64ec836a47146faL, 0x68b71d7d93211d7bL, // 1e-337 * 2**1247
            0xe3e27a444d8d98b8L, 0x02e4e4dcf7e964daL, // 1e-336 * 2**1244
            0x8e6d8c6ab0787f73L, 0x01cf0f0a1af1df08L, // 1e-335 * 2**1240
            0xb208ef855c969f50L, 0x4242d2cca1ae56caL, // 1e-334 * 2**1237
            0xde8b2b66b3bc4724L, 0x52d3877fca19ec7dL, // 1e-333 * 2**1234
            0x8b16fb203055ac77L, 0xb3c434afde5033ceL, // 1e-332 * 2**1230
            0xaddcb9e83c6b1794L, 0x20b541dbd5e440c2L, // 1e-331 * 2**1227
            0xd953e8624b85dd79L, 0x28e29252cb5d50f2L, // 1e-330 * 2**1224
            0x87d4713d6f33aa6cL, 0x798d9b73bf1a5297L, // 1e-329 * 2**1220
            0xa9c98d8ccb009507L, 0x97f10250aee0e73dL, // 1e-328 * 2**1217
            0xd43bf0effdc0ba49L, 0xfded42e4da99210dL, // 1e-327 * 2**1214
            0x84a57695fe98746eL, 0xfeb449cf089fb4a8L, // 1e-326 * 2**1210
            0xa5ced43b7e3e9189L, 0xbe615c42cac7a1d2L, // 1e-325 * 2**1207
            0xcf42894a5dce35ebL, 0xadf9b3537d798a46L, // 1e-324 * 2**1204
            0x818995ce7aa0e1b3L, 0x8cbc10142e6bf66cL, // 1e-323 * 2**1200
            0xa1ebfb4219491a20L, 0xefeb14193a06f407L, // 1e-322 * 2**1197
            0xca66fa129f9b60a7L, 0x2be5d91f8888b109L, // 1e-321 * 2**1194
            0xfd00b897478238d1L, 0x76df4f676aaadd4bL, // 1e-320 * 2**1191
            0x9e20735e8cb16383L, 0xaa4b91a0a2aaca4fL, // 1e-319 * 2**1187
            0xc5a890362fddbc63L, 0x14de7608cb557ce2L, // 1e-318 * 2**1184
            0xf712b443bbd52b7cL, 0x5a16138afe2adc1bL, // 1e-317 * 2**1181
            0x9a6bb0aa55653b2eL, 0xb84dcc36dedac991L, // 1e-316 * 2**1177
            0xc1069cd4eabe89f9L, 0x66613f4496917bf5L, // 1e-315 * 2**1174
            0xf148440a256e2c77L, 0x3ff98f15bc35daf2L, // 1e-314 * 2**1171
            0x96cd2a865764dbcbL, 0xc7fbf96d95a1a8d7L, // 1e-313 * 2**1167
            0xbc807527ed3e12bdL, 0x39faf7c8fb0a130dL, // 1e-312 * 2**1164
            0xeba09271e88d976cL, 0x0879b5bb39cc97d1L, // 1e-311 * 2**1161
            0x93445b8731587ea4L, 0x854c1195041fdee2L, // 1e-310 * 2**1157
            0xb8157268fdae9e4dL, 0xa69f15fa4527d69bL, // 1e-309 * 2**1154
            0xe61acf033d1a45e0L, 0x9046db78d671cc42L, // 1e-308 * 2**1151
            0x8fd0c16206306bacL, 0x5a2c492b86071fa9L, // 1e-307 * 2**1147
            0xb3c4f1ba87bc8697L, 0x70b75b766788e793L, // 1e-306 * 2**1144
            0xe0b62e2929aba83dL, 0xcce53254016b2178L, // 1e-305 * 2**1141
            0x8c71dcd9ba0b4926L, 0x600f3f7480e2f4ebL, // 1e-304 * 2**1137
            0xaf8e5410288e1b70L, 0xf8130f51a11bb226L, // 1e-303 * 2**1134
            0xdb71e91432b1a24bL, 0x3617d32609629eafL, // 1e-302 * 2**1131
            0x892731ac9faf056fL, 0x41cee3f7c5dda32dL, // 1e-301 * 2**1127
            0xab70fe17c79ac6cbL, 0x92429cf5b7550bf9L, // 1e-300 * 2**1124
            0xd64d3d9db981787eL, 0xf6d34433252a4ef7L, // 1e-299 * 2**1121
            0x85f0468293f0eb4fL, 0xda440a9ff73a715aL, // 1e-298 * 2**1117
            0xa76c582338ed2622L, 0x50d50d47f5090db1L, // 1e-297 * 2**1114
            0xd1476e2c07286fabL, 0xe50a5099f24b511eL, // 1e-296 * 2**1111
            0x82cca4db847945cbL, 0xaf267260376f12b2L, // 1e-295 * 2**1107
            0xa37fce126597973dL, 0x1af00ef8454ad75fL, // 1e-294 * 2**1104
            0xcc5fc196fefd7d0dL, 0xe1ac12b6569d8d37L, // 1e-293 * 2**1101
            0xff77b1fcbebcdc50L, 0xda171763ec44f085L, // 1e-292 * 2**1098
            0x9faacf3df73609b2L, 0x884e6e9e73ab1653L, // 1e-291 * 2**1094
            0xc795830d75038c1eL, 0x2a620a461095dbe8L, // 1e-290 * 2**1091
            0xf97ae3d0d2446f26L, 0xb4fa8cd794bb52e2L, // 1e-289 * 2**1088
            0x9becce62836ac578L, 0xb11c9806bcf513cdL, // 1e-288 * 2**1084
            0xc2e801fb244576d6L, 0xdd63be086c3258c0L, // 1e-287 * 2**1081
            0xf3a20279ed56d48bL, 0x94bcad8a873eeef0L, // 1e-286 * 2**1078
            0x9845418c345644d7L, 0x7cf5ec7694875556L, // 1e-285 * 2**1074
            0xbe5691ef416bd60dL, 0xdc33679439a92aacL, // 1e-284 * 2**1071
            0xedec366b11c6cb90L, 0xd340417948137557L, // 1e-283 * 2**1068
            0x94b3a202eb1c3f3aL, 0x840828ebcd0c2956L, // 1e-282 * 2**1064
            0xb9e08a83a5e34f08L, 0x250a3326c04f33acL, // 1e-281 * 2**1061
            0xe858ad248f5c22caL, 0x2e4cbff070630097L, // 1e-280 * 2**1058
            0x91376c36d99995bfL, 0xdceff7f6463de05eL, // 1e-279 * 2**1054
            0xb58547448ffffb2eL, 0x542bf5f3d7cd5875L, // 1e-278 * 2**1051
            0xe2e69915b3fff9faL, 0xe936f370cdc0ae93L, // 1e-277 * 2**1048
            0x8dd01fad907ffc3cL, 0x51c2582680986d1cL, // 1e-276 * 2**1044
            0xb1442798f49ffb4bL, 0x6632ee3020be8863L, // 1e-275 * 2**1041
            0xdd95317f31c7fa1eL, 0xbfbfa9bc28ee2a7cL, // 1e-274 * 2**1038
            0x8a7d3eef7f1cfc53L, 0xb7d7ca159994da8dL, // 1e-273 * 2**1034
            0xad1c8eab5ee43b67L, 0x25cdbc9afffa1130L, // 1e-272 * 2**1031
            0xd863b256369d4a41L, 0x6f412bc1bff8957dL, // 1e-271 * 2**1028
            0x873e4f75e2224e69L, 0xa588bb5917fb5d6eL, // 1e-270 * 2**1024
            0xa90de3535aaae203L, 0x8eeaea2f5dfa34c9L, // 1e-269 * 2**1021
            0xd3515c2831559a84L, 0xf2a5a4bb3578c1fcL, // 1e-268 * 2**1018
            0x8412d9991ed58092L, 0x17a786f5016b793dL, // 1e-267 * 2**1014
            0xa5178fff668ae0b7L, 0x9d9168b241c6578dL, // 1e-266 * 2**1011
            0xce5d73ff402d98e4L, 0x04f5c2ded237ed70L, // 1e-265 * 2**1008
            0x80fa687f881c7f8fL, 0x831999cb4362f466L, // 1e-264 * 2**1004
            0xa139029f6a239f73L, 0xe3e0003e143bb17fL, // 1e-263 * 2**1001
            0xc987434744ac874fL, 0x5cd8004d994a9ddfL, // 1e-262 * 2**998
            0xfbe9141915d7a923L, 0xb40e0060ff9d4557L, // 1e-261 * 2**995
            0x9d71ac8fada6c9b6L, 0x9088c03c9fc24b56L, // 1e-260 * 2**991
            0xc4ce17b399107c23L, 0x34aaf04bc7b2de2cL, // 1e-259 * 2**988
            0xf6019da07f549b2cL, 0x81d5ac5eb99f95b7L, // 1e-258 * 2**985
            0x99c102844f94e0fcL, 0xd1258bbb3403bd92L, // 1e-257 * 2**981
            0xc0314325637a193aL, 0x056eeeaa0104acf7L, // 1e-256 * 2**978
            0xf03d93eebc589f89L, 0x86caaa548145d835L, // 1e-255 * 2**975
            0x96267c7535b763b6L, 0xb43eaa74d0cba721L, // 1e-254 * 2**971
            0xbbb01b9283253ca3L, 0x614e551204fe90e9L, // 1e-253 * 2**968
            0xea9c227723ee8bccL, 0xb9a1ea56863e3523L, // 1e-252 * 2**965
            0x92a1958a76751760L, 0xf405327613e6e136L, // 1e-251 * 2**961
            0xb749faed14125d37L, 0x31067f1398e09984L, // 1e-250 * 2**958
            0xe51c79a85916f485L, 0x7d481ed87f18bfe5L, // 1e-249 * 2**955
            0x8f31cc0937ae58d3L, 0x2e4d13474f6f77efL, // 1e-248 * 2**951
            0xb2fe3f0b8599ef08L, 0x79e05819234b55eaL, // 1e-247 * 2**948
            0xdfbdcece67006acaL, 0x98586e1f6c1e2b65L, // 1e-246 * 2**945
            0x8bd6a141006042beL, 0x1f3744d3a392db1fL, // 1e-245 * 2**941
            0xaecc49914078536eL, 0xa70516088c7791e7L, // 1e-244 * 2**938
            0xda7f5bf590966849L, 0x50c65b8aaf957661L, // 1e-243 * 2**935
            0x888f99797a5e012eL, 0x927bf936adbd69fcL, // 1e-242 * 2**931
            0xaab37fd7d8f58179L, 0x371af784592cc47cL, // 1e-241 * 2**928
            0xd5605fcdcf32e1d7L, 0x04e1b5656f77f59bL, // 1e-240 * 2**925
            0x855c3be0a17fcd27L, 0xa30d115f65aaf980L, // 1e-239 * 2**921
            0xa6b34ad8c9dfc070L, 0x0bd055b73f15b7e1L, // 1e-238 * 2**918
            0xd0601d8efc57b08cL, 0x0ec46b250edb25d9L, // 1e-237 * 2**915
            0x823c12795db6ce58L, 0x893ac2f72948f7a7L, // 1e-236 * 2**911
            0xa2cb1717b52481eeL, 0xab8973b4f39b3591L, // 1e-235 * 2**908
            0xcb7ddcdda26da269L, 0x566bd0a2308202f6L, // 1e-234 * 2**905
            0xfe5d54150b090b03L, 0x2c06c4cabca283b3L, // 1e-233 * 2**902
            0x9efa548d26e5a6e2L, 0x3b843afeb5e59250L, // 1e-232 * 2**898
            0xc6b8e9b0709f109bL, 0xca6549be635ef6e4L, // 1e-231 * 2**895
            0xf867241c8cc6d4c1L, 0x3cfe9c2dfc36b49dL, // 1e-230 * 2**892
            0x9b407691d7fc44f9L, 0x861f219cbda230e2L, // 1e-229 * 2**888
            0xc21094364dfb5637L, 0x67a6ea03ed0abd1bL, // 1e-228 * 2**885
            0xf294b943e17a2bc5L, 0xc190a484e84d6c62L, // 1e-227 * 2**882
            0x979cf3ca6cec5b5bL, 0x58fa66d3113063bdL, // 1e-226 * 2**878
            0xbd8430bd08277232L, 0xaf390087d57c7cacL, // 1e-225 * 2**875
            0xece53cec4a314ebeL, 0x5b0740a9cadb9bd7L, // 1e-224 * 2**872
            0x940f4613ae5ed137L, 0x78e4886a1ec94166L, // 1e-223 * 2**868
            0xb913179899f68585L, 0xd71daa84a67b91c0L, // 1e-222 * 2**865
            0xe757dd7ec07426e6L, 0xcce51525d01a7630L, // 1e-221 * 2**862
            0x9096ea6f38489850L, 0xc00f2d37a21089deL, // 1e-220 * 2**858
            0xb4bca50b065abe64L, 0xf012f8858a94ac56L, // 1e-219 * 2**855
            0xe1ebce4dc7f16dfcL, 0x2c17b6a6ed39d76bL, // 1e-218 * 2**852
            0x8d3360f09cf6e4beL, 0x9b8ed228544426a3L, // 1e-217 * 2**848
            0xb080392cc4349dedL, 0x427286b26955304cL, // 1e-216 * 2**845
            0xdca04777f541c568L, 0x130f285f03aa7c5fL, // 1e-215 * 2**842
            0x89e42caaf9491b61L, 0x0be9793b624a8dbbL, // 1e-214 * 2**838
            0xac5d37d5b79b623aL, 0xcee3d78a3add312aL, // 1e-213 * 2**835
            0xd77485cb25823ac8L, 0x829ccd6cc9947d74L, // 1e-212 * 2**832
            0x86a8d39ef77164bdL, 0x51a20063fdfcce68L, // 1e-211 * 2**828
            0xa8530886b54dbdecL, 0x260a807cfd7c0203L, // 1e-210 * 2**825
            0xd267caa862a12d67L, 0x2f8d209c3cdb0284L, // 1e-209 * 2**822
            0x8380dea93da4bc61L, 0xbdb83461a608e192L, // 1e-208 * 2**818
            0xa46116538d0deb79L, 0xad26417a0f8b19f7L, // 1e-207 * 2**815
            0xcd795be870516657L, 0x986fd1d8936de074L, // 1e-206 * 2**812
            0x806bd9714632dff7L, 0xff45e3275c24ac49L, // 1e-205 * 2**808
            0xa086cfcd97bf97f4L, 0x7f175bf1332dd75bL, // 1e-204 * 2**805
            0xc8a883c0fdaf7df1L, 0x9edd32ed7ff94d32L, // 1e-203 * 2**802
            0xfad2a4b13d1b5d6dL, 0x86947fa8dff7a07eL, // 1e-202 * 2**799
            0x9cc3a6eec6311a64L, 0x341ccfc98bfac44fL, // 1e-201 * 2**795
            0xc3f490aa77bd60fdL, 0x412403bbeef97563L, // 1e-200 * 2**792
            0xf4f1b4d515acb93cL, 0x116d04aaeab7d2bbL, // 1e-199 * 2**789
            0x991711052d8bf3c6L, 0x8ae422ead2b2e3b5L, // 1e-198 * 2**785
            0xbf5cd54678eef0b7L, 0x2d9d2ba5875f9ca2L, // 1e-197 * 2**782
            0xef340a98172aace5L, 0x7904768ee93783cbL, // 1e-196 * 2**779
            0x9580869f0e7aac0fL, 0x2ba2ca1951c2b25fL, // 1e-195 * 2**775
            0xbae0a846d2195713L, 0x768b7c9fa6335ef6L, // 1e-194 * 2**772
            0xe998d258869facd8L, 0xd42e5bc78fc036b4L, // 1e-193 * 2**769
            0x91ff83775423cc07L, 0x849cf95cb9d82230L, // 1e-192 * 2**765
            0xb67f6455292cbf09L, 0xe5c437b3e84e2abdL, // 1e-191 * 2**762
            0xe41f3d6a7377eecbL, 0xdf3545a0e261b56cL, // 1e-190 * 2**759
            0x8e938662882af53fL, 0xab814b848d7d1163L, // 1e-189 * 2**755
            0xb23867fb2a35b28eL, 0x16619e65b0dc55bcL, // 1e-188 * 2**752
            0xdec681f9f4c31f32L, 0x9bfa05ff1d136b2bL, // 1e-187 * 2**749
            0x8b3c113c38f9f37fL, 0x217c43bf722c22fbL, // 1e-186 * 2**745
            0xae0b158b4738705fL, 0x69db54af4eb72bbaL, // 1e-185 * 2**742
            0xd98ddaee19068c77L, 0xc45229db2264f6a8L, // 1e-184 * 2**739
            0x87f8a8d4cfa417caL, 0x1ab35a28f57f1a29L, // 1e-183 * 2**735
            0xa9f6d30a038d1dbdL, 0xa16030b332dee0b3L, // 1e-182 * 2**732
            0xd47487cc8470652cL, 0x89b83cdfff9698e0L, // 1e-181 * 2**729
            0x84c8d4dfd2c63f3cL, 0xd613260bffbe1f8cL, // 1e-180 * 2**725
            0xa5fb0a17c777cf0aL, 0x0b97ef8effada76fL, // 1e-179 * 2**722
            0xcf79cc9db955c2cdL, 0x8e7deb72bf99114bL, // 1e-178 * 2**719
            0x81ac1fe293d599c0L, 0x390eb327b7bfaacfL, // 1e-177 * 2**715
            0xa21727db38cb0030L, 0x47525ff1a5af9583L, // 1e-176 * 2**712
            0xca9cf1d206fdc03cL, 0x5926f7ee0f1b7ae3L, // 1e-175 * 2**709
            0xfd442e4688bd304bL, 0x6f70b5e992e2599cL, // 1e-174 * 2**706
            0x9e4a9cec15763e2fL, 0x65a671b1fbcd7801L, // 1e-173 * 2**702
            0xc5dd44271ad3cdbbL, 0xbf100e1e7ac0d602L, // 1e-172 * 2**699
            0xf7549530e188c129L, 0x2ed411a619710b83L, // 1e-171 * 2**696
            0x9a94dd3e8cf578baL, 0x7d448b07cfe6a731L, // 1e-170 * 2**692
            0xc13a148e3032d6e8L, 0x1c95adc9c3e050feL, // 1e-169 * 2**689
            0xf18899b1bc3f8ca2L, 0x23bb193c34d8653eL, // 1e-168 * 2**686
            0x96f5600f15a7b7e6L, 0xd654efc5a1073f46L, // 1e-167 * 2**682
            0xbcb2b812db11a5dfL, 0x8bea2bb709490f18L, // 1e-166 * 2**679
            0xebdf661791d60f57L, 0xeee4b6a4cb9b52deL, // 1e-165 * 2**676
            0x936b9fcebb25c996L, 0x354ef226ff4113cbL, // 1e-164 * 2**672
            0xb84687c269ef3bfcL, 0xc2a2aeb0bf1158bdL, // 1e-163 * 2**669
            0xe65829b3046b0afbL, 0xf34b5a5ceed5aeedL, // 1e-162 * 2**666
            0x8ff71a0fe2c2e6ddL, 0xb80f187a15458d54L, // 1e-161 * 2**662
            0xb3f4e093db73a094L, 0xa612de989a96f0a9L, // 1e-160 * 2**659
            0xe0f218b8d25088b9L, 0xcf97963ec13cacd3L, // 1e-159 * 2**656
            0x8c974f7383725574L, 0xe1bebde738c5ec04L, // 1e-158 * 2**652
            0xafbd2350644eead0L, 0x1a2e6d6106f76705L, // 1e-157 * 2**649
            0xdbac6c247d62a584L, 0x20ba08b948b540c6L, // 1e-156 * 2**646
            0x894bc396ce5da773L, 0x94744573cd71487cL, // 1e-155 * 2**642
            0xab9eb47c81f51150L, 0xf99156d0c0cd9a9bL, // 1e-154 * 2**639
            0xd686619ba27255a3L, 0x37f5ac84f1010142L, // 1e-153 * 2**636
            0x8613fd0145877586L, 0x42f98bd316a0a0c9L, // 1e-152 * 2**632
            0xa798fc4196e952e8L, 0xd3b7eec7dc48c8fbL, // 1e-151 * 2**629
            0xd17f3b51fca3a7a1L, 0x08a5ea79d35afb3aL, // 1e-150 * 2**626
            0x82ef85133de648c5L, 0x6567b28c2418dd04L, // 1e-149 * 2**622
            0xa3ab66580d5fdaf6L, 0x3ec19f2f2d1f1445L, // 1e-148 * 2**619
            0xcc963fee10b7d1b4L, 0xce7206faf866d957L, // 1e-147 * 2**616
            0xffbbcfe994e5c620L, 0x020e88b9b6808fadL, // 1e-146 * 2**613
            0x9fd561f1fd0f9bd4L, 0x01491574121059ccL, // 1e-145 * 2**609
            0xc7caba6e7c5382c9L, 0x019b5ad11694703fL, // 1e-144 * 2**606
            0xf9bd690a1b68637cL, 0xc20231855c398c4fL, // 1e-143 * 2**603
            0x9c1661a651213e2eL, 0xf9415ef359a3f7b1L, // 1e-142 * 2**599
            0xc31bfa0fe5698db9L, 0xb791b6b0300cf59dL, // 1e-141 * 2**596
            0xf3e2f893dec3f127L, 0xa576245c3c103305L, // 1e-140 * 2**593
            0x986ddb5c6b3a76b8L, 0x0769d6b9a58a1fe3L, // 1e-139 * 2**589
            0xbe89523386091466L, 0x09444c680eeca7dcL, // 1e-138 * 2**586
            0xee2ba6c0678b5980L, 0x8b955f8212a7d1d3L, // 1e-137 * 2**583
            0x94db483840b717f0L, 0x573d5bb14ba8e323L, // 1e-136 * 2**579
            0xba121a4650e4ddecL, 0x6d0cb29d9e931becL, // 1e-135 * 2**576
            0xe896a0d7e51e1567L, 0x884fdf450637e2e8L, // 1e-134 * 2**573
            0x915e2486ef32cd61L, 0xf531eb8b23e2edd1L, // 1e-133 * 2**569
            0xb5b5ada8aaff80b9L, 0xf27e666decdba945L, // 1e-132 * 2**566
            0xe3231912d5bf60e7L, 0xef1e000968129396L, // 1e-131 * 2**563
            0x8df5efabc5979c90L, 0x3572c005e10b9c3eL, // 1e-130 * 2**559
            0xb1736b96b6fd83b4L, 0x42cf7007594e834dL, // 1e-129 * 2**556
            0xddd0467c64bce4a1L, 0x53834c092fa22421L, // 1e-128 * 2**553
            0x8aa22c0dbef60ee5L, 0x94320f85bdc55694L, // 1e-127 * 2**549
            0xad4ab7112eb3929eL, 0x793e93672d36ac39L, // 1e-126 * 2**546
            0xd89d64d57a607745L, 0x178e3840f8845748L, // 1e-125 * 2**543
            0x87625f056c7c4a8cL, 0xeeb8e3289b52b68dL, // 1e-124 * 2**539
            0xa93af6c6c79b5d2eL, 0x2a671bf2c2276430L, // 1e-123 * 2**536
            0xd389b4787982347aL, 0xb500e2ef72b13d3cL, // 1e-122 * 2**533
            0x843610cb4bf160ccL, 0x31208dd5a7aec645L, // 1e-121 * 2**529
            0xa54394fe1eedb8ffL, 0x3d68b14b119a77d7L, // 1e-120 * 2**526
            0xce947a3da6a9273fL, 0x8cc2dd9dd60115cdL, // 1e-119 * 2**523
            0x811ccc668829b888L, 0xf7f9ca82a5c0ada0L, // 1e-118 * 2**519
            0xa163ff802a3426a9L, 0x35f83d234f30d908L, // 1e-117 * 2**516
            0xc9bcff6034c13053L, 0x03764c6c22fd0f4aL, // 1e-116 * 2**513
            0xfc2c3f3841f17c68L, 0x4453df872bbc531dL, // 1e-115 * 2**510
            0x9d9ba7832936edc1L, 0x2ab46bb47b55b3f2L, // 1e-114 * 2**506
            0xc5029163f384a932L, 0xf56186a19a2b20eeL, // 1e-113 * 2**503
            0xf64335bcf065d37eL, 0xb2b9e84a00b5e92aL, // 1e-112 * 2**500
            0x99ea0196163fa42fL, 0xafb4312e4071b1baL, // 1e-111 * 2**496
            0xc06481fb9bcf8d3aL, 0x1ba13d79d08e1e29L, // 1e-110 * 2**493
            0xf07da27a82c37089L, 0xa2898cd844b1a5b3L, // 1e-109 * 2**490
            0x964e858c91ba2656L, 0xc595f8072aef0790L, // 1e-108 * 2**486
            0xbbe226efb628afebL, 0x76fb7608f5aac974L, // 1e-107 * 2**483
            0xeadab0aba3b2dbe6L, 0xd4ba538b33157bd1L, // 1e-106 * 2**480
            0x92c8ae6b464fc970L, 0xc4f47436ffed6d62L, // 1e-105 * 2**476
            0xb77ada0617e3bbccL, 0xf6319144bfe8c8bbL, // 1e-104 * 2**473
            0xe55990879ddcaabeL, 0x33bdf595efe2faeaL, // 1e-103 * 2**470
            0x8f57fa54c2a9eab7L, 0x6056b97db5eddcd2L, // 1e-102 * 2**466
            0xb32df8e9f3546565L, 0xb86c67dd23695406L, // 1e-101 * 2**463
            0xdff9772470297ebeL, 0xa68781d46c43a908L, // 1e-100 * 2**460
            0x8bfbea76c619ef37L, 0xa814b124c3aa49a5L, // 1e-99 * 2**456
            0xaefae51477a06b04L, 0x1219dd6df494dc0eL, // 1e-98 * 2**453
            0xdab99e59958885c5L, 0x16a054c971ba1312L, // 1e-97 * 2**450
            0x88b402f7fd75539cL, 0xee2434fde7144bebL, // 1e-96 * 2**446
            0xaae103b5fcd2a882L, 0x29ad423d60d95ee6L, // 1e-95 * 2**443
            0xd59944a37c0752a3L, 0xb41892ccb90fb6a0L, // 1e-94 * 2**440
            0x857fcae62d8493a6L, 0x908f5bbff3a9d224L, // 1e-93 * 2**436
            0xa6dfbd9fb8e5b88fL, 0x34b332aff09446adL, // 1e-92 * 2**433
            0xd097ad07a71f26b3L, 0x81dfff5becb95858L, // 1e-91 * 2**430
            0x825ecc24c8737830L, 0x712bff9973f3d737L, // 1e-90 * 2**426
            0xa2f67f2dfa90563cL, 0x8d76ff7fd0f0cd05L, // 1e-89 * 2**423
            0xcbb41ef979346bcbL, 0xb0d4bf5fc52d0046L, // 1e-88 * 2**420
            0xfea126b7d78186bdL, 0x1d09ef37b6784057L, // 1e-87 * 2**417
            0x9f24b832e6b0f437L, 0xf2263582d20b2836L, // 1e-86 * 2**413
            0xc6ede63fa05d3144L, 0x6eafc2e3868df244L, // 1e-85 * 2**410
            0xf8a95fcf88747d95L, 0x8a5bb39c68316ed5L, // 1e-84 * 2**407
            0x9b69dbe1b548ce7dL, 0x36795041c11ee545L, // 1e-83 * 2**403
            0xc24452da229b021cL, 0x0417a45231669e97L, // 1e-82 * 2**400
            0xf2d56790ab41c2a3L, 0x051d8d66bdc0463cL, // 1e-81 * 2**397
            0x97c560ba6b0919a6L, 0x2332786036982be5L, // 1e-80 * 2**393
            0xbdb6b8e905cb6010L, 0xabff1678443e36dfL, // 1e-79 * 2**390
            0xed246723473e3814L, 0xd6fedc16554dc497L, // 1e-78 * 2**387
            0x9436c0760c86e30cL, 0x065f498df5509adeL, // 1e-77 * 2**383
            0xb94470938fa89bcfL, 0x07f71bf172a4c196L, // 1e-76 * 2**380
            0xe7958cb87392c2c3L, 0x49f4e2edcf4df1fbL, // 1e-75 * 2**377
            0x90bd77f3483bb9baL, 0x4e390dd4a190b73dL, // 1e-74 * 2**373
            0xb4ecd5f01a4aa829L, 0xe1c75149c9f4e50cL, // 1e-73 * 2**370
            0xe2280b6c20dd5233L, 0xda39259c3c721e4fL, // 1e-72 * 2**367
            0x8d590723948a5360L, 0xa863b781a5c752f1L, // 1e-71 * 2**363
            0xb0af48ec79ace838L, 0xd27ca5620f3927aeL, // 1e-70 * 2**360
            0xdcdb1b2798182245L, 0x071bceba9307719aL, // 1e-69 * 2**357
            0x8a08f0f8bf0f156cL, 0xe47161349be4a700L, // 1e-68 * 2**353
            0xac8b2d36eed2dac6L, 0x1d8db981c2ddd0c0L, // 1e-67 * 2**350
            0xd7adf884aa879178L, 0xa4f127e2339544f0L, // 1e-66 * 2**347
            0x86ccbb52ea94baebL, 0x6716b8ed603d4b16L, // 1e-65 * 2**343
            0xa87fea27a539e9a6L, 0xc0dc6728b84c9ddbL, // 1e-64 * 2**340
            0xd29fe4b18e88640fL, 0x711380f2e65fc552L, // 1e-63 * 2**337
            0x83a3eeeef9153e8aL, 0xe6ac3097cffbdb53L, // 1e-62 * 2**333
            0xa48ceaaab75a8e2cL, 0xa0573cbdc3fad228L, // 1e-61 * 2**330
            0xcdb02555653131b7L, 0xc86d0bed34f986b2L, // 1e-60 * 2**327
            0x808e17555f3ebf12L, 0x1d442774411bf42fL, // 1e-59 * 2**323
            0xa0b19d2ab70e6ed7L, 0xa49531515162f13bL, // 1e-58 * 2**320
            0xc8de047564d20a8cL, 0x0dba7da5a5bbad8aL, // 1e-57 * 2**317
            0xfb158592be068d2fL, 0x11291d0f0f2a98edL, // 1e-56 * 2**314
            0x9ced737bb6c4183eL, 0xaab9b229697a9f94L, // 1e-55 * 2**310
            0xc428d05aa4751e4dL, 0x55681eb3c3d94779L, // 1e-54 * 2**307
            0xf53304714d9265e0L, 0x2ac22660b4cf9957L, // 1e-53 * 2**304
            0x993fe2c6d07b7facL, 0x1ab957fc7101bfd6L, // 1e-52 * 2**300
            0xbf8fdb78849a5f97L, 0x2167adfb8d422fccL, // 1e-51 * 2**297
            0xef73d256a5c0f77dL, 0x69c1997a7092bbbfL, // 1e-50 * 2**294
            0x95a8637627989aaeL, 0x2218ffec865bb557L, // 1e-49 * 2**290
            0xbb127c53b17ec15aL, 0xaa9f3fe7a7f2a2adL, // 1e-48 * 2**287
            0xe9d71b689dde71b0L, 0x55470fe191ef4b59L, // 1e-47 * 2**284
            0x9226712162ab070eL, 0x354c69ecfb358f17L, // 1e-46 * 2**280
            0xb6b00d69bb55c8d2L, 0xc29f84683a02f2ddL, // 1e-45 * 2**277
            0xe45c10c42a2b3b06L, 0x734765824883af95L, // 1e-44 * 2**274
            0x8eb98a7a9a5b04e4L, 0x880c9f716d524dbdL, // 1e-43 * 2**270
            0xb267ed1940f1c61dL, 0xaa0fc74dc8a6e12cL, // 1e-42 * 2**267
            0xdf01e85f912e37a4L, 0x9493b9213ad09977L, // 1e-41 * 2**264
            0x8b61313bbabce2c7L, 0xdcdc53b4c4c25feaL, // 1e-40 * 2**260
            0xae397d8aa96c1b78L, 0x541368a1f5f2f7e5L, // 1e-39 * 2**257
            0xd9c7dced53c72256L, 0x691842ca736fb5deL, // 1e-38 * 2**254
            0x881cea14545c7576L, 0x81af29be8825d1abL, // 1e-37 * 2**250
            0xaa242499697392d3L, 0x221af42e2a2f4616L, // 1e-36 * 2**247
            0xd4ad2dbfc3d07788L, 0x6aa1b139b4bb179bL, // 1e-35 * 2**244
            0x84ec3c97da624ab5L, 0x42a50ec410f4eec1L, // 1e-34 * 2**240
            0xa6274bbdd0fadd62L, 0x134e527515322a71L, // 1e-33 * 2**237
            0xcfb11ead453994bbL, 0x9821e7125a7eb50dL, // 1e-32 * 2**234
            0x81ceb32c4b43fcf5L, 0x7f15306b788f3128L, // 1e-31 * 2**230
            0xa2425ff75e14fc32L, 0x5eda7c8656b2fd72L, // 1e-30 * 2**227
            0xcad2f7f5359a3b3fL, 0xf6911ba7ec5fbccfL, // 1e-29 * 2**224
            0xfd87b5f28300ca0eL, 0x74356291e777ac03L, // 1e-28 * 2**221
            0x9e74d1b791e07e49L, 0x88a15d9b30aacb82L, // 1e-27 * 2**217
            0xc612062576589ddbL, 0x6ac9b501fcd57e62L, // 1e-26 * 2**214
            0xf79687aed3eec552L, 0xc57c22427c0addfbL, // 1e-25 * 2**211
            0x9abe14cd44753b53L, 0x3b6d95698d86cabdL, // 1e-24 * 2**207
            0xc16d9a0095928a28L, 0x8a48fac3f0e87d6cL, // 1e-23 * 2**204
            0xf1c90080baf72cb2L, 0xacdb3974ed229cc7L, // 1e-22 * 2**201
            0x971da05074da7befL, 0x2c0903e91435a1fcL, // 1e-21 * 2**197
            0xbce5086492111aebL, 0x770b44e359430a7bL, // 1e-20 * 2**194
            0xec1e4a7db69561a6L, 0xd4ce161c2f93cd1aL, // 1e-19 * 2**191
            0x9392ee8e921d5d08L, 0xc500cdd19dbc6030L, // 1e-18 * 2**187
            0xb877aa3236a4b44aL, 0xf6410146052b783dL, // 1e-17 * 2**184
            0xe69594bec44de15cL, 0xb3d141978676564cL, // 1e-16 * 2**181
            0x901d7cf73ab0acdaL, 0xf062c8feb409f5efL, // 1e-15 * 2**177
            0xb424dc35095cd810L, 0xac7b7b3e610c736bL, // 1e-14 * 2**174
            0xe12e13424bb40e14L, 0xd79a5a0df94f9046L, // 1e-13 * 2**171
            0x8cbccc096f5088ccL, 0x06c07848bbd1ba2cL, // 1e-12 * 2**167
            0xafebff0bcb24aaffL, 0x0870965aeac628b7L, // 1e-11 * 2**164
            0xdbe6fecebdedd5bfL, 0x4a8cbbf1a577b2e4L, // 1e-10 * 2**161
            0x89705f4136b4a598L, 0xce97f577076acfcfL, // 1e-9 * 2**157
            0xabcc77118461cefdL, 0x023df2d4c94583c2L, // 1e-8 * 2**154
            0xd6bf94d5e57a42bdL, 0xc2cd6f89fb96e4b3L, // 1e-7 * 2**151
            0x8637bd05af6c69b6L, 0x59c065b63d3e4ef0L, // 1e-6 * 2**147
            0xa7c5ac471b478424L, 0xf0307f23cc8de2acL, // 1e-5 * 2**144
            0xd1b71758e219652cL, 0x2c3c9eecbfb15b57L, // 1e-4 * 2**141
            0x83126e978d4fdf3cL, 0x9ba5e353f7ced916L, // 1e-3 * 2**137
            0xa3d70a3d70a3d70bL, 0xc28f5c28f5c28f5cL, // 1e-2 * 2**134
            0xcccccccccccccccdL, 0x3333333333333333L, // 1e-1 * 2**131
            0x8000000000000000L, 0x0000000000000000L, // 1e0 * 2**127
            0xa000000000000000L, 0x0000000000000000L, // 1e1 * 2**124
            0xc800000000000000L, 0x0000000000000000L, // 1e2 * 2**121
            0xfa00000000000000L, 0x0000000000000000L, // 1e3 * 2**118
            0x9c40000000000000L, 0x0000000000000000L, // 1e4 * 2**114
            0xc350000000000000L, 0x0000000000000000L, // 1e5 * 2**111
            0xf424000000000000L, 0x0000000000000000L, // 1e6 * 2**108
            0x9896800000000000L, 0x0000000000000000L, // 1e7 * 2**104
            0xbebc200000000000L, 0x0000000000000000L, // 1e8 * 2**101
            0xee6b280000000000L, 0x0000000000000000L, // 1e9 * 2**98
            0x9502f90000000000L, 0x0000000000000000L, // 1e10 * 2**94
            0xba43b74000000000L, 0x0000000000000000L, // 1e11 * 2**91
            0xe8d4a51000000000L, 0x0000000000000000L, // 1e12 * 2**88
            0x9184e72a00000000L, 0x0000000000000000L, // 1e13 * 2**84
            0xb5e620f480000000L, 0x0000000000000000L, // 1e14 * 2**81
            0xe35fa931a0000000L, 0x0000000000000000L, // 1e15 * 2**78
            0x8e1bc9bf04000000L, 0x0000000000000000L, // 1e16 * 2**74
            0xb1a2bc2ec5000000L, 0x0000000000000000L, // 1e17 * 2**71
            0xde0b6b3a76400000L, 0x0000000000000000L, // 1e18 * 2**68
            0x8ac7230489e80000L, 0x0000000000000000L, // 1e19 * 2**64
            0xad78ebc5ac620000L, 0x0000000000000000L, // 1e20 * 2**61
            0xd8d726b7177a8000L, 0x0000000000000000L, // 1e21 * 2**58
            0x878678326eac9000L, 0x0000000000000000L, // 1e22 * 2**54
            0xa968163f0a57b400L, 0x0000000000000000L, // 1e23 * 2**51
            0xd3c21bcecceda100L, 0x0000000000000000L, // 1e24 * 2**48
            0x84595161401484a0L, 0x0000000000000000L, // 1e25 * 2**44
            0xa56fa5b99019a5c8L, 0x0000000000000000L, // 1e26 * 2**41
            0xcecb8f27f4200f3aL, 0x0000000000000000L, // 1e27 * 2**38
            0x813f3978f8940985L, 0xbfffffffffffffffL, // 1e28 * 2**34
            0xa18f07d736b90be6L, 0xafffffffffffffffL, // 1e29 * 2**31
            0xc9f2c9cd04674edfL, 0x5bffffffffffffffL, // 1e30 * 2**28
            0xfc6f7c4045812297L, 0xb2ffffffffffffffL, // 1e31 * 2**25
            0x9dc5ada82b70b59eL, 0x0fdfffffffffffffL, // 1e32 * 2**21
            0xc5371912364ce306L, 0x93d7ffffffffffffL, // 1e33 * 2**18
            0xf684df56c3e01bc7L, 0x38cdffffffffffffL, // 1e34 * 2**15
            0x9a130b963a6c115dL, 0xc380bfffffffffffL, // 1e35 * 2**11
            0xc097ce7bc90715b4L, 0xb460efffffffffffL, // 1e36 * 2**8
            0xf0bdc21abb48db21L, 0xe1792bffffffffffL, // 1e37 * 2**5
            0x96769950b50d88f5L, 0xecebbb7fffffffffL, // 1e38 * 2**1
            0xbc143fa4e250eb32L, 0xe826aa5fffffffffL, // 1e39 * 2**-2
            0xeb194f8e1ae525feL, 0xa23054f7ffffffffL, // 1e40 * 2**-5
            0x92efd1b8d0cf37bfL, 0xa55e351affffffffL, // 1e41 * 2**-9
            0xb7abc627050305aeL, 0x0eb5c261bfffffffL, // 1e42 * 2**-12
            0xe596b7b0c643c71aL, 0x926332fa2fffffffL, // 1e43 * 2**-15
            0x8f7e32ce7bea5c70L, 0x1b7dffdc5dffffffL, // 1e44 * 2**-19
            0xb35dbf821ae4f38cL, 0x225d7fd3757fffffL, // 1e45 * 2**-22
            0xe0352f62a19e306fL, 0x2af4dfc852dfffffL, // 1e46 * 2**-25
            0x8c213d9da502de46L, 0xbad90bdd33cbffffL, // 1e47 * 2**-29
            0xaf298d050e4395d7L, 0x698f4ed480beffffL, // 1e48 * 2**-32
            0xdaf3f04651d47b4dL, 0xc3f32289a0eebfffL, // 1e49 * 2**-35
            0x88d8762bf324cd10L, 0x5a77f596049537ffL, // 1e50 * 2**-39
            0xab0e93b6efee0054L, 0x7115f2fb85ba85ffL, // 1e51 * 2**-42
            0xd5d238a4abe98069L, 0x8d5b6fba6729277fL, // 1e52 * 2**-45
            0x85a36366eb71f042L, 0xb85925d48079b8afL, // 1e53 * 2**-49
            0xa70c3c40a64e6c52L, 0x666f6f49a09826dbL, // 1e54 * 2**-52
            0xd0cf4b50cfe20766L, 0x000b4b1c08be3092L, // 1e55 * 2**-55
            0x82818f1281ed44a0L, 0x40070ef18576de5bL, // 1e56 * 2**-59
            0xa321f2d7226895c8L, 0x5008d2ade6d495f2L, // 1e57 * 2**-62
            0xcbea6f8ceb02bb3aL, 0x640b07596089bb6fL, // 1e58 * 2**-65
            0xfee50b7025c36a09L, 0xfd0dc92fb8ac2a4bL, // 1e59 * 2**-68
            0x9f4f2726179a2246L, 0xfe289dbdd36b9a6fL, // 1e60 * 2**-72
            0xc722f0ef9d80aad7L, 0xbdb2c52d4846810aL, // 1e61 * 2**-75
            0xf8ebad2b84e0d58cL, 0x2d1f76789a58214dL, // 1e62 * 2**-78
            0x9b934c3b330c8578L, 0x9c33aa0b607714d0L, // 1e63 * 2**-82
            0xc2781f49ffcfa6d6L, 0xc340948e3894da04L, // 1e64 * 2**-85
            0xf316271c7fc3908bL, 0x7410b9b1c6ba1085L, // 1e65 * 2**-88
            0x97edd871cfda3a57L, 0x688a740f1c344a53L, // 1e66 * 2**-92
            0xbde94e8e43d0c8edL, 0xc2ad1112e3415ce8L, // 1e67 * 2**-95
            0xed63a231d4c4fb28L, 0xb35855579c11b422L, // 1e68 * 2**-98
            0x945e455f24fb1cf9L, 0x70173556c18b1095L, // 1e69 * 2**-102
            0xb975d6b6ee39e437L, 0x4c1d02ac71edd4bbL, // 1e70 * 2**-105
            0xe7d34c64a9c85d45L, 0x9f2443578e6949e9L, // 1e71 * 2**-108
            0x90e40fbeea1d3a4bL, 0x4376aa16b901ce32L, // 1e72 * 2**-112
            0xb51d13aea4a488deL, 0x9454549c674241beL, // 1e73 * 2**-115
            0xe264589a4dcdab15L, 0x396969c38112d22eL, // 1e74 * 2**-118
            0x8d7eb76070a08aedL, 0x03e1e21a30abc35dL, // 1e75 * 2**-122
            0xb0de65388cc8ada9L, 0xc4da5aa0bcd6b434L, // 1e76 * 2**-125
            0xdd15fe86affad913L, 0xb610f148ec0c6141L, // 1e77 * 2**-128
            0x8a2dbf142dfcc7acL, 0x91ca96cd9387bcc8L, // 1e78 * 2**-132
            0xacb92ed9397bf997L, 0xb63d3c80f869abfbL, // 1e79 * 2**-135
            0xd7e77a8f87daf7fcL, 0x23cc8ba1368416f9L, // 1e80 * 2**-138
            0x86f0ac99b4e8dafeL, 0x965fd744c2128e5cL, // 1e81 * 2**-142
            0xa8acd7c0222311bdL, 0x3bf7cd15f29731f3L, // 1e82 * 2**-145
            0xd2d80db02aabd62cL, 0x0af5c05b6f3cfe6fL, // 1e83 * 2**-148
            0x83c7088e1aab65dcL, 0x86d9983925861f05L, // 1e84 * 2**-152
            0xa4b8cab1a1563f53L, 0xa88ffe476ee7a6c7L, // 1e85 * 2**-155
            0xcde6fd5e09abcf27L, 0x12b3fdd94aa19079L, // 1e86 * 2**-158
            0x80b05e5ac60b6179L, 0xabb07ea7cea4fa4bL, // 1e87 * 2**-162
            0xa0dc75f1778e39d7L, 0x969c9e51c24e38deL, // 1e88 * 2**-165
            0xc913936dd571c84dL, 0xfc43c5e632e1c716L, // 1e89 * 2**-168
            0xfb5878494ace3a60L, 0xfb54b75fbf9a38dcL, // 1e90 * 2**-171
            0x9d174b2dcec0e47cL, 0x9d14f29bd7c06389L, // 1e91 * 2**-175
            0xc45d1df942711d9bL, 0xc45a2f42cdb07c6bL, // 1e92 * 2**-178
            0xf5746577930d6501L, 0x3570bb13811c9b86L, // 1e93 * 2**-181
            0x9968bf6abbe85f21L, 0x816674ec30b1e134L, // 1e94 * 2**-185
            0xbfc2ef456ae276e9L, 0x61c012273cde5981L, // 1e95 * 2**-188
            0xefb3ab16c59b14a3L, 0x3a3016b10c15efe1L, // 1e96 * 2**-191
            0x95d04aee3b80ece6L, 0x445e0e2ea78db5edL, // 1e97 * 2**-195
            0xbb445da9ca612820L, 0xd57591ba51712368L, // 1e98 * 2**-198
            0xea1575143cf97227L, 0x0ad2f628e5cd6c42L, // 1e99 * 2**-201
            0x924d692ca61be759L, 0xa6c3d9d98fa063a9L, // 1e100 * 2**-205
            0xb6e0c377cfa2e12fL, 0x9074d04ff3887c93L, // 1e101 * 2**-208
            0xe498f455c38b997bL, 0xf4920463f06a9bb8L, // 1e102 * 2**-211
            0x8edf98b59a373fedL, 0xb8db42be7642a153L, // 1e103 * 2**-215
            0xb2977ee300c50fe8L, 0xa712136e13d349a8L, // 1e104 * 2**-218
            0xdf3d5e9bc0f653e2L, 0xd0d6984998c81c12L, // 1e105 * 2**-221
            0x8b865b215899f46dL, 0x42861f2dff7d118bL, // 1e106 * 2**-225
            0xae67f1e9aec07188L, 0x1327a6f97f5c55eeL, // 1e107 * 2**-228
            0xda01ee641a708deaL, 0x17f190b7df336b6aL, // 1e108 * 2**-231
            0x884134fe908658b3L, 0xcef6fa72eb802322L, // 1e109 * 2**-235
            0xaa51823e34a7eedfL, 0x42b4b90fa6602beaL, // 1e110 * 2**-238
            0xd4e5e2cdc1d1ea97L, 0x9361e7538ff836e5L, // 1e111 * 2**-241
            0x850fadc09923329fL, 0xfc1d309439fb224fL, // 1e112 * 2**-245
            0xa6539930bf6bff46L, 0x7b247cb94879eae3L, // 1e113 * 2**-248
            0xcfe87f7cef46ff17L, 0x19ed9be79a98659cL, // 1e114 * 2**-251
            0x81f14fae158c5f6fL, 0xb0348170c09f3f81L, // 1e115 * 2**-255
            0xa26da3999aef774aL, 0x1c41a1ccf0c70f62L, // 1e116 * 2**-258
            0xcb090c8001ab551dL, 0xa3520a402cf8d33aL, // 1e117 * 2**-261
            0xfdcb4fa002162a64L, 0x8c268cd038370809L, // 1e118 * 2**-264
            0x9e9f11c4014dda7fL, 0xd798180223226505L, // 1e119 * 2**-268
            0xc646d63501a1511eL, 0x4d7e1e02abeafe47L, // 1e120 * 2**-271
            0xf7d88bc24209a566L, 0xe0dda58356e5bdd9L, // 1e121 * 2**-274
            0x9ae7575969460760L, 0xcc8a8772164f96a7L, // 1e122 * 2**-278
            0xc1a12d2fc3978938L, 0xffad294e9be37c51L, // 1e123 * 2**-281
            0xf209787bb47d6b85L, 0x3f9873a242dc5b65L, // 1e124 * 2**-284
            0x9745eb4d50ce6333L, 0x07bf484569c9b91fL, // 1e125 * 2**-288
            0xbd176620a501fc00L, 0x49af1a56c43c2767L, // 1e126 * 2**-291
            0xec5d3fa8ce427b00L, 0x5c1ae0ec754b3141L, // 1e127 * 2**-294
            0x93ba47c980e98ce0L, 0x3990cc93c94efec8L, // 1e128 * 2**-298
            0xb8a8d9bbe123f018L, 0x47f4ffb8bba2be7bL, // 1e129 * 2**-301
            0xe6d3102ad96cec1eL, 0x59f23fa6ea8b6e1aL, // 1e130 * 2**-304
            0x9043ea1ac7e41393L, 0x783767c8529724d0L, // 1e131 * 2**-308
            0xb454e4a179dd1878L, 0xd64541ba673cee04L, // 1e132 * 2**-311
            0xe16a1dc9d8545e95L, 0x0bd69229010c2985L, // 1e133 * 2**-314
            0x8ce2529e2734bb1eL, 0xe7661b59a0a799f3L, // 1e134 * 2**-318
            0xb01ae745b101e9e5L, 0xa13fa23008d18070L, // 1e135 * 2**-321
            0xdc21a1171d42645eL, 0x898f8abc0b05e08cL, // 1e136 * 2**-324
            0x899504ae72497ebbL, 0x95f9b6b586e3ac57L, // 1e137 * 2**-328
            0xabfa45da0edbde6aL, 0xfb782462e89c976dL, // 1e138 * 2**-331
            0xd6f8d7509292d604L, 0xba562d7ba2c3bd49L, // 1e139 * 2**-334
            0x865b86925b9bc5c3L, 0xf475dc6d45ba564dL, // 1e140 * 2**-338
            0xa7f26836f282b733L, 0x719353889728ebe1L, // 1e141 * 2**-341
            0xd1ef0244af236500L, 0xcdf8286abcf326d9L, // 1e142 * 2**-344
            0x8335616aed761f20L, 0x80bb1942b617f847L, // 1e143 * 2**-348
            0xa402b9c5a8d3a6e8L, 0xa0e9df93639df659L, // 1e144 * 2**-351
            0xcd036837130890a2L, 0xc92457783c8573f0L, // 1e145 * 2**-354
            0x802221226be55a65L, 0x3db6b6ab25d36876L, // 1e146 * 2**-358
            0xa02aa96b06deb0feL, 0x0d246455ef484293L, // 1e147 * 2**-361
            0xc83553c5c8965d3eL, 0x906d7d6b6b1a5338L, // 1e148 * 2**-364
            0xfa42a8b73abbf48dL, 0x3488dcc645e0e806L, // 1e149 * 2**-367
            0x9c69a97284b578d8L, 0x00d589fbebac9104L, // 1e150 * 2**-371
            0xc38413cf25e2d70eL, 0x010aec7ae697b545L, // 1e151 * 2**-374
            0xf46518c2ef5b8cd2L, 0x814da799a03da296L, // 1e152 * 2**-377
            0x98bf2f79d5993803L, 0x10d088c00426859eL, // 1e153 * 2**-381
            0xbeeefb584aff8604L, 0x5504aaf005302705L, // 1e154 * 2**-384
            0xeeaaba2e5dbf6785L, 0x6a45d5ac067c30c7L, // 1e155 * 2**-387
            0x952ab45cfa97a0b3L, 0x226ba58b840d9e7cL, // 1e156 * 2**-391
            0xba756174393d88e0L, 0x6b068eee6511061bL, // 1e157 * 2**-394
            0xe912b9d1478ceb18L, 0x85c832a9fe5547a2L, // 1e158 * 2**-397
            0x91abb422ccb812efL, 0x539d1faa3ef54cc5L, // 1e159 * 2**-401
            0xb616a12b7fe617abL, 0xa8846794ceb29ff6L, // 1e160 * 2**-404
            0xe39c49765fdf9d95L, 0x12a5817a025f47f4L, // 1e161 * 2**-407
            0x8e41ade9fbebc27eL, 0xeba770ec417b8cf8L, // 1e162 * 2**-411
            0xb1d219647ae6b31dL, 0xa6914d2751da7037L, // 1e163 * 2**-414
            0xde469fbd99a05fe4L, 0x9035a07126510c44L, // 1e164 * 2**-417
            0x8aec23d680043befL, 0xda218446b7f2a7abL, // 1e165 * 2**-421
            0xada72ccc20054aeaL, 0x50a9e55865ef5195L, // 1e166 * 2**-424
            0xd910f7ff28069da5L, 0xe4d45eae7f6b25fbL, // 1e167 * 2**-427
            0x87aa9aff79042287L, 0x6f04bb2d0fa2f7bdL, // 1e168 * 2**-431
            0xa99541bf57452b29L, 0xcac5e9f8538bb5acL, // 1e169 * 2**-434
            0xd3fa922f2d1675f3L, 0xbd776476686ea317L, // 1e170 * 2**-437
            0x847c9b5d7c2e09b8L, 0x966a9eca014525eeL, // 1e171 * 2**-441
            0xa59bc234db398c26L, 0xbc05467c81966f6aL, // 1e172 * 2**-444
            0xcf02b2c21207ef2fL, 0x6b06981ba1fc0b44L, // 1e173 * 2**-447
            0x8161afb94b44f57eL, 0xe2e41f11453d870aL, // 1e174 * 2**-451
            0xa1ba1ba79e1632ddL, 0x9b9d26d5968ce8cdL, // 1e175 * 2**-454
            0xca28a291859bbf94L, 0x8284708afc302301L, // 1e176 * 2**-457
            0xfcb2cb35e702af79L, 0xa3258cadbb3c2bc1L, // 1e177 * 2**-460
            0x9defbf01b061adacL, 0xc5f777ec95059b58L, // 1e178 * 2**-464
            0xc56baec21c7a1917L, 0xf77555e7ba47022fL, // 1e179 * 2**-467
            0xf6c69a72a3989f5cL, 0x7552ab61a8d8c2baL, // 1e180 * 2**-470
            0x9a3c2087a63f639aL, 0xc953ab1d098779b4L, // 1e181 * 2**-474
            0xc0cb28a98fcf3c80L, 0x7ba895e44be95822L, // 1e182 * 2**-477
            0xf0fdf2d3f3c30ba0L, 0x9a92bb5d5ee3ae2aL, // 1e183 * 2**-480
            0x969eb7c47859e744L, 0x609bb51a5b4e4cdaL, // 1e184 * 2**-484
            0xbc4665b596706115L, 0x78c2a260f221e011L, // 1e185 * 2**-487
            0xeb57ff22fc0c795aL, 0x56f34af92eaa5815L, // 1e186 * 2**-490
            0x9316ff75dd87cbd9L, 0xf6580edbbd2a770dL, // 1e187 * 2**-494
            0xb7dcbf5354e9becfL, 0xf3ee1292ac7514d0L, // 1e188 * 2**-497
            0xe5d3ef282a242e82L, 0x70e9973757925a05L, // 1e189 * 2**-500
            0x8fa475791a569d11L, 0x0691fe8296bb7843L, // 1e190 * 2**-504
            0xb38d92d760ec4456L, 0xc8367e233c6a5653L, // 1e191 * 2**-507
            0xe070f78d3927556bL, 0x7a441dac0b84ebe8L, // 1e192 * 2**-510
            0x8c469ab843b89563L, 0x6c6a928b87331371L, // 1e193 * 2**-514
            0xaf58416654a6babcL, 0xc785372e68ffd84dL, // 1e194 * 2**-517
            0xdb2e51bfe9d0696bL, 0xf96684fa033fce61L, // 1e195 * 2**-520
            0x88fcf317f22241e3L, 0xbbe0131c4207e0fcL, // 1e196 * 2**-524
            0xab3c2fddeeaad25bL, 0x2ad817e35289d93cL, // 1e197 * 2**-527
            0xd60b3bd56a5586f2L, 0x758e1ddc272c4f8bL, // 1e198 * 2**-530
            0x85c7056562757457L, 0x0978d2a9987bb1b6L, // 1e199 * 2**-534
            0xa738c6bebb12d16dL, 0x4bd70753fe9a9e24L, // 1e200 * 2**-537
            0xd106f86e69d785c8L, 0x1eccc928fe4145adL, // 1e201 * 2**-540
            0x82a45b450226b39dL, 0x133ffdb99ee8cb8cL, // 1e202 * 2**-544
            0xa34d721642b06085L, 0xd80ffd2806a2fe6fL, // 1e203 * 2**-547
            0xcc20ce9bd35c78a6L, 0xce13fc72084bbe0bL, // 1e204 * 2**-550
            0xff290242c83396cfL, 0x8198fb8e8a5ead8eL, // 1e205 * 2**-553
            0x9f79a169bd203e42L, 0xf0ff9d39167b2c79L, // 1e206 * 2**-557
            0xc75809c42c684dd2L, 0xad3f84875c19f797L, // 1e207 * 2**-560
            0xf92e0c3537826146L, 0x588f65a93320757dL, // 1e208 * 2**-563
            0x9bbcc7a142b17cccL, 0x77599f89bff4496eL, // 1e209 * 2**-567
            0xc2abf989935ddbffL, 0x9530076c2ff15bcaL, // 1e210 * 2**-570
            0xf356f7ebf83552ffL, 0xfa7c09473bedb2bcL, // 1e211 * 2**-573
            0x98165af37b2153dfL, 0x3c8d85cc85748fb5L, // 1e212 * 2**-577
            0xbe1bf1b059e9a8d7L, 0x8bb0e73fa6d1b3a3L, // 1e213 * 2**-580
            0xeda2ee1c7064130dL, 0xee9d210f9086208cL, // 1e214 * 2**-583
            0x9485d4d1c63e8be8L, 0x752234a9ba53d457L, // 1e215 * 2**-587
            0xb9a74a0637ce2ee2L, 0x926ac1d428e8c96dL, // 1e216 * 2**-590
            0xe8111c87c5c1ba9aL, 0x370572493322fbc8L, // 1e217 * 2**-593
            0x910ab1d4db9914a1L, 0xe263676dbff5dd5dL, // 1e218 * 2**-597
            0xb54d5e4a127f59c9L, 0xdafc41492ff354b4L, // 1e219 * 2**-600
            0xe2a0b5dc971f303bL, 0xd1bb519b7bf029e2L, // 1e220 * 2**-603
            0x8da471a9de737e25L, 0xa31513012d761a2dL, // 1e221 * 2**-607
            0xb10d8e1456105daeL, 0x8bda57c178d3a0b8L, // 1e222 * 2**-610
            0xdd50f1996b947519L, 0x2ed0edb1d70888e6L, // 1e223 * 2**-613
            0x8a5296ffe33cc930L, 0x7d42948f26655590L, // 1e224 * 2**-617
            0xace73cbfdc0bfb7cL, 0x9c9339b2effeaaf4L, // 1e225 * 2**-620
            0xd8210befd30efa5bL, 0xc3b8081fabfe55b1L, // 1e226 * 2**-623
            0x8714a775e3e95c79L, 0x9a530513cb7ef58eL, // 1e227 * 2**-627
            0xa8d9d1535ce3b397L, 0x80e7c658be5eb2f2L, // 1e228 * 2**-630
            0xd31045a8341ca07dL, 0xe121b7eeedf65fafL, // 1e229 * 2**-633
            0x83ea2b892091e44eL, 0x6cb512f554b9fbcdL, // 1e230 * 2**-637
            0xa4e4b66b68b65d61L, 0x07e257b2a9e87ac0L, // 1e231 * 2**-640
            0xce1de40642e3f4baL, 0xc9daed9f54629971L, // 1e232 * 2**-643
            0x80d2ae83e9ce78f4L, 0x3e28d48394bd9fe6L, // 1e233 * 2**-647
            0xa1075a24e4421731L, 0x4db309a479ed07e0L, // 1e234 * 2**-650
            0xc94930ae1d529cfdL, 0x211fcc0d986849d8L, // 1e235 * 2**-653
            0xfb9b7cd9a4a7443dL, 0xe967bf10fe825c4eL, // 1e236 * 2**-656
            0x9d412e0806e88aa6L, 0x71e0d76a9f1179b1L, // 1e237 * 2**-660
            0xc491798a08a2ad4fL, 0x0e590d4546d5d81dL, // 1e238 * 2**-663
            0xf5b5d7ec8acb58a3L, 0x51ef5096988b4e24L, // 1e239 * 2**-666
            0x9991a6f3d6bf1766L, 0x5335925e1f5710d6L, // 1e240 * 2**-670
            0xbff610b0cc6edd40L, 0xe802f6f5a72cd50cL, // 1e241 * 2**-673
            0xeff394dcff8a948fL, 0x2203b4b310f80a4fL, // 1e242 * 2**-676
            0x95f83d0a1fb69cdaL, 0xb54250efea9b0671L, // 1e243 * 2**-680
            0xbb764c4ca7a44410L, 0x6292e52be541c80eL, // 1e244 * 2**-683
            0xea53df5fd18d5514L, 0x7b379e76de923a12L, // 1e245 * 2**-686
            0x92746b9be2f8552dL, 0xcd02c30a4b1b644bL, // 1e246 * 2**-690
            0xb7118682dbb66a78L, 0xc04373ccdde23d5eL, // 1e247 * 2**-693
            0xe4d5e82392a40516L, 0xf05450c0155accb5L, // 1e248 * 2**-696
            0x8f05b1163ba6832eL, 0xd634b2780d58bff1L, // 1e249 * 2**-700
            0xb2c71d5bca9023f9L, 0x8bc1df1610aeefedL, // 1e250 * 2**-703
            0xdf78e4b2bd342cf7L, 0x6eb256db94daabe9L, // 1e251 * 2**-706
            0x8bab8eefb6409c1bL, 0xe52f76493d08ab71L, // 1e252 * 2**-710
            0xae9672aba3d0c321L, 0x5e7b53db8c4ad64eL, // 1e253 * 2**-713
            0xda3c0f568cc4f3e9L, 0x361a28d26f5d8be1L, // 1e254 * 2**-716
            0x8865899617fb1872L, 0x81d05983859a776dL, // 1e255 * 2**-720
            0xaa7eebfb9df9de8eL, 0x22446fe467011548L, // 1e256 * 2**-723
            0xd51ea6fa85785632L, 0xaad58bdd80c15a9aL, // 1e257 * 2**-726
            0x8533285c936b35dfL, 0x2ac5776a7078d8a0L, // 1e258 * 2**-730
            0xa67ff273b8460357L, 0x7576d5450c970ec8L, // 1e259 * 2**-733
            0xd01fef10a657842dL, 0xd2d48a964fbcd27aL, // 1e260 * 2**-736
            0x8213f56a67f6b29cL, 0x63c4d69df1d6038cL, // 1e261 * 2**-740
            0xa298f2c501f45f43L, 0x7cb60c456e4b8470L, // 1e262 * 2**-743
            0xcb3f2f7642717714L, 0xdbe38f56c9de658cL, // 1e263 * 2**-746
            0xfe0efb53d30dd4d8L, 0x12dc732c7c55feefL, // 1e264 * 2**-749
            0x9ec95d1463e8a507L, 0x0bc9c7fbcdb5bf55L, // 1e265 * 2**-753
            0xc67bb4597ce2ce49L, 0x4ebc39fac1232f2aL, // 1e266 * 2**-756
            0xf81aa16fdc1b81dbL, 0x226b4879716bfaf5L, // 1e267 * 2**-759
            0x9b10a4e5e9913129L, 0x35830d4be6e37cd9L, // 1e268 * 2**-763
            0xc1d4ce1f63f57d73L, 0x02e3d09ee09c5c0fL, // 1e269 * 2**-766
            0xf24a01a73cf2dcd0L, 0x439cc4c698c37313L, // 1e270 * 2**-769
            0x976e41088617ca02L, 0x2a41fafc1f7a27ecL, // 1e271 * 2**-773
            0xbd49d14aa79dbc83L, 0xb4d279bb2758b1e7L, // 1e272 * 2**-776
            0xec9c459d51852ba3L, 0x22071829f12ede61L, // 1e273 * 2**-779
            0x93e1ab8252f33b46L, 0x35446f1a36bd4afcL, // 1e274 * 2**-783
            0xb8da1662e7b00a18L, 0xc2958ae0c46c9dbcL, // 1e275 * 2**-786
            0xe7109bfba19c0c9eL, 0xf33aed98f587c52bL, // 1e276 * 2**-789
            0x906a617d450187e3L, 0xd804d47f9974db3aL, // 1e277 * 2**-793
            0xb484f9dc9641e9dbL, 0x4e06099f7fd21209L, // 1e278 * 2**-796
            0xe1a63853bbd26452L, 0xa1878c075fc6968cL, // 1e279 * 2**-799
            0x8d07e33455637eb3L, 0x24f4b7849bdc1e17L, // 1e280 * 2**-803
            0xb049dc016abc5e60L, 0x6e31e565c2d3259dL, // 1e281 * 2**-806
            0xdc5c5301c56b75f8L, 0x89be5ebf3387ef04L, // 1e282 * 2**-809
            0x89b9b3e11b6329bbL, 0x5616fb378034f562L, // 1e283 * 2**-813
            0xac2820d9623bf42aL, 0xab9cba05604232bbL, // 1e284 * 2**-816
            0xd732290fbacaf134L, 0x5683e886b852bf6aL, // 1e285 * 2**-819
            0x867f59a9d4bed6c1L, 0xb61271543333b7a2L, // 1e286 * 2**-823
            0xa81f301449ee8c71L, 0xa3970da94000a58bL, // 1e287 * 2**-826
            0xd226fc195c6a2f8dL, 0x8c7cd1139000ceeeL, // 1e288 * 2**-829
            0x83585d8fd9c25db8L, 0x37ce02ac3a008154L, // 1e289 * 2**-833
            0xa42e74f3d032f526L, 0x45c183574880a1aaL, // 1e290 * 2**-836
            0xcd3a1230c43fb270L, 0xd731e42d1aa0ca14L, // 1e291 * 2**-839
            0x80444b5e7aa7cf86L, 0x867f2e9c30a47e4cL, // 1e292 * 2**-843
            0xa0555e361951c367L, 0x281efa433ccd9de0L, // 1e293 * 2**-846
            0xc86ab5c39fa63441L, 0x7226b8d40c010558L, // 1e294 * 2**-849
            0xfa856334878fc151L, 0x4eb067090f0146aeL, // 1e295 * 2**-852
            0x9c935e00d4b9d8d3L, 0x912e4065a960cc2cL, // 1e296 * 2**-856
            0xc3b8358109e84f08L, 0xf579d07f13b8ff37L, // 1e297 * 2**-859
            0xf4a642e14c6262c9L, 0x32d8449ed8a73f05L, // 1e298 * 2**-862
            0x98e7e9cccfbd7dbeL, 0x7fc72ae347688763L, // 1e299 * 2**-866
            0xbf21e44003acdd2dL, 0x1fb8f59c1942a93cL, // 1e300 * 2**-869
            0xeeea5d5004981479L, 0xe7a733031f93538bL, // 1e301 * 2**-872
            0x95527a5202df0cccL, 0xf0c87fe1f3bc1437L, // 1e302 * 2**-876
            0xbaa718e68396cffeL, 0x2cfa9fda70ab1945L, // 1e303 * 2**-879
            0xe950df20247c83feL, 0xb83947d10cd5df96L, // 1e304 * 2**-882
            0x91d28b7416cdd27fL, 0xb323cce2a805abbeL, // 1e305 * 2**-886
            0xb6472e511c81471eL, 0x1fecc01b520716adL, // 1e306 * 2**-889
            0xe3d8f9e563a198e6L, 0xa7e7f0222688dc59L, // 1e307 * 2**-892
            0x8e679c2f5e44ff90L, 0xa8f0f615581589b7L, // 1e308 * 2**-896
            0xb201833b35d63f74L, 0xd32d339aae1aec25L, // 1e309 * 2**-899
            0xde81e40a034bcf50L, 0x07f8808159a1a72eL, // 1e310 * 2**-902
            0x8b112e86420f6192L, 0x04fb5050d805087dL, // 1e311 * 2**-906
            0xadd57a27d29339f7L, 0x863a24650e064a9cL, // 1e312 * 2**-909
            0xd94ad8b1c7380875L, 0xe7c8ad7e5187dd43L, // 1e313 * 2**-912
            0x87cec76f1c830549L, 0x70dd6c6ef2f4ea4aL, // 1e314 * 2**-916
            0xa9c2794ae3a3c69bL, 0x4d14c78aafb224ddL, // 1e315 * 2**-919
            0xd433179d9c8cb842L, 0xa059f96d5b9eae14L, // 1e316 * 2**-922
            0x849feec281d7f329L, 0x24383be459432cccL, // 1e317 * 2**-926
            0xa5c7ea73224deff4L, 0xed464add6f93f7ffL, // 1e318 * 2**-929
            0xcf39e50feae16bf0L, 0x2897dd94cb78f5ffL, // 1e319 * 2**-932
            0x81842f29f2cce376L, 0x195eea7cff2b99bfL, // 1e320 * 2**-936
            0xa1e53af46f801c54L, 0x9fb6a51c3ef6802fL, // 1e321 * 2**-939
            0xca5e89b18b602369L, 0xc7a44e634eb4203bL, // 1e322 * 2**-942
            0xfcf62c1dee382c43L, 0xb98d61fc2261284aL, // 1e323 * 2**-945
            0x9e19db92b4e31baaL, 0x93f85d3d957cb92eL, // 1e324 * 2**-949
            0xc5a05277621be294L, 0x38f6748cfadbe77aL, // 1e325 * 2**-952
            0xf70867153aa2db39L, 0x473411b03992e158L, // 1e326 * 2**-955
            0x9a65406d44a5c904L, 0x8c808b0e23fbccd7L, // 1e327 * 2**-959
            0xc0fe908895cf3b45L, 0xafa0add1acfac00dL, // 1e328 * 2**-962
            0xf13e34aabb430a16L, 0x9b88d94618397010L, // 1e329 * 2**-965
            0x96c6e0eab509e64eL, 0xa13587cbcf23e60aL, // 1e330 * 2**-969
            0xbc789925624c5fe1L, 0x4982e9bec2ecdf8dL, // 1e331 * 2**-972
            0xeb96bf6ebadf77d9L, 0x1be3a42e73a81770L, // 1e332 * 2**-975
            0x933e37a534cbaae8L, 0x716e469d08490ea6L, // 1e333 * 2**-979
            0xb80dc58e81fe95a2L, 0x8dc9d8444a5b524fL, // 1e334 * 2**-982
            0xe61136f2227e3b0aL, 0x313c4e555cf226e3L, // 1e335 * 2**-985
            0x8fcac257558ee4e7L, 0xdec5b0f55a17584eL, // 1e336 * 2**-989
            0xb3bd72ed2af29e20L, 0x56771d32b09d2e62L, // 1e337 * 2**-992
            0xe0accfa875af45a8L, 0x6c14e47f5cc479faL, // 1e338 * 2**-995
            0x8c6c01c9498d8b89L, 0x438d0ecf99facc3cL, // 1e339 * 2**-999
            0xaf87023b9bf0ee6bL, 0x1470528380797f4bL, // 1e340 * 2**-1002
            0xdb68c2ca82ed2a06L, 0x598c67246097df1eL, // 1e341 * 2**-1005
            0x892179be91d43a44L, 0x77f7c076bc5eeb73L, // 1e342 * 2**-1009
            0xab69d82e364948d5L, 0x95f5b0946b76a64fL, // 1e343 * 2**-1012
            0xd6444e39c3db9b0aL, 0x7b731cb986544fe3L, // 1e344 * 2**-1015
            0x85eab0e41a6940e6L, 0x0d27f1f3f3f4b1eeL, // 1e345 * 2**-1019
            0xa7655d1d21039120L, 0x9071ee70f0f1de6aL, // 1e346 * 2**-1022
            0xd13eb46469447568L, 0xb48e6a0d2d2e5604L, // 1e347 * 2**-1025
    };
    //endregion
    private static final int FLOAT_BITS = 32;
    private static final int FLOAT_MANT_BITS = 23;
    private static final int FLOAT_EXP_BITS = 8;
    private static final int FLOAT_BIAS = -127;
    private static final int FLOAT_MIN_EXP = -189;
    private static final int DOUBLE_BITS = 64;
    private static final int DOUBLE_MANT_BITS = 52;
    private static final int DOUBLE_EXP_BITS = 11;
    private static final int DOUBLE_BIAS = -1023;
    private static final int DOUBLE_MIN_EXP = -1085;
    private static final int MIN_SCI_EXP = -3;
    private static final int MAX_SCI_EXP = 7; // align with jdk format

    // string to integer constants
    private static final byte[] ZERO_NINE_TABLE = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];

    // float to integer constants
    private static final int STRTOD_MAX_INTEGER_DIGITS = 19;
    private static final int STRTOD_MAX_EXP_DIGITS = 9;

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        for (int i = 1; i <= 31; i++) {
            int maxNum = (1 << (32 - i)) - 1;
            int v = String.valueOf(maxNum).getBytes(StandardCharsets.US_ASCII).length;
            INT_LEN_TABLE[i] = v;
        }
        for (int i = 1; i <= 63; i++) {
            long maxNum = (1L << (64 - i)) - 1;
            int v = String.valueOf(maxNum).getBytes(StandardCharsets.US_ASCII).length;
            LONG_LEN_TABLE[i] = v;
        }
        for (int i = 2; i <= 10; i++) {
            INT_POW_TABLE[i] = Math.powExact(10, i - 1);
        }
        for (int i = 2; i <= 19; i++) {
            LONG_POW_TABLE[i] = Math.powExact(10L, i - 1);
        }
        Arrays.fill(ZERO_NINE_TABLE, Byte.MAX_VALUE);
        for(byte b = BYTE_ZERO; b <= BYTE_NINE; b++) {
            ZERO_NINE_TABLE[b] = (byte) (BYTE_ZERO - b);
        }
    }

    private MarshallUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * using jdk string conversion to write integer number to writeBuffer, slower but guaranteed to be safe
     */

    public static void writeInt0(int number, WriteBuffer writeBuffer) {
        String s = Integer.toString(number);
        writeBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    public static void writeLong0(long number, WriteBuffer writeBuffer) {
        String s = Long.toString(number);
        writeBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * write 1 digit to writeBuffer each time, significantly faster than jdk string version
     */

    public static void writeInt1(int number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_INT_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0;
        int sum = number < 0 ? number : -number;
        while (sum != 0) {
            int i = sum % 10;
            buffer[--index] = (byte) (BYTE_ZERO - i);
            sum = sum / 10;
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    public static void writeLong1(long number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_LONG_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0L;
        long sum = number < 0L ? number : -number;
        while (sum != 0) {
            int i = (int) (sum % 10L);
            buffer[--index] = (byte) (BYTE_ZERO - i);
            sum = sum / 10L;
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    /**
     * using lookup table to write 2 digits to writeBuffer each time, slightly faster than the 1 digit version
     */

    public static void writeInt2(int number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_INT_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0;
        if (!negative) {
            number = -number;
        }
        int q;
        int c;
        while (number <= -100) {
            q = number / 100;
            index = index - 2;
            c = ((q * 100) - number) << 1;
            number = q;
            System.arraycopy(ITOA_BYTES, c, buffer, index, 2);
        }
        if (number <= -10) {
            index = index - 2;
            c = (-number) << 1;
            System.arraycopy(ITOA_BYTES, c, buffer, index, 2);
        } else {
            buffer[--index] = (byte) (BYTE_ZERO - number);
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    public static void writeLong2(long number, WriteBuffer writeBuffer) {
        if (number == 0L) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_LONG_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0L;
        if (!negative) {
            number = -number;
        }
        long q;
        int c;
        while (number <= -100L) {
            q = number / 100L;
            index = index - 2;
            c = (int) (((q * 100L) - number) << 1);
            number = q;
            System.arraycopy(ITOA_BYTES, c, buffer, index, 2);
        }
        if (number <= -10L) {
            index = index - 2;
            c = (int) ((-number) << 1);
            System.arraycopy(ITOA_BYTES, c, buffer, index, 2);
        } else {
            buffer[--index] = (byte) (BYTE_ZERO - number);
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    private static int digitCount(int n) {
        int leadingZeros = Integer.numberOfLeadingZeros(n);
        assert leadingZeros >= 1 && leadingZeros <= 31;
        int count = INT_LEN_TABLE[leadingZeros];
        assert count >= 1 && count <= MIN_INT_BYTES.length - 1;
        if (n < INT_POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    private static int digitCount(long n) {
        int leadingZeros = Long.numberOfLeadingZeros(n);
        assert leadingZeros >= 1 && leadingZeros <= 63;
        int count = LONG_LEN_TABLE[leadingZeros];
        assert count >= 1 && count <= MIN_LONG_BYTES.length - 1;
        if (n < LONG_POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    /**
     * using lookup table to write 2 digits to writeBuffer each time, using write-through strategy to implement manual loop-unrolling
     * using in-place byte assignment to avoid allocation and memcpy, slightly faster than the standard 2 digit version
     */

    public static void writeInt(int value, WriteBuffer writeBuffer) {
        if (value == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        if (value == Integer.MIN_VALUE) {
            writeBuffer.writeBytes(MIN_INT_BYTES);
            return;
        }
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeIntToHeapWriteBuffer(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> writeIntToSegmentWriteBuffer(value, segmentWriteBuffer);
        }
    }

    public static void writeLong(long value, WriteBuffer writeBuffer) {
        if (value instanceof int intValue) {
            writeInt(intValue, writeBuffer);
            return;
        }
        if (value == 0L) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        if (value == Long.MIN_VALUE) {
            writeBuffer.writeBytes(MIN_LONG_BYTES);
            return;
        }
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeLongToHeapWriteBuffer(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> writeLongToSegmentWriteBuffer(value, segmentWriteBuffer);
        }
    }

    private static void writeIntToHeapWriteBuffer(int value, HeapWriteBuffer heapWriteBuffer) {
        int position = heapWriteBuffer.intPosition();
        byte[] bytes = heapWriteBuffer.rawByteArray();
        int digitCount;
        if (value < 0) {
            value = -value;
            digitCount = digitCount(value);
            heapWriteBuffer.ensureCapacity(digitCount + 1); // no overflow
            bytes[position++] = BYTE_MINUS;
        } else {
            digitCount = digitCount(value);
            heapWriteBuffer.ensureCapacity(digitCount);
        }
        position = writePositiveIntToBytes(value, bytes, position, digitCount);
        heapWriteBuffer.setPosition(position);
    }

    private static int writePositiveIntToBytes(int value, byte[] bytes, int position, int digitCount) {
        int v;
        switch (digitCount) {
            case 10:
                v = value / 100000000;
                position = cpb2(v, bytes, position);
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                position = cpb2(v, bytes, position);
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                position = cpb2(v, bytes, position);
                value -= v * 10000;
            case 4:
                v = value / 100;
                position = cpb2(v, bytes, position);
                value -= v * 100;
            case 2:
                position = cpb2(value, bytes, position);
                break;
            case 9:
                v = value / 10000000;
                position = cpb2(v, bytes, position);
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                position = cpb2(v, bytes, position);
                value -= v * 100000;
            case 5:
                v = value / 1000;
                position = cpb2(v, bytes, position);
                value -= v * 1000;
            case 3:
                v = value / 10;
                position = cpb2(v, bytes, position);
                value -= v * 10;
            case 1:
                position = cpb1(value, bytes, position);
        }
        return position;
    }

    private static void writeIntToSegmentWriteBuffer(int value, SegmentWriteBuffer segmentWriteBuffer) {
        long position = segmentWriteBuffer.longPosition();
        MemorySegment segment = segmentWriteBuffer.rawSegment();
        int digitCount;
        if (value < 0) {
            value = -value;
            digitCount = digitCount(value);
            segmentWriteBuffer.ensureCapacity(digitCount + 1); // no overflow
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
        } else {
            digitCount = digitCount(value);
            segmentWriteBuffer.ensureCapacity(digitCount);
        }
        position = writePositiveIntToSegment(value, segment, position, digitCount);
        segmentWriteBuffer.setPosition(position);
    }

    private static long writePositiveIntToSegment(int value, MemorySegment segment, long position, int digitCount) {
        int v;
        switch (digitCount) {
            case 10:
                v = value / 100000000;
                position = cpm2(v, segment, position);
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                position = cpm2(v, segment, position);
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                position = cpm2(v, segment, position);
                value -= v * 10000;
            case 4:
                v = value / 100;
                position = cpm2(v, segment, position);
                value -= v * 100;
            case 2:
                position = cpm2(value, segment, position);
                break;
            case 9:
                v = value / 10000000;
                position = cpm2(v, segment, position);
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                position = cpm2(v, segment, position);
                value -= v * 100000;
            case 5:
                v = value / 1000;
                position = cpm2(v, segment, position);
                value -= v * 1000;
            case 3:
                v = value / 10;
                position = cpm2(v, segment, position);
                value -= v * 10;
            case 1:
                position = cpm1(value, segment, position);
        }
        return position;
    }

    private static void writeLongToHeapWriteBuffer(long value, HeapWriteBuffer heapWriteBuffer) {
        int position = heapWriteBuffer.intPosition();
        byte[] bytes = heapWriteBuffer.rawByteArray();
        int digitCount;
        if (value < 0) {
            value = -value;
            digitCount = digitCount(value);
            heapWriteBuffer.ensureCapacity(digitCount + 1); // no overflow
            bytes[position++] = BYTE_MINUS;
        } else {
            digitCount = digitCount(value);
            heapWriteBuffer.ensureCapacity(digitCount);
        }
        position = writePositiveLongToBytes(value, bytes, position, digitCount);
        heapWriteBuffer.setPosition(position);
    }

    private static int writePositiveLongToBytes(long value, byte[] bytes, int position, int digitCount) {
        int v;
        switch (digitCount) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                position = cpb2(v, bytes, position);
                value -= v * 100L;
            case 2:
                position = cpb2((int) value, bytes, position);
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                position = cpb2(v, bytes, position);
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                position = cpb2(v, bytes, position);
                value -= v * 10L;
            case 1:
                position = cpb1((int) value, bytes, position);
        }
        return position;
    }

    private static void writeLongToSegmentWriteBuffer(long value, SegmentWriteBuffer segmentWriteBuffer) {
        long position = segmentWriteBuffer.longPosition();
        MemorySegment segment = segmentWriteBuffer.rawSegment();
        int digitCount;
        if (value < 0) {
            value = -value;
            digitCount = digitCount(value);
            segmentWriteBuffer.ensureCapacity(digitCount + 1); // no overflow
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
        } else {
            digitCount = digitCount(value);
            segmentWriteBuffer.ensureCapacity(digitCount);
        }
        position = writePositiveLongToSegment(value, segment, position, digitCount);
        segmentWriteBuffer.setPosition(position);
    }

    private static long writePositiveLongToSegment(long value, MemorySegment segment, long position, int digitCount) {
        int v;
        switch (digitCount) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                position = cpm2(v, segment, position);
                value -= v * 100L;
            case 2:
                position = cpm2((int) value, segment, position);
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                position = cpm2(v, segment, position);
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                position = cpm2(v, segment, position);
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                position = cpm2(v, segment, position);
                value -= v * 10L;
            case 1:
                position = cpm1((int) value, segment, position);
        }
        return position;
    }

    private static int cpb2(int value, byte[] bytes, int index) {
        System.arraycopy(ITOA_BYTES, value << 1, bytes, index, 2);
        return index + 2; // no overflow
    }

    private static int cpb1(int value, byte[] bytes, int index) {
        bytes[index] = (byte) (BYTE_ZERO + value);
        return index + 1; // no overflow
    }

    private static long cpm2(int value, MemorySegment seg, long index) {
        MemorySegment.copy(ITOA_SEG, (long) value << 1, seg, index, 2);
        return index + 2; // no overflow
    }

    private static long cpm1(int value, MemorySegment seg, long index) {
        SegmentAccess.setByte(seg, index, (byte) (BYTE_ZERO + value));
        return index + 1; // no overflow
    }

    // no overflow
    private static FpRep unpack(long b, int mantBits, int expBits, int bias) {
        int exp = (int) ((b >>> mantBits) & ((1L << expBits) - 1));
        long mant = b & ((1L << mantBits) - 1);
        if(exp == 0) {
            exp++;
        } else {
            mant |= (1L << mantBits);
        }
        exp += bias;
        int s = Long.numberOfLeadingZeros(mant);
        return new FpRep(mant << s, exp - s - mantBits);
    }

    private static long packDoubleBits(long m, int e) {
        if ((m & (1L << DOUBLE_MANT_BITS)) != 0L) {
            m = (m & ~(1L << DOUBLE_MANT_BITS)) | (((long) (DOUBLE_MANT_BITS - DOUBLE_BIAS + e)) << DOUBLE_MANT_BITS);
        }
        return m;
    }

    private static int packFloatBits(long m, int e) {
        int im = Math.toIntExact(m);
        if ((im & (1 << FLOAT_MANT_BITS)) != 0) {
            im = (im & ~(1 << FLOAT_MANT_BITS)) | ((FLOAT_MANT_BITS - FLOAT_BIAS + e) << FLOAT_MANT_BITS);
        }
        return im;
    }

    // no overflow
    private static long ufloor(long u) {
        return (u) >>> 2;
    }

    // no overflow
    private static long uceil(long u) {
        return (u + 3L) >>> 2;
    }

    // no overflow
    private static long unudge(long u, int d) {
        return u + d;
    }

    // no overflow
    private static long uround(long u) {
        return (u + 1L + ((u >>> 2) & 1L)) >>> 2;
    }
    
    // no overflow
    private static long umin(long u) {
        return (u << 2) - 2L;
    }

    // safe overflow
    private static int log10Pow2(int x) {
        return (x * 78913) >> 18; // x * log₁₀2
    }

    // safe overflow
    private static int log2Pow10(int x) {
        return (x * 108853) >> 15; // x * log₂10
    }

    // safe overflow
    private static int skewed(int e) {
        return (e * 631305 - 261663) >> 21; // ⌊log₁₀ 3/4 * 2**p⌋
    }

    // no overflow
    private static Scalers prescale(int e, int p, int lp) {
        assert p >= POW10MIN && p <= POW10MAX;
        int s = -(e + lp + 3);
        assert s >= 0 && s < 64;
        int idx = (p - POW10MIN) << 1;
        long pmHi = POW10TAB[idx];
        long pmLo = POW10TAB[idx + 1];
        return new Scalers(pmHi, pmLo, s);
    }

    // no overflow
    private static long uscale(long x, Scalers c) {
        long hi = Math.unsignedMultiplyHigh(x, c.pmHi());
        long mid1 = x * c.pmHi();
        long sticky = 1L;
        if ((hi & ((1L << c.s()) - 1L)) == 0L) {
            long mid2 = Math.unsignedMultiplyHigh(x, c.pmLo());
            sticky = Long.compareUnsigned(mid1 - mid2, 1L) > 0 ? 1L : 0L;
            if (Long.compareUnsigned(mid1, mid2) < 0) {
                hi -= 1L;
            }
        }
        return (hi >>> c.s()) | sticky;
    }

    // safe overflow
    private static FpRep trimZeros(long x, int p) {
        long d;
        // cut 1 zero, or else return.
        d = Long.rotateRight(x * DIV_1_E_1_M, 1);
        if (Long.compareUnsigned(d, DIV_1_E_1_LE) > 0) {
            return new FpRep(x, p);
        }
        x = d;
        p += 1;
        // Cut 8 zeros, then 4, then 2, then 1.
        d = Long.rotateRight(x * DIV_1_E_8_M, 8);
        if (Long.compareUnsigned(d, DIV_1_E_8_LE) <= 0) {
            x = d;
            p += 8;
        }
        d = Long.rotateRight(x * DIV_1_E_4_M, 4);
        if (Long.compareUnsigned(d, DIV_1_E_4_LE) <= 0) {
            x = d;
            p += 4;
        }
        d = Long.rotateRight(x * DIV_1_E_2_M, 2);
        if (Long.compareUnsigned(d, DIV_1_E_2_LE) <= 0) {
            x = d;
            p += 2;
        }
        d = Long.rotateRight(x * DIV_1_E_1_M, 1);
        if (Long.compareUnsigned(d, DIV_1_E_1_LE) <= 0) {
            x = d;
            p += 1;
        }
        return new FpRep(x, p);
    }

    // no overflow
    private static FpRep transform(FpRep r, int mantBits, int minExp) {
        long m = r.d();
        int e = r.e();
        int p;
        long min;
        int z = 63 - mantBits;
        if (m == (1L << 63) && e > minExp) {
            p = -skewed(e + z);
            min = m - (1L << (z - 2));
        } else {
            if (e < minExp) {
                z += (minExp - e);
            }
            p = -log10Pow2(e + z);
            min = m - (1L << (z - 1));
        }
        long max = m + (1L << (z - 1));
        int odd = (int) (m >>> z) & 1;
        Scalers pre = prescale(e, p, log2Pow10(p));
        long dmin = uceil(unudge(uscale(min, pre), odd));
        long dmax = ufloor(unudge(uscale(max, pre), -odd));
        long d0 = Long.divideUnsigned(dmax, 10L) * 10L;
        if (Long.compareUnsigned(d0, dmin) >= 0) {
            return trimZeros(Long.divideUnsigned(dmax, 10L), -(p - 1));
        }
        long d = dmin;
        if (Long.compareUnsigned(d, dmax) < 0) {
            d = uround(uscale(m, pre));
        }
        return new FpRep(d, -p);
    }

    public static void writeFloat(float f, WriteBuffer writeBuffer) {
        if(!Float.isFinite(f)) {
            throw new IllegalArgumentException("nan and infinite float are not supported");
        }
        int bits = Float.floatToRawIntBits(f);
        boolean negative = (bits >>> 31) == 1;
        if((bits & 0x7FFFFFFF) == 0) {
            if(negative) {
                writeBuffer.writeShort(NEG_ZERO);
            } else {
                writeBuffer.writeByte(BYTE_ZERO);
            }
            return ;
        }
        writeBuffer.ensureCapacity(MAX_FLOAT_CAPACITY);
        if(negative) {
            bits &= ~(1 << 31);
            writeBuffer.writeByte(BYTE_MINUS);
        }
        FpRep r = transform(unpack(bits, FLOAT_MANT_BITS, FLOAT_EXP_BITS, FLOAT_BIAS), FLOAT_MANT_BITS, FLOAT_MIN_EXP);
        long d = r.d();
        int e = r.e();
        int digitCount = digitCount(d);
        int scientificExp = e + digitCount - 1;
        assert scientificExp >= MIN_FLOAT_E && scientificExp <= MAX_FLOAT_E;
        writeFpToWriteBuffer(d, e, digitCount, scientificExp, writeBuffer);
    }

    public static void writeDouble(double f, WriteBuffer writeBuffer) {
        if(!Double.isFinite(f)) {
            throw new IllegalArgumentException("nan and infinite double are not supported");
        }
        long bits = Double.doubleToRawLongBits(f);
        boolean negative = (bits >>> 63) == 1L;
        if((bits & 0x7FFFFFFFFFFFFFFFL) == 0L) {
            if(negative) {
                writeBuffer.writeShort(NEG_ZERO);
            } else {
                writeBuffer.writeByte(BYTE_ZERO);
            }
            return ;
        }
        writeBuffer.ensureCapacity(MAX_DOUBLE_CAPACITY);
        if(negative) {
            bits &= ~(1L << 63);
            writeBuffer.writeByte(BYTE_MINUS);
        }
        FpRep r = transform(unpack(bits, DOUBLE_MANT_BITS, DOUBLE_EXP_BITS, DOUBLE_BIAS), DOUBLE_MANT_BITS, DOUBLE_MIN_EXP);
        long d = r.d();
        int e = r.e();
        int digitCount = digitCount(d);
        int scientificExp = e + digitCount - 1;
        assert scientificExp >= MIN_DOUBLE_E && scientificExp <= MAX_DOUBLE_E;
        writeFpToWriteBuffer(d, e, digitCount, scientificExp, writeBuffer);
    }

    private static void writeFpToWriteBuffer(long d, int e, int digitCount, int scientificExp, WriteBuffer writeBuffer) {
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> {
                int position = heapWriteBuffer.intPosition();
                byte[] bytes = heapWriteBuffer.rawByteArray();
                if(scientificExp >= MIN_SCI_EXP && scientificExp < MAX_SCI_EXP) {
                    position = writeFixedDoubleToBytes(d, e, bytes, position, digitCount);
                } else {
                    position = writeScientificDoubleToBytes(d, bytes, position, digitCount, scientificExp);
                }
                heapWriteBuffer.setPosition(position);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                long position = segmentWriteBuffer.longPosition();
                MemorySegment segment = segmentWriteBuffer.rawSegment();
                if(scientificExp >= MIN_SCI_EXP && scientificExp < MAX_SCI_EXP) {
                    position = writeFixedDoubleToSegment(d, e, segment, position, digitCount);
                } else {
                    position = writeScientificDoubleToSegment(d, segment, position, digitCount, scientificExp);
                }
                segmentWriteBuffer.setPosition(position);
            }
        }
    }

    private static int writeFixedDoubleToBytes(long d, int e, byte[] bytes, int position, int digitCount) {
        int newPosition;
        if(e >= 0) {
            position = writePositiveLongToBytes(d, bytes, position, digitCount);
            if(e > 0) {
                newPosition = position + e;
                Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                position = newPosition;
            }
        } else {
            int fracDigits = -e;
            if(fracDigits >= digitCount) {
                newPosition = position + 2;
                ArrayAccess.setShort(bytes, position, ZERO_PERIOD);
                position = newPosition;
                int leadingZeros = fracDigits - digitCount;
                if(leadingZeros > 0) {
                    newPosition = position + leadingZeros;
                    Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                    position = newPosition;
                }
                position = writePositiveLongToBytes(d, bytes, position, digitCount);
            } else {
                int dotPosition = position + (digitCount - fracDigits);
                newPosition = writePositiveLongToBytes(d, bytes, position, digitCount);
                System.arraycopy(bytes, dotPosition, bytes, dotPosition + 1, fracDigits);
                bytes[dotPosition] = BYTE_PERIOD;
                position = newPosition + 1;
            }
        }
        return position;
    }

    private static int writeScientificDoubleToBytes(long d, byte[] bytes, int position, int digitCount, int scientificExp) {
        if(digitCount > 1) {
            int startPosition = position + 1;
            int endPosition = writePositiveLongToBytes(d, bytes, startPosition, digitCount);
            bytes[position] = bytes[startPosition];
            bytes[startPosition] = BYTE_PERIOD;
            position = endPosition;
        } else {
            bytes[position++] = (byte) (BYTE_ZERO + d);
        }
        bytes[position++] = BYTE_E;
        if(scientificExp < 0) {
            bytes[position++] = BYTE_MINUS;
            scientificExp = -scientificExp;
        }
        int expDigitCount = digitCount(scientificExp);
        position = writePositiveIntToBytes(scientificExp, bytes, position, expDigitCount);
        return position;
    }

    private static long writeFixedDoubleToSegment(long d, int e, MemorySegment segment, long position, int digitCount) {
        long newPosition;
        if(e >= 0) {
            position = writePositiveLongToSegment(d, segment, position, digitCount);
            if(e > 0) {
                newPosition = position + e;
                segment.asSlice(position, e).fill(BYTE_ZERO);
                position = newPosition;
            }
        } else {
            int fracDigits = -e;
            if(fracDigits >= digitCount) {
                newPosition = position + 2;
                SegmentAccess.setShort(segment, position, ZERO_PERIOD);
                position = newPosition;
                int leadingZeros = fracDigits - digitCount;
                if(leadingZeros > 0) {
                    newPosition = position + leadingZeros;
                    segment.asSlice(position, leadingZeros).fill(BYTE_ZERO);
                    position = newPosition;
                }
                position = writePositiveLongToSegment(d, segment, position, digitCount);
            } else {
                long dotPosition = position + (digitCount - fracDigits);
                newPosition = writePositiveLongToSegment(d, segment, position, digitCount);
                MemorySegment.copy(segment, dotPosition, segment, dotPosition + 1L, fracDigits);
                SegmentAccess.setByte(segment, dotPosition, BYTE_PERIOD);
                position = newPosition + 1L;
            }
        }
        return position;
    }

    private static long writeScientificDoubleToSegment(long d, MemorySegment segment, long position, int digitCount, int scientificExp) {
        if(digitCount > 1) {
            long startPosition = position + 1L;
            long endPosition = writePositiveLongToSegment(d, segment, startPosition, digitCount);
            SegmentAccess.setByte(segment, position, SegmentAccess.getByte(segment, startPosition));
            SegmentAccess.setByte(segment, startPosition, BYTE_PERIOD);
            position = endPosition;
        } else {
            SegmentAccess.setByte(segment, position++, (byte) (BYTE_ZERO + d));
        }
        SegmentAccess.setByte(segment, position++, BYTE_E);
        if(scientificExp < 0) {
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            scientificExp = -scientificExp;
        }
        int expDigitCount = digitCount(scientificExp);
        position = writePositiveIntToSegment(scientificExp, segment, position, expDigitCount);
        return position;
    }

    public static byte readByte(ReadBuffer readBuffer) {
        long v = readLong(readBuffer);
        if(v instanceof byte r) {
            return r;
        }
        throw new ArithmeticException("byte overflow");
    }

    public static short readShort(ReadBuffer readBuffer) {
        long v = readLong(readBuffer);
        if(v instanceof short r) {
            return r;
        }
        throw new ArithmeticException("short overflow");
    }

    public static char readChar(ReadBuffer readBuffer) {
        long v = readLong(readBuffer);
        if(v instanceof char r) {
            return r;
        }
        throw new ArithmeticException("short overflow");
    }

    public static int readInt(ReadBuffer readBuffer) {
        long v = readLong(readBuffer);
        if(v instanceof int i) {
            return i;
        }
        throw new ArithmeticException("int overflow");
    }

    public static long readLong(ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readLongFromHeapReadBuffer(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> readLongFromSegmentReadBuffer(segmentReadBuffer);
        };
    }

    private static long readLongFromHeapReadBuffer(HeapReadBuffer heapReadBuffer) {
        int position = heapReadBuffer.intPosition();
        if(position == heapReadBuffer.intLength()) {
            throw new NumberFormatException("empty buffer");
        }
        byte[] bytes = heapReadBuffer.rawByteArray();
        boolean negative = false;
        long r;
        byte firstByte = bytes[position++];
        if(firstByte == BYTE_MINUS) {
            negative = true;
            if(position == bytes.length) {
                throw new NumberFormatException("empty buffer");
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])];
            if(v >= 0) {
                throw new NumberFormatException("illegal negative value : " + v);
            }
            r = v;
            position++;
        } else if(firstByte == BYTE_ZERO) {
            if(position < bytes.length && ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])] <= 0) {
                throw new NumberFormatException("leading zero");
            }
            return 0L;
        } else {
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
            if(v > 0) {
                throw new NumberFormatException("illegal value : " + v);
            }
            r = v;
        }
        while (position < bytes.length) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])];
            if(b <= 0) {
                r = Math.addExact(Math.multiplyExact(r, 10L), b);
                position++;
            } else {
                heapReadBuffer.setPosition(position);
                break ;
            }
        }
        if(negative) {
            return r;
        }
        if(r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -r;
    }

    private static long readLongFromSegmentReadBuffer(SegmentReadBuffer segmentReadBuffer) {
        int position = segmentReadBuffer.intPosition();
        int len = segmentReadBuffer.intLength();
        if(position == len) {
            throw new NumberFormatException("empty buffer");
        }
        MemorySegment segment = segmentReadBuffer.rawSegment();
        boolean negative = false;
        long r;
        byte firstByte = SegmentAccess.getByte(segment, position++);
        if(firstByte == BYTE_MINUS) {
            negative = true;
            if(position == len) {
                throw new NumberFormatException("empty buffer");
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
            if(v >= 0) {
                throw new NumberFormatException("empty buffer");
            }
            r = v;
            position++;
        } else if(firstByte == BYTE_ZERO) {
            if(position < len && ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))] <= 0) {
                throw new NumberFormatException("leading zero");
            }
            return 0L;
        } else {
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
            if(v > 0) {
                throw new NumberFormatException("illegal value : " + v);
            }
            r = v;
        }
        while (position < len) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
            if(b <= 0) {
                r = Math.addExact(Math.multiplyExact(r, 10L), b);
                position++;
            } else {
                segmentReadBuffer.setPosition(position);
                break ;
            }
        }
        if(negative) {
            return r;
        }
        if(r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -r;
    }

    /**
     * floating-point parsing and printing algorithms are based on Russ Cox's
     * "Floating-Point Printing and Parsing Can Be Simple And Fast".
     *
     * @see <a href="https://research.swtch.com/fp-all">research.swtch.com/fp-all</a>
     */
    
    public static float readFloat(ReadBuffer readBuffer) {
        FpFormat fpFormat = parseFpFormat(readBuffer);
        if(fpFormatFallbackRequired(fpFormat, MAX_FLOAT_E)) {
            return readFloatFallback(readBuffer, fpFormat);
        }
        long d = fpFormat.d();
        int p = fpFormat.p();
        int lp = log2Pow10(p);
        int shift = Long.numberOfLeadingZeros(d);
        int b = 64 - shift;
        int e = Math.min(FLOAT_MANT_BITS - FLOAT_BIAS - 1, FLOAT_MANT_BITS + 1 - b - lp);
        long u = uscale(d << shift, prescale(e - shift, p, lp));
        if(u >= umin(1L << (FLOAT_MANT_BITS + 1))) {
            u = (u >>> 1) | (u & 1);
            e--;
        }
        int r = packFloatBits(uround(u), -e);
        if(fpFormat.negative()) {
            r |= 1 << (FLOAT_MANT_BITS + FLOAT_EXP_BITS);
        }
        return Float.intBitsToFloat(r);
    }

    private static float readFloatFallback(ReadBuffer readBuffer, FpFormat fpFormat) {
        int len = fpFormat.len();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                String s = new String(bytes, position, len, StandardCharsets.US_ASCII);
                heapReadBuffer.setPosition(position + len);
                return Float.parseFloat(s);
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long position = segmentReadBuffer.longPosition();
                byte[] bytes = segment.asSlice(position, len).toArray(ValueLayout.JAVA_BYTE);
                String s = new String(bytes, StandardCharsets.US_ASCII);
                segmentReadBuffer.setPosition(position + len);
                return Float.parseFloat(s);
            }
        }
    }

    // no overflow
    public static double readDouble(ReadBuffer readBuffer) {
        FpFormat fpFormat = parseFpFormat(readBuffer);
        if(fpFormatFallbackRequired(fpFormat, MAX_DOUBLE_E)) {
            return readDoubleFallback(readBuffer, fpFormat);
        }
        long d = fpFormat.d();
        int p = fpFormat.p();
        int lp = log2Pow10(p);
        int shift = Long.numberOfLeadingZeros(d);
        int b = 64 - shift;
        int e = Math.min(DOUBLE_MANT_BITS - DOUBLE_BIAS - 1, DOUBLE_MANT_BITS + 1 - b - lp);
        long u = uscale(d << shift, prescale(e - shift, p, lp));
        if(u >= umin(1L << (DOUBLE_MANT_BITS + 1))) {
            u = (u >>> 1) | (u & 1);
            e--;
        }
        long r = packDoubleBits(uround(u), -e);
        if(fpFormat.negative()) {
            r |= 1L << (DOUBLE_MANT_BITS + DOUBLE_EXP_BITS);
        }
        return Double.longBitsToDouble(r);
    }

    private static double readDoubleFallback(ReadBuffer readBuffer, FpFormat fpFormat) {
        int len = fpFormat.len();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                String s = new String(bytes, position, len, StandardCharsets.US_ASCII);
                heapReadBuffer.setPosition(position + len);
                return Double.parseDouble(s);
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long postion = segmentReadBuffer.longPosition();
                byte[] bytes = segment.asSlice(postion, len).toArray(ValueLayout.JAVA_BYTE);
                String s = new String(bytes, StandardCharsets.US_ASCII);
                segmentReadBuffer.setPosition(postion + len);
                return Double.parseDouble(s);
            }
        }
    }

    private static boolean fpFormatFallbackRequired(FpFormat fpFormat, int maxExponent) {
        // If the length of the exponent part already exceeds the int range,
        // it means the value of p has overflowed and is invalid, so a fallback is needed.
        if(fpFormat.pLen() > STRTOD_MAX_EXP_DIGITS) {
            return true;
        }
        // The long type can only represent decimal numbers with up to STRTOD_MAX_INTEGER_DIGITS significant digits.
        // For numbers exceeding this limit, a fallback is needed.
        int digitCount = Math.addExact(fpFormat.dLen(), fpFormat.frac());
        if(digitCount > STRTOD_MAX_INTEGER_DIGITS) {
            return true;
        }
        // Values < 10^(digitCount + exp) <= 10^(maxExponent) < (max fp value) can be guaranteed not to cause arithmetic overflow.
        return Math.addExact(digitCount, fpFormat.p()) > maxExponent;
    }

    private static FpFormat parseFpFormat(ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseFpFormatFromHeapReadBuffer(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> parseFpFormatFromSegmentReadBuffer(segmentReadBuffer);
        };
    }

    enum ParsingState {
        FIRST_INTEGER,
        MORE_INTEGER,
        FIRST_FRACTION,
        MORE_FRACTION,
        FIRST_EXPONENT,
        MORE_EXPONENT,
        END
    }

    private static FpFormat parseFpFormatFromHeapReadBuffer(HeapReadBuffer heapReadBuffer) {
        int position = heapReadBuffer.intPosition();
        if(position == heapReadBuffer.intLength()) {
            throw new NumberFormatException("empty buffer");
        }
        byte[] bytes = heapReadBuffer.rawByteArray();
        int index = position;
        boolean negative = false;
        boolean negativeExponent = false;
        long d = Long.MIN_VALUE;
        int dLen = 0;
        int frac = 0;
        int p = 0;
        int pLen = 0;
        ParsingState parsingState = ParsingState.FIRST_INTEGER;
        loop:
        for (; index < bytes.length; index++) {
            byte target = bytes[index];
            switch (parsingState) {
                case FIRST_INTEGER -> {
                    if (target == BYTE_MINUS) {
                        negative = true;
                    } else {
                        byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                        if (v <= 0) {
                            d = -v;
                            dLen = Math.incrementExact(dLen);
                            parsingState = ParsingState.MORE_INTEGER;
                        } else {
                            throw new NumberFormatException("not a digit : " + target);
                        }
                    }
                }
                case MORE_INTEGER -> {
                    if (target == BYTE_PERIOD) {
                        parsingState = ParsingState.FIRST_FRACTION;
                    } else if (target == BYTE_E || target == BYTE_e) {
                        parsingState = ParsingState.FIRST_EXPONENT;
                    } else {
                        byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                        if (v <= 0) {
                            if(d == 0L) {
                                throw new NumberFormatException("leading zero");
                            }
                            d = d * 10L - v;
                            dLen = Math.incrementExact(dLen);
                        } else {
                            break loop;
                        }
                    }
                }
                case FIRST_FRACTION -> {
                    byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                    if (v <= 0) {
                        d = d * 10L - v;
                        frac = Math.incrementExact(frac);
                        parsingState = ParsingState.MORE_FRACTION;
                    } else {
                        throw new NumberFormatException("not a digit : " + target);
                    }
                }
                case MORE_FRACTION -> {
                    if(target == BYTE_E || target == BYTE_e) {
                        parsingState = ParsingState.FIRST_EXPONENT;
                    } else {
                        byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                        if (v <= 0) {
                            d = d * 10L - v;
                            frac = Math.incrementExact(frac);
                        } else {
                            throw new NumberFormatException("not a digit : " + target);
                        }
                    }
                }
                case FIRST_EXPONENT -> {
                    if(target == BYTE_MINUS) {
                        negativeExponent = true;
                    } else if(target != BYTE_PLUS) {
                        byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                        if (v <= 0) {
                            p = -v;
                            pLen = Math.incrementExact(pLen);
                            parsingState = ParsingState.MORE_EXPONENT;
                        } else {
                            throw new NumberFormatException("not a digit : " + target);
                        }
                    }
                }
                case MORE_EXPONENT -> {
                    byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                    if(v <= 0) {
                        p = 10 * p - v;
                        pLen = Math.incrementExact(pLen);
                    } else {
                        break loop;
                    }
                }
            }
        }
        p = Math.subtractExact(negativeExponent ? -p : p, frac);
        return new FpFormat(negative, d, dLen, frac, p, pLen, index - position);
    }

    private static FpFormat parseFpFormatFromSegmentReadBuffer(SegmentReadBuffer segmentReadBuffer) {
        int position = segmentReadBuffer.intPosition();
        int length = segmentReadBuffer.intLength();
        if(position == length) {
            throw new NumberFormatException("empty buffer");
        }
        MemorySegment segment = segmentReadBuffer.rawSegment();
        int index = position;
        boolean negative = false;
        boolean negativeExponent = false;
        long d = 0L;
        int dLen = 0;
        int frac = 0;
        int p = 0;
        int pLen = 0;
        ParsingState parsingState = ParsingState.FIRST_INTEGER;
        loop:
        for (; index < length; index++) {
            byte target = SegmentAccess.getByte(segment, index);
            switch (parsingState) {
                case FIRST_INTEGER -> {
                    if (target == BYTE_MINUS) {
                        negative = true;
                    } else {
                        byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                        if (v <= 0) {
                            d = -v;
                            dLen = Math.incrementExact(dLen);
                            parsingState = ParsingState.MORE_INTEGER;
                        } else {
                            throw new NumberFormatException("not a digit : " + target);
                        }
                    }
                }
                case MORE_INTEGER -> {
                    if (target == BYTE_PERIOD) {
                        parsingState = ParsingState.FIRST_FRACTION;
                    } else if (target == BYTE_E || target == BYTE_e) {
                        parsingState = ParsingState.FIRST_EXPONENT;
                    } else {
                        byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                        if (v <= 0) {
                            if(d == 0L) {
                                throw new NumberFormatException("leading zero");
                            }
                            d = d * 10L - v;
                            dLen = Math.incrementExact(dLen);
                        } else {
                            break loop;
                        }
                    }
                }
                case FIRST_FRACTION -> {
                    byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                    if (v <= 0) {
                        d = d * 10L - v;
                        frac = Math.incrementExact(frac);
                        parsingState = ParsingState.MORE_FRACTION;
                    } else {
                        throw new NumberFormatException("not a digit : " + target);
                    }
                }
                case MORE_FRACTION -> {
                    if(target == BYTE_E || target == BYTE_e) {
                        parsingState = ParsingState.FIRST_EXPONENT;
                    } else {
                        byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                        if (v <= 0) {
                            d = d * 10L - v;
                            frac = Math.incrementExact(frac);
                        } else {
                            throw new NumberFormatException("not a digit : " + target);
                        }
                    }
                }
                case FIRST_EXPONENT -> {
                    if(target == BYTE_MINUS) {
                        negativeExponent = true;
                    } else if(target != BYTE_PLUS) {
                        byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                        if (v <= 0) {
                            p = -v;
                            pLen = Math.incrementExact(pLen);
                            parsingState = ParsingState.MORE_EXPONENT;
                        } else {
                            throw new NumberFormatException("not a digit : " + target);
                        }
                    }
                }
                case MORE_EXPONENT -> {
                    byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                    if(v <= 0) {
                        p = 10 * p - v;
                        pLen = Math.incrementExact(pLen);
                    } else {
                        break loop;
                    }
                }
            }
        }
        p = Math.subtractExact(negativeExponent ? -p : p, frac);
        return new FpFormat(negative, d, dLen, frac, p, pLen, index - position);
    }

}
