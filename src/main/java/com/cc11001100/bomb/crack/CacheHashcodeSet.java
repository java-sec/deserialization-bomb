package com.cc11001100.bomb.crack;

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
