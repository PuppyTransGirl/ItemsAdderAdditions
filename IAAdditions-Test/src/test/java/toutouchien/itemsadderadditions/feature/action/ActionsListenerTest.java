package toutouchien.itemsadderadditions.feature.action;

import org.bukkit.event.Listener;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.feature.action.listener.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ActionsListenerTest {
    @Test
    void createAllReturnsEveryActionListenerType() {
        List<Listener> listeners = ActionsListener.createAll();
        Set<Class<?>> classes = listeners.stream().map(Object::getClass).collect(Collectors.toSet());

        assertEquals(7, listeners.size());
        assertEquals(Set.of(
                ComplexFurnitureActionListener.class,
                FurnitureActionListener.class,
                BlockActionListener.class,
                ItemInteractionActionListener.class,
                ItemCombatInventoryActionListener.class,
                ItemUseProjectileActionListener.class,
                MiscItemActionListener.class
        ), classes);
    }

    @Test
    void createAllReturnsFreshListenerInstances() {
        List<Listener> first = ActionsListener.createAll();
        List<Listener> second = ActionsListener.createAll();

        assertEquals(first.stream().map(Object::getClass).toList(), second.stream().map(Object::getClass).toList());
        for (int i = 0; i < first.size(); i++) {
            assertNotSame(first.get(i), second.get(i));
        }
    }

    @Test
    void returnedListIsImmutable() {
        List<Listener> listeners = ActionsListener.createAll();

        assertThrows(UnsupportedOperationException.class, listeners::clear);
    }
}
