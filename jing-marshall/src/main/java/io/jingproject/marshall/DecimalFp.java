package io.jingproject.marshall;

// 浮点数的十进制表现形式，d * 10^p
public record DecimalFp(long d, int e) {
}
