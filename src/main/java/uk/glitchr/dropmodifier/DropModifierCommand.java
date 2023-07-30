package uk.glitchr.dropmodifier;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Command for modifying drop chances.
 * <p>
 * Usage: /drops <subcommand> [args...]
 * <p>
 * Subcommands:
 * <ul>
 *     <li>set <block> <chance> - Sets the drop chance for a block</li>
 *     <li>get <block> - Gets the drop chance for a block</li>
 *     <li>remove <block> - Removes the drop chance for a block</li>
 * </ul>
 */
public class DropModifierCommand implements CommandExecutor, TabCompleter {

    private static final String ERROR_NO_PERMISSION = "You do not have permission to use this command!";
    private static final String ERROR_INVALID_SUBCOMMAND = "Invalid subcommand";
    private static final String ERROR_PROVIDE_SUBCOMMAND = "Please provide a subcommand";
    private static final String ERROR_PROVIDE_BLOCK = "Please provide a block";
    private static final String ERROR_PROVIDE_CHANCE = "Please provide a drop chance";
    private static final String ERROR_INVALID_CHANCE = "Drop chance must be a number";
    private static final String ERROR_CHANCE_RANGE = "Drop chance must be between 0 and 1";
    private static final String ERROR_NO_CHANCES = "No drop chances have been set";
    private static final String ERROR_NO_CHANCE_BLOCK = "No drop chance has been set for: %s";
    private static final String MESSAGE_SET_CHANCE = "Set drop chance for %s to ";
    private static final String MESSAGE_GET_CHANCE = "Drop chance for %s is ";
    private static final String MESSAGE_REMOVE_CHANCE = "Removed drop chance for %s";
    private static final String MESSAGE_LIST_CHANCES = "Drop chances:";

    private static final String PERMISSION_USE = "dropmodifier.use";
    private static final String PERMISSION_SET = "dropmodifier.set";
    private static final String PERMISSION_GET = "dropmodifier.get";
    private static final String PERMISSION_REMOVE = "dropmodifier.remove";

    private static final List<String> SUBCOMMANDS = List.of("set", "get", "remove");


    /**
     * Called when a command is tab-completed by a player.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside a command block, this will be the player, not
     *                the command block.
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return A List of possible completions for the final argument, or null.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            List<String> completions = new ArrayList<>();

            for (String subcommand : SUBCOMMANDS) {
                if (subcommand.startsWith(input)) {
                    completions.add(subcommand);
                }
            }

            return completions;
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase();

            List<String> completions = new ArrayList<>();

            for (Material material : Material.values()) {
                if (material.isBlock() && material.name().toLowerCase().startsWith(input)) {
                    completions.add("minecraft:" + material.name().toLowerCase());
                }
            }

            return completions;
        }

        return null;
    }

    /**
     * Called when a command is executed by a player.
     * <p>
     * Usage: /drops <subcommand> [args...]
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return True if a valid command, otherwise false.
     */
    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String[] args
    ) {
        // -- Check permissions.
        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage(Component.text(ERROR_NO_PERMISSION).color(NamedTextColor.RED));
            return true;
        }

        // -- Ensure the subcommand is provided.
        if (args.length < 1) {
            sender.sendMessage(Component.text(ERROR_PROVIDE_SUBCOMMAND).color(NamedTextColor.RED));
            return false;
        }

        // -- Check if the subcommand is valid.
        final String subcommand = args[0].toLowerCase();
        if (!SUBCOMMANDS.contains(subcommand)) {
            sender.sendMessage(Component.text(ERROR_INVALID_SUBCOMMAND).color(NamedTextColor.RED));
            return false;
        }

        // -- Get subcommand arguments.
        final String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        // -- Handle the subcommand.
        return switch (subcommand) {
            case "set" -> handleSet(sender, subArgs);
            case "get" -> handleGet(sender, subArgs);
            case "remove" -> handleRemove(sender, subArgs);
            default -> handleInvalid(sender, subArgs);
        };
    }

    /**
     * Create a text component for the drop chance, colored based on the chance.
     *
     * @param chance The drop chance.
     * @return The text component.
     */
    private Component colorChance(final float chance) {
        if (chance < 0.25) {
            return Component.text(chance).color(NamedTextColor.RED);
        } else if (chance < 0.5) {
            return Component.text(chance).color(NamedTextColor.GOLD);
        } else if (chance < 0.75) {
            return Component.text(chance).color(NamedTextColor.YELLOW);
        } else {
            return Component.text(chance).color(NamedTextColor.GREEN);
        }
    }

    /**
     * Ensure that the block is prefixed with 'minecraft:'.
     *
     * @param blockName The block name.
     * @return The block name with the 'minecraft:' prefix.
     */
    private String formatBlock(String blockName) {
        blockName = blockName.toLowerCase();
        if (!blockName.startsWith("minecraft:")) {
            return "minecraft:" + blockName;
        }
        return blockName;
    }

    /**
     * Handle the "set" subcommand.
     * <p>
     * Usage: /drops set <block> <chance>
     *
     * @param sender The command sender.
     * @param args   The subcommand arguments.
     * @return Whether the command was successful.
     */
    private boolean handleSet(
            final @NotNull CommandSender sender,
            final @NotNull String[] args
    ) {
        // -- Check permissions.
        if (!sender.hasPermission(PERMISSION_SET)) {
            sender.sendMessage(Component.text(ERROR_NO_PERMISSION).color(NamedTextColor.RED));
            return true;
        }

        // -- Check the number of arguments.
        if (args.length < 1) {
            sender.sendMessage(Component.text(ERROR_PROVIDE_BLOCK).color(NamedTextColor.RED));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text(ERROR_PROVIDE_CHANCE).color(NamedTextColor.RED));
            return false;
        }

        // -- Parse drop chance.
        final float dropChance;
        try {
            dropChance = Float.parseFloat(args[1]);
        } catch (final NumberFormatException exception) {
            sender.sendMessage(Component.text(ERROR_INVALID_CHANCE).color(NamedTextColor.RED));
            return true;
        }

        // -- Check if drop chance is valid.
        if (dropChance < 0 || dropChance > 1) {
            sender.sendMessage(Component.text(ERROR_CHANCE_RANGE).color(NamedTextColor.RED));
            return true;
        }

        // -- Get block.
        String block = formatBlock(args[0]);

        // -- Set drop chance.
        final FileConfiguration config = DropModifierPlugin.getInstance().getConfig();
        config.set("blocks." + block, dropChance);
        DropModifierPlugin.getInstance().saveConfig();

        // -- Send message.
        sender.sendMessage(Component.text(MESSAGE_SET_CHANCE.formatted(block)).append(colorChance(dropChance)));

        // -- The command was successful.
        return true;
    }

    /**
     * Handle the "get" subcommand.  Get the drop chance of a block, or all blocks if no block is specified.
     * <p>
     * Usage: /drops get [block]
     *
     * @param sender The command sender.
     * @param args   The subcommand arguments.
     * @return Whether the command was successful.
     */
    private boolean handleGet(
            final @NotNull CommandSender sender,
            final @NotNull String[] args
    ) {
        // -- Check permissions.
        if (!sender.hasPermission(PERMISSION_GET)) {
            sender.sendMessage(Component.text(ERROR_NO_PERMISSION).color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            // == No block specified.  Get all blocks.

            // -- Get blocks from config.
            final FileConfiguration config = DropModifierPlugin.getInstance().getConfig();
            final ConfigurationSection blocksSection = config.getConfigurationSection("blocks");

            // -- Check if there are any blocks.
            if (blocksSection == null || blocksSection.getKeys(false).isEmpty()) {
                sender.sendMessage(Component.text(ERROR_NO_CHANCES).color(NamedTextColor.RED));
                return true;
            }

            // -- Get blocks.
            final Set<String> blocks = blocksSection.getKeys(false);

            // -- Display blocks and drop chances.
            sender.sendMessage(Component.text(MESSAGE_LIST_CHANCES).color(NamedTextColor.GOLD));
            for (final String block : blocks) {
                final float dropChance = (float) config.getDouble("blocks." + block, -1);
                sender.sendMessage(Component.text(" - " + block + ": ").append(colorChance(dropChance)));
            }
        } else {
            // == Block specified.  Get drop chance of block.

            // -- Get block.
            String block = formatBlock(args[0]);

            // -- Get drop chance.
            final FileConfiguration config = DropModifierPlugin.getInstance().getConfig();
            final float dropChance = (float) config.getDouble("blocks." + block, -1);

            // -- Check if drop chance exists.
            if (dropChance == -1) {
                sender.sendMessage(Component.text(ERROR_NO_CHANCE_BLOCK.formatted(block)).color(NamedTextColor.RED));
                return true;
            }

            // -- Send message.
            sender.sendMessage(Component.text(MESSAGE_GET_CHANCE.formatted(block)).append(colorChance(dropChance)));
        }

        // -- The command was successful.
        return true;
    }

    /**
     * Handle the "remove" subcommand.  Remove the drop chance of a block.
     * <p>
     * Usage: /drops remove <block>
     *
     * @param sender The command sender.
     * @param args   The subcommand arguments.
     * @return Whether the command was successful.
     */
    private boolean handleRemove(
            final @NotNull CommandSender sender,
            final @NotNull String[] args
    ) {
        // -- Check permissions.
        if (!sender.hasPermission(PERMISSION_REMOVE)) {
            sender.sendMessage(Component.text(ERROR_NO_PERMISSION).color(NamedTextColor.RED));
            return true;
        }

        // -- Check the number of arguments.
        if (args.length < 1) {
            sender.sendMessage(Component.text(ERROR_PROVIDE_BLOCK).color(NamedTextColor.RED));
            return false;
        }

        // -- Get block.
        String block = formatBlock(args[0]);

        // -- Get config.
        final FileConfiguration config = DropModifierPlugin.getInstance().getConfig();

        // -- Check if drop chance exists.
        if (!config.contains("blocks." + block)) {
            sender.sendMessage(Component.text(ERROR_NO_CHANCE_BLOCK.formatted(block)).color(NamedTextColor.RED));
            return true;
        }

        // -- Remove drop chance.
        config.set("blocks." + block, null);
        DropModifierPlugin.getInstance().saveConfig();

        // -- Send message.
        sender.sendMessage(Component.text(MESSAGE_REMOVE_CHANCE.formatted(block)).color(NamedTextColor.GREEN));

        // -- The command was successful.
        return true;
    }

    /**
     * Handle any invalid subcommands.
     *
     * @param sender The command sender.
     * @param args   The subcommand arguments.
     * @return Whether the command was successful.
     */
    private boolean handleInvalid(
            final @NotNull CommandSender sender,
            final @NotNull String[] args
    ) {
        // -- Send message.
        sender.sendMessage(Component.text(ERROR_INVALID_SUBCOMMAND).color(NamedTextColor.RED));

        // -- The command was not successful.
        return false;
    }

}
