package toutouchien.itemsadderadditions.feature.component;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.builtin.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in component prototypes registered by this plugin.
 */
@NullMarked
final class BuiltInComponents {
    private BuiltInComponents() {
    }

    static List<ComponentExecutor> create() {
        if (VersionUtils.isLowerThan(VersionUtils.v1_21_5)) {
            return List.of();
        }

        List<ComponentExecutor> components = new ArrayList<>(List.of(
                new BannerPatternsComponent(),
                new BaseColorComponent(),
                new BundleContentsComponent(),
                new CanPlaceOnComponent(),
                new ChargedProjectilesComponent(),
                new DamageResistantComponent(),
                new DamageTypeComponent(),
                new DyedColorComponent(),
                new EnchantableComponent(),
                new FireworkExplosionComponent(),
                new FireworksComponent(),
                new IntangibleProjectileComponent(),
                new LodestoneTrackerComponent(),
                new MapColorComponent(),
                new MapDecorationsComponent(),
                new MapIdComponent(),
                new OminousBottleAmplifierComponent(),
                new PotDecorationsComponent(),
                new PotionContentsComponent(),
                new PotionDurationScaleComponent(),
                new ProfileComponent(),
                new ProvidesBannerPatternsComponent(),
                new ProvidesTrimMaterialComponent(),
                new RarityComponent(),
                new StoredEnchantmentsComponent(),
                new SuspiciousStewEffectsComponent(),
                new TooltipDisplayComponent(),
                new UseCooldownComponent(),
                new UseRemainderComponent(),
                new WritableBookContentComponent()
        ));

        if (VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_1)) {
            components.add(new MinimumAttackChargeComponent());
        }
        if (VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_3)) {
            components.add(new GliderComponent());
        }
        if (VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_5)) {
            components.add(new BlocksAttacksComponent());
            components.add(new CanBreakComponent());
            components.add(new DeathProtectionComponent());
            components.add(new KineticWeaponComponent());
            components.add(new PiercingWeaponComponent());
            components.add(new RepairableComponent());
            components.add(new ToolComponent());
            components.add(new WeaponComponent());
        }
        if (VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_11)) {
            components.add(new AttackRangeComponent());
            components.add(new SwingAnimationComponent());
        }

        return List.copyOf(components);
    }
}
