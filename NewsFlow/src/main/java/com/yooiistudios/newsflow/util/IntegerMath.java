package com.yooiistudios.newsflow.util;

/**
 * Created by Dongheyon Jeong in News-Android-L from Yooii Studios Co., LTD. on 15. 3. 3.
 *
 * IntegerMath
 *  Integer 에 관한 계산
 */
public class IntegerMath {
    private IntegerMath() { throw new AssertionError(""); }

    public static int getLargestInteger(int... ints) {
        if (ints.length <= 0) {
            throw new IndexOutOfBoundsException(
                    "Parameter MUST contain more than or equal to 1 value.");
        }

        int largestInteger = -1;
        for (int value : ints) {
            largestInteger = value > largestInteger ? value : largestInteger;
        }

        return largestInteger;
    }
}
