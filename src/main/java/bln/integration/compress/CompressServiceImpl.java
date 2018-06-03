package bln.integration.compress;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@Service
public class CompressServiceImpl implements CompressService {

    @Override
    public byte[] compress(byte[] data) throws Exception {
        byte[] sizeInBytes = toBytes(data.length);

        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length+4);
        outputStream.write(sizeInBytes);

        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        outputStream.flush();

        return outputStream.toByteArray();
    }


    @Override
    public byte[] decompress(byte[] data) throws Exception {
        byte[] sizeInBytes = new byte[4];
        sizeInBytes[0]=data[0];
        sizeInBytes[1]=data[1];
        sizeInBytes[2]=data[2];
        sizeInBytes[3]=data[3];
        int origSize = toInt(sizeInBytes);

        Inflater inflater = new Inflater();
        inflater.setInput(data, 4, data.length-4);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(origSize);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        outputStream.flush();

        return outputStream.toByteArray();
    }

    private byte[] toBytes(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i >> 0 );

        ArrayUtils.reverse(result);
        return result;
    }

    public int toInt(byte[] b) {
        ArrayUtils.reverse(b);
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }
}
