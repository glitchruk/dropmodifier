# DropModifier

A simple [PaperMC](https://papermc.io/) plugin that allows you to modify the drop rate of blocks.

## Installation

1. Download the plugin from the [releases page](https://github.com/glitchruk/dropmodifier/releases)
2. Place the plugin in your `plugins` folder
3. Restart your server

## Permissions

| Permission            | Description                                           |
|-----------------------|-------------------------------------------------------|
| `dropmodifier.use`    | Allows the player to use the command.                 |
| `dropmodifier.set`    | Allows the player to set the drop rate of a block.    |
| `dropmodifier.get`    | Allows the player to get the drop rate of a block.    |
| `dropmodifier.remove` | Allows the player to remove the drop rate of a block. |

## Commands

| Command                       | Description                       |
|-------------------------------|-----------------------------------|
| `/drops set <block> <chance>` | Sets the drop rate of a block.    |
| `/drops get <block>`          | Gets the drop rate of a block.    |
| `/drops get`                  | Gets the drop rate of all blocks. |
| `/drops remove <block>`       | Removes the drop rate of a block. |