package toutouchien.itemsadderadditions.testsupport;

import org.junit.jupiter.api.function.Executable;
import org.mockbukkit.mockbukkit.exception.UnimplementedOperationException;

public final class MockBukkitUnsupported {
    private MockBukkitUnsupported() {
    }

    public static void failInsteadOfSkip(Executable executable) {
        try {
            executable.execute();
        } catch (UnimplementedOperationException e) {
            throw new AssertionError("MockBukkit unsupported API reached; keeping this as a real test failure", e);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
