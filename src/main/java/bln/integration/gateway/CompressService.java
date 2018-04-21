package bln.integration.gateway;

public interface CompressService {
    byte[] compress(byte[] data) throws Exception;
    byte[] decompress(byte[] data) throws Exception;
}