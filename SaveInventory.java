package me.ssscrazy.snowball;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SaveInventory { // Temporarily stores a player's inventory in this plugin's memory while the player is in a snowball match
			     // This allows for players to play the normal game of Minecraft and join/leave arenas as they please.
			     // i.e. the player does not need to worry about losing their items while in this special gamemode.
	private HashMap<String, ItemStack> originalInventory = new HashMap<String, ItemStack>(); // Create a list of the player's items.
	private Player p; 
	public SaveInventory(Player player) { // This function stores the inventory of the player that was passed into the function.
		p = player;
		Inventory inventory = player.getInventory(); // Get their entire inventory.

		for (int i = 0; i < inventory.getSize(); i++) { // Cycle through all the potential item slots in their inventory
			ItemStack item = inventory.getItem(i); // Get the item in that slot
			if(item != null) // If it wasn't empty,
			originalInventory.put("s" + i, item.clone()); // Get a copy of the item and put it into our saved inventory.
		}
		if(player.getInventory().getHelmet() != null) // Check if the player is wearing a helmet.
			originalInventory.put("Helmet", player.getInventory().getHelmet().clone()); // If so, store the helmet.
		if(player.getInventory().getChestplate() != null) // Same as above
			originalInventory.put("Chestplate", player.getInventory().getChestplate().clone());
		if(player.getInventory().getLeggings() != null) 
			originalInventory.put("Leggings", player.getInventory().getLeggings().clone()); 
		if(player.getInventory().getBoots() != null)
			originalInventory.put("Boots", player.getInventory().getBoots().clone());
		
		
	}
	public void Load() { // This is called when we return the player's inventory after they leave a match.
		for (int i = 0; i < p.getInventory().getSize(); i++)  { // Set their items back to how they were, in the same spots as before.
			p.getInventory().setItem(i, originalInventory.get("s" + i));
		}
		p.getInventory().setHelmet(originalInventory.get("Helmet")); // Put their armor back on as well.
		p.getInventory().setChestplate(originalInventory.get("Chestplate"));
		p.getInventory().setLeggings(originalInventory.get("Leggings"));
		p.getInventory().setBoots(originalInventory.get("Boots"));
	}
}
