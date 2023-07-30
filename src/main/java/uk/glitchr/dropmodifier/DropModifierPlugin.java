package uk.glitchr.dropmodifier;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;


public class DropModifierPlugin extends JavaPlugin implements Listener {

    /**
     * The instance of the plugin.
     */
    private static DropModifierPlugin instance;

    /**
     * Creates a new instance of the plugin.
     */
    public DropModifierPlugin() {
        instance = this;
    }

    /**
     * Gets the instance of the plugin.
     *
     * @return The instance of the plugin.
     */
    public static DropModifierPlugin getInstance() {
        return instance;
    }

    /**
     * Called when the plugin is enabled.
     */
    @Override
    public void onEnable() {
        getLogger().info("DropModifier enabled!");
        getServer().getPluginManager().registerEvents(this, this);

        final PluginCommand dropsCommand = getCommand("drops");

        if (dropsCommand == null) {
            getLogger().severe("Could not get drops command!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final DropModifierCommand command = new DropModifierCommand();

        dropsCommand.setExecutor(command);
        dropsCommand.setTabCompleter(command);
    }

    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("DropModifier disabled!");
    }

    /**
     * Called when a block is broken.
     *
     * @param event The block break event.
     */
    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        // -- Determine the block type.
        final String blockType = event.getBlock().getType().getKey().asString();

        // -- Get the drop chance from the config.
        final float dropChance = (float) getConfig().getDouble("blocks." + blockType, -1);

        // -- Get the block above the block that was broken.
        final Block blockAbove = event.getBlock().getRelative(BlockFace.UP);
        final float aboveDropChance = (float) getConfig().getDouble("blocks." + blockAbove.getType().getKey().asString(), -1);

        // -- If the block above has a drop chance, and it's a crop, check if it should drop.
        if (aboveDropChance != -1 && blockAbove.getBlockData() instanceof Ageable) {
            // -- If the drop chance is 0, or if the random number is greater than the drop chance, set the block above to air.
            //    This is because the block above is a crop, the crop will drop the item.
            if (aboveDropChance == 0 || Math.random() > aboveDropChance) {
                blockAbove.setType(Material.AIR);
            }
        }

        // -- If the block doesn't have a drop chance, don't do anything.
        if (dropChance == -1) {
            return;
        }

        // -- If drop chance is 0, or if the random number is greater than the drop chance, don't drop anything.
        if (dropChance == 0 || Math.random() > dropChance) {
            event.setDropItems(false);
        }
    }

}
