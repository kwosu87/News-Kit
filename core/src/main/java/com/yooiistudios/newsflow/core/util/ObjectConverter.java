package com.yooiistudios.newsflow.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * Created by Dongheyon Jeong in News Flow from Yooii Studios Co., LTD. on 15. 3. 11.
 *
 * ObjectConverter
 *  Object 를 다른 객체로 변환해주는 유틸
 */
public class ObjectConverter {
    public static byte[] toByteArray(Object object) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = new ObjectOutputStream(outputStream);
        objectOutput.writeObject(object);
        byte[] bytes = outputStream.toByteArray();
        objectOutput.close();

        return bytes;
    }
}
