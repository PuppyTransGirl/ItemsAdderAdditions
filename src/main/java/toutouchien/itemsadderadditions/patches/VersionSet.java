package toutouchien.itemsadderadditions.patches;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class VersionSet implements VersionConstraint {
    private final Axis axis;
    private final Set<String> mcVersions; // used for MC and BOTH (normalized)
    private final Set<String> iaVersions; // used for IA and BOTH (normalized)
    private final Set<Version> pairs; // used for BOTH (normalized)

    private VersionSet(Axis axis, Set<String> mc, Set<String> ia, Set<Version> pairs) {
        this.axis = axis;
        this.mcVersions = mc;
        this.iaVersions = ia;
        this.pairs = pairs;
    }

    public static VersionSet mc(String... versions) {
        return new VersionSet(Axis.MC,
                Arrays.stream(versions)
                        .map(Version::normalize)
                        .collect(Collectors.toUnmodifiableSet()),
                Set.of(),
                Set.of());
    }

    public static VersionSet ia(String... versions) {
        return new VersionSet(Axis.IA,
                Set.of(),
                Arrays.stream(versions)
                        .map(Version::normalize)
                        .collect(Collectors.toUnmodifiableSet()),
                Set.of());
    }

    public static VersionSet of(Version... versions) {
        Set<String> mc = Arrays.stream(versions)
                .map(v -> Version.normalize(v.minecraft()))
                .collect(Collectors.toUnmodifiableSet());
        Set<String> ia = Arrays.stream(versions)
                .map(v -> Version.normalize(v.itemsAdder()))
                .collect(Collectors.toUnmodifiableSet());

        // Normalize pairs too, so (mc, ia) equality works with betas
        Set<Version> normalizedPairs = Arrays.stream(versions)
                .map(v -> Version.of(
                        Version.normalize(v.minecraft()),
                        Version.normalize(v.itemsAdder())))
                .collect(Collectors.toUnmodifiableSet());

        return new VersionSet(Axis.BOTH, mc, ia, normalizedPairs);
    }

    private static Version versionNormalized(Version version) {
        return Version.of(
                Version.normalize(version.minecraft()),
                Version.normalize(version.itemsAdder()));
    }

    @Override
    public boolean test(Version version) {
        return switch (axis) {
            case MC -> mcVersions.contains(Version.normalize(version.minecraft()));
            case IA -> iaVersions.contains(Version.normalize(version.itemsAdder()));
            case BOTH -> pairs.contains(versionNormalized(version));
        };
    }

    @Override
    public String toString() {
        return switch (axis) {
            case MC -> "VersionSet{mc=" + mcVersions + "}";
            case IA -> "VersionSet{ia=" + iaVersions + "}";
            case BOTH -> "VersionSet{pairs=" + pairs + "}";
        };
    }

    private enum Axis {MC, IA, BOTH}
}
