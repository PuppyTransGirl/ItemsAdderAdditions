package toutouchien.itemsadderadditions.feature.behaviour;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.behaviour.annotation.Behaviour;

import static org.junit.jupiter.api.Assertions.*;

class BehaviourExecutorTest {
    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void keyComesFromAnnotation() {
        assertEquals("sample", new SampleBehaviour().key());
    }

    @Test
    void missingAnnotationKeyThrows() {
        assertThrows(IllegalStateException.class, () -> new MissingAnnotationBehaviour().key());
    }

    @Test
    void newInstanceCreatesFreshBehaviour() {
        SampleBehaviour original = new SampleBehaviour();
        BehaviourExecutor copy = original.newInstance();

        assertInstanceOf(SampleBehaviour.class, copy);
        assertNotSame(original, copy);
    }

    @Test
    void newInstanceWithInaccessibleConstructorThrows() {
        assertThrows(IllegalStateException.class, () -> new PrivateConstructorBehaviour().newInstance());
    }

    @Test
    void configureSectionInjectsParameters() {
        SampleBehaviour behaviour = new SampleBehaviour();

        assertTrue(behaviour.configure(yamlOf("amount: 3\n"), "ns:item"));
        assertEquals(3, behaviour.amount);
    }

    @Test
    void configureNonSectionFails() {
        assertFalse(new SampleBehaviour().configure("wrong", "ns:item"));
    }

    @Test
    void loadStoresHostAndCallsOnLoad() {
        SampleBehaviour behaviour = new SampleBehaviour();
        BehaviourHost host = new BehaviourHost("ns:item", ItemCategory.ITEM, null);

        behaviour.load(host);

        assertSame(host, behaviour.host());
        assertEquals(1, behaviour.loadCalls);
        assertSame(host, behaviour.loadedHost);
    }

    @Test
    void unloadCallsOnUnloadAndClearsHost() {
        SampleBehaviour behaviour = new SampleBehaviour();
        BehaviourHost host = new BehaviourHost("ns:item", ItemCategory.ITEM, null);
        behaviour.load(host);

        behaviour.unload();

        assertNull(behaviour.host());
        assertEquals(1, behaviour.unloadCalls);
        assertSame(host, behaviour.loadedHost);
    }

    @Test
    void unloadWithoutHostIsNoOp() {
        SampleBehaviour behaviour = new SampleBehaviour();

        behaviour.unload();

        assertEquals(0, behaviour.unloadCalls);
        assertNull(behaviour.host());
    }

    @Behaviour(key = "sample")
    public static class SampleBehaviour extends BehaviourExecutor {
        int loadCalls;
        int unloadCalls;
        BehaviourHost loadedHost;

        @Parameter(key = "amount", type = Integer.class, required = true, min = 1, max = 5)
        int amount;

        @Override
        protected void onLoad(BehaviourHost host) {
            loadCalls++;
            loadedHost = host;
        }

        @Override
        protected void onUnload(BehaviourHost host) {
            unloadCalls++;
            loadedHost = host;
        }
    }

    @Behaviour(key = "private_ctor")
    public static class PrivateConstructorBehaviour extends BehaviourExecutor {
        private PrivateConstructorBehaviour() {
        }

        @Override
        protected void onLoad(BehaviourHost host) {
        }

        @Override
        protected void onUnload(BehaviourHost host) {
        }
    }

    public static class MissingAnnotationBehaviour extends BehaviourExecutor {
        @Override
        protected void onLoad(BehaviourHost host) {
        }

        @Override
        protected void onUnload(BehaviourHost host) {
        }
    }
}
