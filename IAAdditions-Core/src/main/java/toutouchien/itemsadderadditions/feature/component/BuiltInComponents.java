package toutouchien.itemsadderadditions.feature.component;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.component.builtin.*;

import java.util.List;

/**
 * Built-in component prototypes registered by this plugin.
 */
@NullMarked
final class BuiltInComponents {
    private BuiltInComponents() {
    }

    static List<ComponentExecutor> create() {
        return List.of(
                new AttackRangeComponent(),
                new BannerPatternsComponent(),
                new BlocksAttacksComponent(),
                new BaseColorComponent(),
                new BundleContentsComponent(),
                new CanBreakComponent(),
                new CanPlaceOnComponent(),
                new ChargedProjectilesComponent(),
                new DamageResistantComponent(),
                new DamageTypeComponent(),
                new DeathProtectionComponent(),
                new DyedColorComponent(),
                new EnchantableComponent(),
                new FireworkExplosionComponent(),
                new FireworksComponent(),
                new GliderComponent(),
                new IntangibleProjectileComponent(),
                new KineticWeaponComponent(),
                new LodestoneTrackerComponent(),
                new MapColorComponent(),
                new MapDecorationsComponent(),
                new MapIdComponent(),
                new MinimumAttackChargeComponent(),
                new OminousBottleAmplifierComponent(),
                new PiercingWeaponComponent(),
                new PotDecorationsComponent(),
                new PotionContentsComponent(),
                new PotionDurationScaleComponent(),
                new ProfileComponent(),
                new ProvidesBannerPatternsComponent(),
                new ProvidesTrimMaterialComponent(),
                new RarityComponent(),
                new RepairableComponent(),
                new StoredEnchantmentsComponent(),
                new SuspiciousStewEffectsComponent(),
                new SwingAnimationComponent(),
                new ToolComponent(),
                new TooltipDisplayComponent(),
                new UseCooldownComponent(),
                new UseRemainderComponent(),
                new WeaponComponent(),
                new WritableBookContentComponent()
        );
    }
}
