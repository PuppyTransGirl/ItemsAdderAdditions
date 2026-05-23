package toutouchien.itemsadderadditions.runtime.reload;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReloadPlanTest {
    private static ReloadableContentSystem stub(String name, ReloadPhase phase) {
        return new ReloadableContentSystem() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public ReloadPhase phase() {
                return phase;
            }

            @Override
            public ReloadStepResult reload(ContentReloadContext context) {
                return ReloadStepResult.loaded(name, 1);
            }
        };
    }

    @Test
    void emptyPlanReturnsEmptyResults() {
        ReloadPlan plan = new ReloadPlan(List.of());
        List<ReloadStepResult> results = plan.run(null);
        assertTrue(results.isEmpty());
    }

    @Test
    void singleSystemRunsAndReturnsResult() {
        ReloadPlan plan = new ReloadPlan(List.of(stub("A", ReloadPhase.CONTENT_FILES)));
        List<ReloadStepResult> results = plan.run(null);
        assertEquals(1, results.size());
        assertEquals("A", results.get(0).system());
    }

    @Test
    void systemsRunInPhaseOrder() {
        List<String> runOrder = new ArrayList<>();

        ReloadableContentSystem late = new ReloadableContentSystem() {
            @Override
            public String name() {
                return "Late";
            }

            @Override
            public ReloadPhase phase() {
                return ReloadPhase.POST_CONTENT;
            }

            @Override
            public ReloadStepResult reload(ContentReloadContext ctx) {
                runOrder.add("Late");
                return ReloadStepResult.unchanged("Late");
            }
        };
        ReloadableContentSystem early = new ReloadableContentSystem() {
            @Override
            public String name() {
                return "Early";
            }

            @Override
            public ReloadPhase phase() {
                return ReloadPhase.REGISTRY_PREPARE;
            }

            @Override
            public ReloadStepResult reload(ContentReloadContext ctx) {
                runOrder.add("Early");
                return ReloadStepResult.unchanged("Early");
            }
        };

        new ReloadPlan(List.of(late, early)).run(null);

        assertEquals(List.of("Early", "Late"), runOrder);
    }

    @Test
    void resultsListIsImmutable() {
        ReloadPlan plan = new ReloadPlan(List.of(stub("A", ReloadPhase.CONTENT_FILES)));
        List<ReloadStepResult> results = plan.run(null);
        assertThrows(UnsupportedOperationException.class, () -> results.add(ReloadStepResult.unchanged("X")));
    }

    @Test
    void resultCountMatchesSystemCount() {
        ReloadPlan plan = new ReloadPlan(List.of(
                stub("A", ReloadPhase.REGISTRY_PREPARE),
                stub("B", ReloadPhase.CONTENT_FILES),
                stub("C", ReloadPhase.POST_CONTENT)
        ));
        assertEquals(3, plan.run(null).size());
    }

    @Test
    void allPhasesCanBeUsedInOrder() {
        List<String> runOrder = new ArrayList<>();
        List<ReloadableContentSystem> systems = new ArrayList<>();
        ReloadPhase[] phases = ReloadPhase.values();
        for (int i = phases.length - 1; i >= 0; i--) {
            ReloadPhase phase = phases[i];
            systems.add(new ReloadableContentSystem() {
                @Override
                public String name() {
                    return phase.name();
                }

                @Override
                public ReloadPhase phase() {
                    return phase;
                }

                @Override
                public ReloadStepResult reload(ContentReloadContext ctx) {
                    runOrder.add(phase.name());
                    return ReloadStepResult.unchanged(phase.name());
                }
            });
        }

        new ReloadPlan(systems).run(null);

        for (int i = 0; i < phases.length - 1; i++) {
            int ai = runOrder.indexOf(phases[i].name());
            int bi = runOrder.indexOf(phases[i + 1].name());
            assertTrue(ai < bi, phases[i] + " must run before " + phases[i + 1]);
        }
    }
}
