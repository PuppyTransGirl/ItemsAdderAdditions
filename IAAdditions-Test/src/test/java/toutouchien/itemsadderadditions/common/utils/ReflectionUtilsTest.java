package toutouchien.itemsadderadditions.common.utils;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.feature.component.SimpleComponentProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReflectionUtilsTest {
    @Test
    void getTypeArgumentForStringSimpleProperty() {
        assertEquals(String.class, ReflectionUtils.getTypeArgument(StringProperty.class));
    }

    @Test
    void getTypeArgumentForIntegerSimpleProperty() {
        assertEquals(Integer.class, ReflectionUtils.getTypeArgument(IntegerProperty.class));
    }

    @Test
    void getTypeArgumentForRawPropertyReturnsNull() {
        assertNull(ReflectionUtils.getTypeArgument(RawProperty.class));
    }

    @Test
    void getTypeArgumentForUnrelatedClassReturnsNull() {
        assertNull(ReflectionUtils.getTypeArgument(NotAProperty.class));
    }

    static final class StringProperty implements SimpleComponentProperty<String> {
        @Override
        public void applyComponent(String parameter, String itemID, ItemStack itemStack) {
        }
    }

    static final class IntegerProperty implements SimpleComponentProperty<Integer> {
        @Override
        public void applyComponent(Integer parameter, String itemID, ItemStack itemStack) {
        }
    }

    @SuppressWarnings("rawtypes")
    static final class RawProperty implements SimpleComponentProperty {
        @Override
        public void applyComponent(Object parameter, String itemID, ItemStack itemStack) {
        }
    }

    static final class NotAProperty {
    }
}
