package myfirstmod;

import xaos.api.mods.ModContext;
import xaos.api.mods.TownsMod;

public final class MyFirstMod implements TownsMod {

    @Override
    public void onLoad(ModContext context) {
        context.logger().info(
                "My First Mod loaded!"
        );
    }

    @Override
    public void onWorldLoaded(ModContext context) {
        context.logger().info(
                "World loaded!"
        );
    }

    @Override
    public void onWorldUnloaded(ModContext context) {
        context.logger().info(
                "World unloaded."
        );
    }

    @Override
    public void onUnload(ModContext context) {
        context.logger().info(
                "My First Mod unloaded."
        );
    }
}