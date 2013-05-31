package com.maxmind.maxminddb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DecoderTest {

    private static Map<Integer, byte[]> int32() {
        int max = (2 << 30) - 1;
        HashMap<Integer, byte[]> int32 = new HashMap<Integer, byte[]>();

        int32.put(Integer.valueOf(0), new byte[] { 0b00000000, 0b00000001 });
        int32.put(Integer.valueOf(-1), new byte[] { 0b00000100, 0b00000001,
                (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111,
                (byte) 0b11111111 });
        int32.put(Integer.valueOf((2 << 7) - 1), new byte[] { 0b00000001,
                0b00000001, (byte) 0b11111111 });
        int32.put(Integer.valueOf(1 - (2 << 7)), new byte[] { 0b00000100,
                0b00000001, (byte) 0b11111111, (byte) 0b11111111,
                (byte) 0b11111111, 0b00000001 });
        int32.put(Integer.valueOf(500), new byte[] { 0b00000010, 0b00000001,
                0b00000001, (byte) 0b11110100 });

        int32.put(Integer.valueOf(-500), new byte[] { 0b00000100, 0b00000001,
                (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111110,
                0b00001100 });

        int32.put(Integer.valueOf((2 << 15) - 1), new byte[] { 0b00000010,
                0b00000001, (byte) 0b11111111, (byte) 0b11111111 });
        int32.put(Integer.valueOf(1 - (2 << 15)), new byte[] { 0b00000100,
                0b00000001, (byte) 0b11111111, (byte) 0b11111111, 0b00000000,
                0b00000001 });
        int32.put(Integer.valueOf((2 << 23) - 1), new byte[] { 0b00000011,
                0b00000001, (byte) 0b11111111, (byte) 0b11111111,
                (byte) 0b11111111 });
        int32.put(Integer.valueOf(1 - (2 << 23)), new byte[] { 0b00000100,
                0b00000001, (byte) 0b11111111, 0b00000000, 0b00000000,
                0b00000001 });
        int32.put(Integer.valueOf(max), new byte[] { 0b00000100, 0b00000001,
                0b01111111, (byte) 0b11111111, (byte) 0b11111111,
                (byte) 0b11111111 });
        int32.put(Integer.valueOf(-max), new byte[] { 0b00000100, 0b00000001,
                (byte) 0b10000000, 0b00000000, 0b00000000, 0b00000001 });
        return int32;
    }

    private static Map<Long, byte[]> uint32() {
        long max = (((long) 1) << 32) - 1;
        HashMap<Long, byte[]> uint32s = new HashMap<Long, byte[]>();

        uint32s.put(Long.valueOf(0), new byte[] { (byte) 0b11000000 });
        uint32s.put(Long.valueOf((1 << 8) - 1), new byte[] { (byte) 0b11000001,
                (byte) 0b11111111 });
        uint32s.put(Long.valueOf(500), new byte[] { (byte) 0b11000010,
                0b00000001, (byte) 0b11110100 });
        uint32s.put(Long.valueOf(10872), new byte[] { (byte) 0b11000010,
                0b00101010, 0b01111000 });
        uint32s.put(Long.valueOf((1 << 16) - 1), new byte[] {
                (byte) 0b11000010, (byte) 0b11111111, (byte) 0b11111111 });
        uint32s.put(Long.valueOf((1 << 24) - 1), new byte[] {
                (byte) 0b11000011, (byte) 0b11111111, (byte) 0b11111111,
                (byte) 0b11111111 });
        uint32s.put(max, new byte[] { (byte) 0b11000100, (byte) 0b11111111,
                (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111111 });

        return uint32s;
    }

    @SuppressWarnings("boxing")
    private static Map<Integer, byte[]> uint16() {
        int max = (1 << 16) - 1;

        Map<Integer, byte[]> uint16s = new HashMap<Integer, byte[]>();

        uint16s.put(0, new byte[] { (byte) 0b10100000 });
        uint16s.put((1 << 8) - 1, new byte[] { (byte) 0b10100001,
                (byte) 0b11111111 });
        uint16s.put(500, new byte[] { (byte) 0b10100010, 0b00000001,
                (byte) 0b11110100 });
        uint16s.put(10872, new byte[] { (byte) 0b10100010, 0b00101010,
                0b01111000 });
        uint16s.put(max, new byte[] { (byte) 0b10100010, (byte) 0b11111111,
                (byte) 0b11111111 });
        return uint16s;
    }

    private static Map<BigInteger, byte[]> largeUint(int bits) {
        Map<BigInteger, byte[]> uints = new HashMap<BigInteger, byte[]>();

        byte ctrlByte = (byte) (bits == 64 ? 0b00000010 : 0b00000011);

        uints.put(BigInteger.valueOf(0), new byte[] { 0b00000000, ctrlByte });
        uints.put(BigInteger.valueOf(500), new byte[] { 0b00000010, ctrlByte,
                0b00000001, (byte) 0b11110100 });
        uints.put(BigInteger.valueOf(10872), new byte[] { 0b00000010, ctrlByte,
                0b00101010, 0b01111000 });

        for (int power = 1; power <= bits / 8; power++) {

            BigInteger key = BigInteger.valueOf(2).pow(8 * power)
                    .subtract(BigInteger.valueOf(1));

            byte[] value = new byte[2 + power];
            value[0] = (byte) power;
            value[1] = ctrlByte;
            for (int i = 2; i < value.length; i++) {
                value[i] = (byte) 0b11111111;
            }
        }
        return uints;

    }

    private static Map<Long, byte[]> pointers() {
        Map<Long, byte[]> pointers = new HashMap<Long, byte[]>();

        pointers.put(Long.valueOf(0), new byte[] { 0b00100000, 0b00000000 });
        pointers.put(Long.valueOf(5), new byte[] { 0b00100000, 0b00000101 });
        pointers.put(Long.valueOf(10), new byte[] { 0b00100000, 0b00001010 });
        pointers.put(Long.valueOf((1 << 10) - 1), new byte[] { 0b00100011,
                (byte) 0b11111111, });
        pointers.put(Long.valueOf(3017), new byte[] { 0b00101000, 0b00000011,
                (byte) 0b11001001 });
        pointers.put(Long.valueOf((1 << 19) - 5), new byte[] { 0b00101111,
                (byte) 0b11110111, (byte) 0b11111011 });
        pointers.put(Long.valueOf((1 << 19) + (1 << 11) - 1), new byte[] {
                0b00101111, (byte) 0b11111111, (byte) 0b11111111 });
        pointers.put(Long.valueOf((1 << 27) - 2), new byte[] { 0b00110111,
                (byte) 0b11110111, (byte) 0b11110111, (byte) 0b11111110 });
        pointers.put(
                Long.valueOf((((long) 1) << 27) + (1 << 19) + (1 << 11) - 1),
                new byte[] { 0b00110111, (byte) 0b11111111, (byte) 0b11111111,
                        (byte) 0b11111111 });
        pointers.put(Long.valueOf((((long) 1) << 32) - 1), new byte[] {
                0b00111000, (byte) 0b11111111, (byte) 0b11111111,
                (byte) 0b11111111, (byte) 0b11111111 });
        return pointers;
    }

    public Map<String, byte[]> strings() {
        Map<String, byte[]> strings = new HashMap<String, byte[]>();

        this.addTestString(strings, (byte) 0b01000000, "");
        this.addTestString(strings, (byte) 0b01000001, "1");
        this.addTestString(strings, (byte) 0b01000011, "人");
        this.addTestString(strings, (byte) 0b01000011, "123");
        this.addTestString(strings, (byte) 0b01011011,
                "123456789012345678901234567");
        this.addTestString(strings, (byte) 0b01011100,
                "1234567890123456789012345678");
        this.addTestString(strings, (byte) 0b01011100,
                "1234567890123456789012345678");
        this.addTestString(strings, new byte[] { 0b01011101, 0b00000000 },
                "12345678901234567890123456789");
        this.addTestString(strings, new byte[] { 0b01011101, 0b00000001 },
                "123456789012345678901234567890");

        this.addTestString(strings, new byte[] { 0b01011110, 0b00000000,
                (byte) 0b11010111 }, this.xString(500));
        this.addTestString(strings, new byte[] { 0b01011110, 0b00000110,
                (byte) 0b10110011 }, this.xString(2000));
        this.addTestString(strings, new byte[] { 0b01011111, 0b00000000,
                0b00010000, 0b01010011, }, this.xString(70000));

        return strings;

    }

    public Map<byte[], byte[]> bytes() {
        Map<byte[], byte[]> bytes = new HashMap<byte[], byte[]>();

        Map<String, byte[]> strings = this.strings();

        for (String s : strings.keySet()) {
            byte[] ba = strings.get(s);
            ba[0] ^= 0b11000000;

            bytes.put(s.getBytes(Charset.forName("UTF-8")), ba);
        }

        return bytes;
    }

    private String xString(int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append("x");
        }
        return sb.toString();
    }

    private void addTestString(Map<String, byte[]> tests, byte ctrl, String str) {
        this.addTestString(tests, new byte[] { ctrl }, str);
    }

    private void addTestString(Map<String, byte[]> tests, byte ctrl[],
            String str) {

        byte[] sb = str.getBytes(Charset.forName("UTF-8"));
        byte[] bytes = new byte[ctrl.length + sb.length];

        System.arraycopy(ctrl, 0, bytes, 0, ctrl.length);
        System.arraycopy(sb, 0, bytes, ctrl.length, sb.length);
        tests.put(str, bytes);
    }

    public Map<Boolean, byte[]> booleans() {
        Map<Boolean, byte[]> booleans = new HashMap<Boolean, byte[]>();

        booleans.put(Boolean.FALSE, new byte[] { 0b00000000, 0b00000111 });
        booleans.put(Boolean.TRUE, new byte[] { 0b00000001, 0b00000111 });
        return booleans;
    }

    public Map<Map<String, Object>, byte[]> maps() {
        Map<Map<String, Object>, byte[]> maps = new HashMap<Map<String, Object>, byte[]>();

        Map<String, Object> empty = new HashMap<String, Object>();
        maps.put(empty, new byte[] { (byte) 0b11100000 });

        Map<String, Object> one = new HashMap<String, Object>();
        one.put("en", "Foo");
        maps.put(one, new byte[] { (byte) 0b11100001, /* en */0b01000010,
                0b01100101, 0b01101110,
                /* Foo */0b01000011, 0b01000110, 0b01101111, 0b01101111 });

        Map<String, Object> two = new HashMap<String, Object>();
        two.put("en", "Foo");
        two.put("zh", "人");
        maps.put(two, new byte[] { (byte) 0b11100010,
        /* en */
        0b01000010, 0b01100101, 0b01101110,
        /* Foo */
        0b01000011, 0b01000110, 0b01101111, 0b01101111,
        /* zh */
        0b01000010, 0b01111010, 0b01101000,
        /* 人 */
        0b01000011, (byte) 0b11100100, (byte) 0b10111010, (byte) 0b10111010 });

        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put("name", two);

        maps.put(nested, new byte[] { (byte) 0b11100001, /* name */
        0b01000100, 0b01101110, 0b01100001, 0b01101101, 0b01100101,
                (byte) 0b11100010,/* en */
                0b01000010, 0b01100101, 0b01101110,
                /* Foo */
                0b01000011, 0b01000110, 0b01101111, 0b01101111,
                /* zh */
                0b01000010, 0b01111010, 0b01101000,
                /* 人 */
                0b01000011, (byte) 0b11100100, (byte) 0b10111010,
                (byte) 0b10111010 });

        /*
         * This currently isn't working as assertEquals thinks all arrays are
         * different. Usually you would just use assertArrayEquals, but that
         * obviously doesn't work here
         */
        // Map<String, Object> guess = new HashMap<String, Object>();
        // guess.put("languages", new String[] { "en", "zh" });
        // maps.put(guess, new byte[] { (byte) 0b11100001,/* languages */
        // 0b01001001, 0b01101100, 0b01100001, 0b01101110, 0b01100111,
        // 0b01110101,
        // 0b01100001, 0b01100111, 0b01100101, 0b01110011,
        // /* array */
        // 0b00000010, 0b00000100,
        // /* en */
        // 0b01000010, 0b01100101, 0b01101110,
        // /* zh */
        // 0b01000010, 0b01111010, 0b01101000 });

        return maps;
    }

    public Map<Object[], byte[]> arrays() {
        Map<Object[], byte[]> arrays = new HashMap<Object[], byte[]>();

        arrays.put(new String[] { "Foo" }, new byte[] { 0b00000001, 0b00000100,
        /* Foo */
        0b01000011, 0b01000110, 0b01101111, 0b01101111 });

        arrays.put(new String[] { "Foo", "人" }, new byte[] { 0b00000010,
                0b00000100,
                /* Foo */
                0b01000011, 0b01000110, 0b01101111, 0b01101111,
                /* 人 */
                0b01000011, (byte) 0b11100100, (byte) 0b10111010,
                (byte) 0b10111010 });

        arrays.put(new Object[0], new byte[] { 0b00000000, 0b00000100 });

        return arrays;
    }

    @Test
    public void testUint16() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.UINT16, uint16());
    }

    @Test
    public void testUint32() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.UINT32, uint32());
    }

    @Test
    public void testInt32() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.INT32, int32());
    }

    @Test
    public void testUint64() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.UINT64, largeUint(64));
    }

    @Test
    public void testUint128() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.UINT128, largeUint(128));
    }

    @Test
    public void testPointers() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.POINTER, pointers());
    }

    @Test
    public void testStrings() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.UTF8_STRING, this.strings());
    }

    @Test
    public void testBooleans() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.BOOLEAN, this.booleans());
    }

    @Test
    public void testBytes() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.BYTES, this.bytes());
    }

    @Test
    public void testMaps() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.MAP, this.maps());
    }

    @Test
    public void testArrays() throws MaxMindDbException, IOException {
        testTypeDecoding(Type.ARRAY, this.arrays());
    }

    public static <T> void testTypeDecoding(Type type, Map<T, byte[]> tests)
            throws MaxMindDbException, IOException {

        for (Map.Entry<T, byte[]> entry : tests.entrySet()) {
            T expect = entry.getKey();
            byte[] input = entry.getValue();
            System.out.println(Arrays.toString(input));

            String desc = "decoded " + type.name() + " - " + expect;
            InputStream in = new ByteArrayInputStream(input);

            Decoder decoder = new Decoder(in, 0);
            decoder.POINTER_TEST_HACK = true;

            if (type.equals(Type.BYTES)) {
                assertArrayEquals(desc, (byte[]) expect, (byte[]) decoder
                        .decode(0).getObject());
            } else if (type.equals(Type.ARRAY)) {
                assertArrayEquals(desc, (Object[]) expect, (Object[]) decoder
                        .decode(0).getObject());
            } else {
                assertEquals(desc, expect, decoder.decode(0).getObject());
            }
        }
    }

    @Test
    public void testDecodeInt32() {
        Map<Integer, byte[]> map = int32();
        for (Map.Entry<Integer, byte[]> entry : map.entrySet()) {
            byte[] bytes = Arrays.copyOfRange(entry.getValue(), 2,
                    entry.getValue().length);
            assertEquals("decode int32: " + entry.getKey(), entry.getKey()
                    .intValue(), Decoder.decodeInt32(bytes));
        }
    }

    @Test
    public void testDecodeUint16() {
        Map<Integer, byte[]> map = uint16();
        for (Map.Entry<Integer, byte[]> entry : map.entrySet()) {
            byte[] bytes = Arrays.copyOfRange(entry.getValue(), 1,
                    entry.getValue().length);
            assertEquals("decode uint16: " + entry.getKey(), entry.getKey()
                    .intValue(), Decoder.decodeUint16(bytes));
        }
    }

    // FIXME - these tests should be combined using generics
    @Test
    public void testDecodeUint32() {
        Map<Long, byte[]> map = uint32();
        for (Map.Entry<Long, byte[]> entry : map.entrySet()) {
            byte[] bytes = Arrays.copyOfRange(entry.getValue(), 1,
                    entry.getValue().length);
            assertEquals("decode uint32: " + entry.getKey(), entry.getKey()
                    .longValue(), Decoder.decodeUint32(bytes));
        }
    }

    @Test
    public void testDecodeUint64() {
        Map<BigInteger, byte[]> map = largeUint(64);
        for (Map.Entry<BigInteger, byte[]> entry : map.entrySet()) {
            byte[] bytes = Arrays.copyOfRange(entry.getValue(), 2,
                    entry.getValue().length);
            assertEquals("decode uint64: " + entry.getKey(), entry.getKey(),
                    Decoder.decodeUint64(bytes));
        }
    }

    @Test
    public void testDecodeUint128() {
        Map<BigInteger, byte[]> map = largeUint(128);
        for (Map.Entry<BigInteger, byte[]> entry : map.entrySet()) {
            byte[] bytes = Arrays.copyOfRange(entry.getValue(), 2,
                    entry.getValue().length);
            assertEquals("decode uint128: " + entry.getKey(), entry.getKey(),
                    Decoder.decodeUint128(bytes));
        }
    }
}
