package toutouchien.itemsadderadditions.integration.hook.worldguard;

import org.jspecify.annotations.NullMarked;

@NullMarked
enum WorldGuardFlagKey {
    STORAGE_OPEN("iaa-storage-open"),
    CONTACT_DAMAGE("iaa-contact-damage"),
    STACKABLE_PLACE("iaa-stackable-place"),
    BED_USE("iaa-bed-use"),
    CUSTOM_PAINTING_PLACE("iaa-custom-painting-place");

    private final String flagName;

    WorldGuardFlagKey(String flagName) {
        this.flagName = flagName;
    }

    String flagName() {
        return flagName;
    }
}
