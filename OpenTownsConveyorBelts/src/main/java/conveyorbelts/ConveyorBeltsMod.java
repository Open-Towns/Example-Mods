package conveyorbelts;

import java.util.HashSet;
import java.util.Set;

import xaos.api.definitions.DefinitionId;
import xaos.api.items.ItemBehaviourContext;
import xaos.api.items.ItemFacing;
import xaos.api.mods.ModContext;
import xaos.api.mods.TownsMod;
import xaos.api.world.WorldPosition;

/**
 * Open Towns Conveyor Belts example mod.
 *
 * Demonstrates:
 * - custom item definitions
 * - item placement
 * - item behaviours
 * - containers and inventory
 * - item reservations
 * - item transfers
 * - item facing
 * - world position queries
 * - lifecycle-managed registrations
 */
public final class ConveyorBeltsMod
        implements TownsMod {

    private static final String MOD_ID =
            "conveyor-belts";

    private static final DefinitionId CONVEYOR =
            DefinitionId.of(
                    MOD_ID,
                    "conveyor"
            );

    private static final DefinitionId BUILD_CONVEYOR =
            DefinitionId.of(
                    MOD_ID,
                    "qconveyor"
            );

    private static final int CONVEYOR_INTERVAL_TICKS =
            30;

    /*
     * Prevent an item that has just moved onto another conveyor
     * from moving several tiles during the same simulation tick.
     */
    private long conveyorMovementTick =
            -1;

    private final Set<Integer> movedCargoThisTick =
            new HashSet<>();

    @Override
    public void onLoad(
            ModContext context
    ) {
        context.logger().info(
                "Conveyor Belts loaded!"
        );
    }

    @Override
    public void onWorldLoaded(
            ModContext context
    ) {
        conveyorMovementTick =
                -1;

        movedCargoThisTick.clear();

        registerMenuAction(
                context
        );

        registerConveyorBehaviour(
                context
        );

        context.api()
                .ui()
                .showMessage(
                        "Conveyor Belts loaded."
                );
    }

    private void registerMenuAction(
            ModContext context
    ) {

        context.worldResource(
                context.api()
                        .ui()
                        .registerGameAction(
                                MOD_ID
                                        + ":place-conveyor",
                                "Place Conveyor",
                                () -> startConveyorPlacement(
                                        context
                                )
                        )
        );
    }

    private void registerConveyorBehaviour(
            ModContext context
    ) {

        context.worldResource(
                context.api()
                        .itemBehaviours()
                        .register(
                                CONVEYOR,
                                CONVEYOR_INTERVAL_TICKS,
                                tick -> processConveyor(
                                        context,
                                        tick
                                )
                        )
        );

        context.logger().info(
                "Registered conveyor behaviour for "
                        + CONVEYOR
                        + " every "
                        + CONVEYOR_INTERVAL_TICKS
                        + " ticks."
        );
    }

    /**
     * Called periodically for every conveyor item in the world.
     */
    private void processConveyor(
            ModContext context,
            ItemBehaviourContext tick
    ) {

        long currentTick =
                tick.simulationTick();

        /*
         * Start a fresh movement record for each simulation tick.
         */
        if (currentTick
                != conveyorMovementTick) {

            conveyorMovementTick =
                    currentTick;

            movedCargoThisTick.clear();
        }

        var conveyor =
                tick.item();

        var inventory =
                context.api()
                        .inventory();

        /*
         * The conveyor itself acts as a container.
         */
        var conveyorContainer =
                inventory.container(
                        conveyor.id()
                );

        if (conveyorContainer == null) {
            return;
        }

        /*
         * A conveyor carrying an item first attempts
         * to push that item forwards.
         */
        if (!conveyorContainer.empty()) {

            pushForward(
                    context,
                    conveyor,
                    conveyorContainer,
                    currentTick
            );

            return;
        }

        /*
         * An empty conveyor attempts to pull an item
         * from a normal container directly behind it.
         */
        pullFromBehind(
                context,
                conveyor,
                currentTick
        );
    }

    private void pushForward(
            ModContext context,
            xaos.api.items.ItemView conveyor,
            xaos.api.inventory.ContainerView source,
            long currentTick
    ) {

        var cargoId =
                source.itemIds()
                        .get(0);

        /*
         * Cargo may only move once during a simulation tick.
         */
        if (movedCargoThisTick.contains(
                cargoId.value()
        )) {
            return;
        }

        WorldPosition destination =
                positionInFront(
                        conveyor.position(),
                        conveyor.facing()
                );

        var target =
                containerAt(
                        context,
                        destination
                );

        if (target == null
                || target.full()) {

            return;
        }

        transferCargo(
                context,
                source.containerId(),
                target.containerId(),
                cargoId,
                currentTick
        );
    }

    private void pullFromBehind(
            ModContext context,
            xaos.api.items.ItemView conveyor,
            long currentTick
    ) {

        WorldPosition sourcePosition =
                positionBehind(
                        conveyor.position(),
                        conveyor.facing()
                );

        var world =
                context.api()
                        .world();

        if (!world.isValidPosition(
                sourcePosition
        )) {
            return;
        }

        var sourceCell =
                world.cell(
                        sourcePosition
                );

        if (sourceCell == null
                || sourceCell.itemId() == null) {

            return;
        }

        var sourceItemId =
                sourceCell.itemId();

        /*
         * Conveyor-to-conveyor movement is push driven.
         *
         * An empty conveyor should therefore not pull
         * backwards from another conveyor.
         */
        if (isConveyor(
                context,
                sourceItemId
        )) {
            return;
        }

        var inventory =
                context.api()
                        .inventory();

        var source =
                inventory.container(
                        sourceItemId
                );

        if (source == null
                || source.empty()) {

            return;
        }

        var target =
                inventory.container(
                        conveyor.id()
                );

        if (target == null
                || target.full()) {

            return;
        }

        /*
         * Try each item until one can be transferred.
         *
         * This prevents an incompatible item at the front
         * of a mixed container from blocking everything else.
         */
        for (var cargoId
                : source.itemIds()) {

            if (movedCargoThisTick.contains(
                    cargoId.value()
            )) {
                continue;
            }

            if (transferCargo(
                    context,
                    source.containerId(),
                    target.containerId(),
                    cargoId,
                    currentTick
            )) {

                return;
            }
        }
    }

    /**
     * Reserve an item and transfer it between two containers.
     */
    private boolean transferCargo(
            ModContext context,
            xaos.api.entity.EntityId sourceContainerId,
            xaos.api.entity.EntityId targetContainerId,
            xaos.api.entity.EntityId cargoId,
            long currentTick
    ) {

        var inventory =
                context.api()
                        .inventory();

        var reservation =
                inventory.reserveItem(
                        sourceContainerId,
                        cargoId
                );

        if (reservation == null) {
            return false;
        }

        var result =
                inventory.transferReserved(
                        reservation,
                        targetContainerId
                );

        if (!result.complete()) {

            /*
             * Conveyor logic never keeps reservations
             * between behaviour cycles.
             */
            inventory.releaseReservation(
                    reservation
            );

            return false;
        }

        movedCargoThisTick.add(
                cargoId.value()
        );

        return true;
    }

    /**
     * Return a container occupying a particular world tile.
     */
    private xaos.api.inventory.ContainerView containerAt(
            ModContext context,
            WorldPosition position
    ) {

        var world =
                context.api()
                        .world();

        if (!world.isValidPosition(
                position
        )) {
            return null;
        }

        var cell =
                world.cell(
                        position
                );

        if (cell == null
                || cell.itemId() == null) {

            return null;
        }

        return context.api()
                .inventory()
                .container(
                        cell.itemId()
                );
    }

    /**
     * Determine whether a world item is one of this mod's conveyors.
     */
    private boolean isConveyor(
            ModContext context,
            xaos.api.entity.EntityId itemId
    ) {

        return context.api()
                .items()
                .findInWorld(
                        CONVEYOR
                )
                .stream()
                .anyMatch(
                        item -> item.id()
                                .equals(
                                        itemId
                                )
                );
    }

    private WorldPosition positionInFront(
            WorldPosition position,
            ItemFacing facing
    ) {

        return switch (facing) {

            case WEST ->
                    position.offset(
                            -1,
                            0,
                            0
                    );

            case NORTH ->
                    position.offset(
                            0,
                            -1,
                            0
                    );

            case EAST ->
                    position.offset(
                            1,
                            0,
                            0
                    );

            case SOUTH ->
                    position.offset(
                            0,
                            1,
                            0
                    );
        };
    }

    private WorldPosition positionBehind(
            WorldPosition position,
            ItemFacing facing
    ) {

        return switch (facing) {

            case WEST ->
                    position.offset(
                            1,
                            0,
                            0
                    );

            case NORTH ->
                    position.offset(
                            0,
                            1,
                            0
                    );

            case EAST ->
                    position.offset(
                            -1,
                            0,
                            0
                    );

            case SOUTH ->
                    position.offset(
                            0,
                            -1,
                            0
                    );
        };
    }

    /**
     * Enter the game's normal queue-and-place mode
     * using the conveyor build action.
     */
    private void startConveyorPlacement(
            ModContext context
    ) {

        var result =
                context.api()
                        .tasks()
                        .beginQueueAndPlace(
                                BUILD_CONVEYOR
                        );

        if (!result.started()) {

            context.api()
                    .ui()
                    .showMessage(
                            "Could not start conveyor placement: "
                                    + result.message()
                    );
        }
    }

    @Override
    public void onWorldUnloaded(
            ModContext context
    ) {

        conveyorMovementTick =
                -1;

        movedCargoThisTick.clear();

        context.logger().info(
                "Conveyor Belts world unloaded."
        );
    }

    @Override
    public void onUnload(
            ModContext context
    ) {
        context.logger().info(
                "Conveyor Belts unloaded."
        );
    }
}