package lavabuckets;

import xaos.api.actions.ActionResult;
import xaos.api.definitions.DefinitionId;
import xaos.api.fluids.FluidView;
import xaos.api.mods.ModContext;
import xaos.api.mods.TownsMod;
import xaos.api.production.ProductionResult;
import xaos.api.world.WorldPosition;

/**
 * Open Towns Lava Buckets example mod.
 *
 * Demonstrates:
 * - custom item/action definitions
 * - production
 * - UI actions
 * - area selection
 * - world positions
 * - fluid inspection
 * - queued world actions
 * - lifecycle-managed UI registrations
 */
public final class LavaBucketsMod
        implements TownsMod {

    private static final String MOD_ID =
            "lava-buckets";

    private static final DefinitionId LAVA_BUCKET =
            DefinitionId.of(
                    MOD_ID,
                    "lavabucket"
            );

    private static final DefinitionId MAKE_LAVA_BUCKET =
            DefinitionId.of(
                    MOD_ID,
                    "qlavabucket"
            );

    private static final DefinitionId DUMP_LAVA =
            DefinitionId.of(
                    MOD_ID,
                    "qdumplava"
            );

    @Override
    public void onLoad(
            ModContext context
    ) {
        context.logger().info(
                "Lava Buckets loaded!"
        );
    }

    @Override
    public void onWorldLoaded(
            ModContext context
    ) {
        registerMenuActions(
                context
        );

        context.api()
                .ui()
                .showMessage(
                        "Lava Buckets loaded."
                );
    }

    private void registerMenuActions(
            ModContext context
    ) {

        /*
         * UI actions are world-scoped because they are only useful
         * while a playable world is active.
         *
         * Open Towns automatically unregisters them when the world
         * is unloaded.
         */
        context.worldResource(
                context.api()
                        .ui()
                        .registerGameAction(
                                MOD_ID
                                        + ":make-bucket",
                                "Create Lava Bucket",
                                () -> queueLavaBucket(
                                        context
                                )
                        )
        );

        context.worldResource(
                context.api()
                        .ui()
                        .registerGameAction(
                                MOD_ID
                                        + ":dump-lava",
                                "Dump Lava",
                                () -> startLavaPlacement(
                                        context
                                )
                        )
        );
    }

    /**
     * Queue production of a single lava bucket.
     */
    private void queueLavaBucket(
            ModContext context
    ) {

        ProductionResult result =
                context.api()
                        .production()
                        .queue(
                                MAKE_LAVA_BUCKET,
                                1
                        );

        if (result.acceptedAnything()) {

            context.api()
                    .ui()
                    .showMessage(
                            "Lava bucket queued."
                    );

        } else {

            context.api()
                    .ui()
                    .showMessage(
                            "Could not queue lava bucket: "
                                    + result.message()
                    );
        }
    }

    /**
     * Begin selecting an area in which lava should be placed.
     */
    private void startLavaPlacement(
            ModContext context
    ) {

        int availableBuckets =
                context.api()
                        .items()
                        .count(
                                LAVA_BUCKET
                        );

        if (availableBuckets <= 0) {

            context.api()
                    .ui()
                    .showMessage(
                            "No lava buckets are available."
                    );

            return;
        }

        context.api()
                .ui()
                .showMessage(
                        "Dump Lava: select an area. "
                                + availableBuckets
                                + " lava bucket(s) available."
                );

        context.api()
                .selection()
                .selectArea(
                        selection -> queueLavaArea(
                                context,
                                selection.origin(),
                                selection.width(),
                                selection.height()
                        )
                );
    }

    /**
     * Queue one lava-placement action per selected tile.
     *
     * One lava bucket represents one tile of lava.
     */
    private void queueLavaArea(
            ModContext context,
            WorldPosition origin,
            int width,
            int height
    ) {

        /*
         * Recheck inventory after area selection because the
         * player's inventory may have changed in the meantime.
         */
        int availableBuckets =
                context.api()
                        .items()
                        .count(
                                LAVA_BUCKET
                        );

        int accepted = 0;
        int rejected = 0;
        int skipped = 0;

        outer:
        for (int y = origin.y();
                y < origin.y() + height;
                y++) {

            for (int x = origin.x();
                    x < origin.x() + width;
                    x++) {

                /*
                 * One bucket can only create one lava tile.
                 */
                if (accepted >= availableBuckets) {
                    break outer;
                }

                WorldPosition position =
                        new WorldPosition(
                                x,
                                y,
                                origin.z()
                        );

                /*
                 * Avoid deliberately creating jobs on cells
                 * that already contain fluid.
                 */
                FluidView existing =
                        context.api()
                                .fluids()
                                .at(
                                        position
                                );

                if (existing != null
                        && !existing.empty()) {

                    skipped++;

                    continue;
                }

                ActionResult result =
                        context.api()
                                .actions()
                                .queueAt(
                                        DUMP_LAVA,
                                        position
                                );

                if (result.accepted()) {

                    accepted++;

                } else {

                    rejected++;
                }
            }
        }

        context.api()
                .ui()
                .showMessage(
                        "Lava jobs created: "
                                + accepted
                                + ", rejected: "
                                + rejected
                                + ", skipped: "
                                + skipped
                );
    }

    @Override
    public void onWorldUnloaded(
            ModContext context
    ) {
        context.logger().info(
                "Lava Buckets world unloaded."
        );
    }

    @Override
    public void onUnload(
            ModContext context
    ) {
        context.logger().info(
                "Lava Buckets unloaded."
        );
    }
}