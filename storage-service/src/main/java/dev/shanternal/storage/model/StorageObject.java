package dev.shanternal.storage.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public record StorageObject(
        InputStream content,
        StorageMetadata metadata
) implements Closeable {

    @Override
    public void close() throws IOException {
        content.close();
    }
}
