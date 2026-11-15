package diary.file.entity.model;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;

@Data
public class OssObjectResource implements AutoCloseable {

    private final InputStream inputStream;
    private final long contentLength;
    private final String contentType;

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}