package com.fang123.common;

import java.util.Base64;

/**
 * ID 混淆工具：将数字 ID 编码为短字符串，防止 URL 遍历
 */
public class IdObfuscator {

    private static final long SALT = 0x2A3B5C7D9E1F4B6DL;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public static String encode(Long id) {
        if (id == null) return "";
        long v = id ^ SALT;
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[7 - i] = (byte) (v & 0xFF);
            v >>= 8;
        }
        return ENCODER.encodeToString(bytes);
    }

    public static Long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        byte[] bytes = DECODER.decode(encoded);
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (bytes[i] & 0xFF);
        }
        return v ^ SALT;
    }
}
