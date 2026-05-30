package io.jingproject.marshall;

// 浮点数的二进制表现形式，m * 2^e
public record BinaryFp(long m, int e) {

}
