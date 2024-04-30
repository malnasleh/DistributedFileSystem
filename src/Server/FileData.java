package Server;

import java.io.Serializable;

/**
 * Used to return data from read and write operations. Allows us to return multiple values from a method.
 * @param fileName Name of the file being downloaded.
 * @param fileData Byte array of the file being downloaded.
 * @param status Status code of the operation. Used to return an appropriate error message.
 */
public record FileData(String fileName, byte[] fileData, StatusCodeEnum status) implements Serializable {
    public enum StatusCodeEnum {
        SUCCESS,
        NO_SERVERS,
        OTHER,
        OVERWRITTEN
    }
}
