package me.ssscrazy.snowball;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SaveInventory {
	private HashMap<String, ItemStack> originalInventory = new HashMap<String, ItemStack>();
	private Player p;
	public SaveInventory(Player player) {
		p = player;
		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack item = inventory.getItem(i);
			if(item != null)
			originalInventory.put("s" + i, item.clone());
		}
		if(player.getInventory().getHelmet() != null)
			originalInventory.put("Helmet", player.getInventory().getHelmet().clone());
		if(player.getInventory().getChestplate() != null)
			originalInventory.put("Chestplate", player.getInventory().getChestplate().clone());
		if(player.getInventory().getLeggings() != null)
			originalInventory.put("Leggings", player.getInventory().getLeggings().clone());
		if(player.getInventory().getBoots() != null)
			originalInventory.put("Boots", player.getInventory().getBoots().clone());
		
		
	}
	public void Load() {
		for (int i = 0; i < p.getInventory().getSize(); i++)  {
			p.getInventory().setItem(i, originalInventory.get("s" + i));
		}
		p.getInventory().setHelmet(originalInventory.get("Helmet"));
		p.getInventory().setChestplate(originalInventory.get("Chestplate"));
		p.getInventory().setLeggings(originalInventory.get("Leggings"));
		p.getInventory().setBoots(originalInventory.get("Boots"));
	}
}
