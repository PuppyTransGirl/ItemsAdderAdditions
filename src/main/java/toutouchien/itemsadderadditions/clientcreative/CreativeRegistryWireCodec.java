package toutouchien.itemsadderadditions.clientcreative;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class CreativeRegistryWireCodec {
    private CreativeRegistryWireCodec() {
    }

    public static byte[] encode(OptimizedCreativeRegistry registry) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(output);

            data.writeInt(registry.hash());

            data.writeInt(registry.items().size());
            for (OptimizedCreativeRegistry.ItemEntry item : registry.items()) {
                writeString(data, item.namespacedId());
                writeBytes(data, item.encodedStack());
            }

            data.writeInt(registry.tabs().size());
            for (OptimizedCreativeRegistry.TabEntry tab : registry.tabs()) {
                writeString(data, tab.id());
                writeString(data, tab.title());
                data.writeInt(tab.iconIndex());

                data.writeInt(tab.itemIndexes().length);
                for (int index : tab.itemIndexes()) {
                    data.writeInt(index);
                }
            }

            data.flush();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode creative registry", exception);
        }
    }

    private static void writeString(DataOutputStream data, String value) throws IOException {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        data.writeInt(bytes.length);
        data.write(bytes);
    }

    private static void writeBytes(DataOutputStream data, byte[] bytes) throws IOException {
        data.writeInt(bytes.length);
        data.write(bytes);
    }
}
