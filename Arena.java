package me.ssscrazy.snowball;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Arena {
	
	String arenaName;
	String gameMode;
	int maxPlayers;
	int minPlayers;
	double lives;
	int time;
	double xCoordinate;
	double yCoordinate;
	double zCoordinate;
	String worldName;
	World world;
	ArrayList<Player> participants = new ArrayList<Player>();
	boolean gameStarting;
	boolean gameActive;
	int timeToStart = time;
	int gameTimer = 0;
	int Snowballs = 0;
	int Jump = 0;
	int Speed = 0;

	Arena(String name) {
		
		arenaName = name;
		world = Bukkit.getWorld(Main.getMe().getConfig().getString(arenaName + ".Coordinates.World"));
		
		if (world == null) {
			
			System.out.println("[SnowBallFight]: ERROR! World does not exist!"); // Error checking. Making sure that this arena actually exists.
			
		} else {
			
			// In here, we load all of the values from the config file for this specific arena.

			xCoordinate = Main.getMe().getConfig().getInt(arenaName + ".Coordinates.X"); // Get the position of the arena.
			yCoordinate = Main.getMe().getConfig().getInt(arenaName + ".Coordinates.Y");
			zCoordinate = Main.getMe().getConfig().getInt(arenaName + ".Coordinates.Z");
			lives = Main.getMe().getConfig().getDouble(arenaName + ".Lives"); // The amount of lives for each player.
			minPlayers = Main.getMe().getConfig().getInt(arenaName + ".Minimum Players"); // Other config settings.
			Snowballs = Main.getMe().getConfig().getInt(arenaName + ".Snowballs");
			Speed = Main.getMe().getConfig().getInt(arenaName + ".Speed");
			Jump = Main.getMe().getConfig().getInt(arenaName + ".Jump");
			
			System.out.println("[SnowBallFight]: new Arena enabled.");
			gameStarting = false; // Game isn't starting or active yet.
			gameActive = false;
			
			if (Main.getMe().getConfig().getStringList("Arenas") == null) { // If the config is set up wrong
				
				List<String> newList = Arrays.asList(name);
				Main.getMe().getConfig().set("Arenas", newList); // Set it to a default value.
				return;
				
			} else {
				
				List<String> arenas = Main.getMe().getConfig().getStringList("Arenas");
				
				if (arenas.contains(name)) {
					
					return;
					
				} else { // If the list of arenas doesn't contain this one, add it to the list.
					
					arenas.add(name);
					Main.getMe().getConfig().set("Arenas", arenas);
					Main.getMe().saveConfig();
					
				}
			}
		}
	}

	public void teleportToArena(Player player, String arenaName) { // Takes a player, and teleports them to the spawn coordinates
																																 // of the corresponding arena. Make sure u save their inventory beforehand
		
		player.setMaxHealth(2 * lives); // Each heart is equal to 2 HP, and I want a heart to designate one life.
		
		player.setHealth(2 * lives);
		
		Location arena = new Location(world, xCoordinate, yCoordinate, zCoordinate);
		
		player.setFoodLevel(Integer.MAX_VALUE); // We don't want our participants to starve during the snow ball fight.
		
		player.getInventory().clear(); // MAKE SURE YOU SAVED THE INVENTORY PRIOR TO CALLING THIS FUNCTION.
		
		player.teleport(arena);
		
		player.sendMessage(Main.getMe().sbf + ChatColor.GREEN + "You have been added to the wait queue!");
		
	}

	public boolean getArenaConfig(String arenaName) {
		// Loads the config file into memory.
		
		time = Main.getMe().getConfig().getInt(arenaName + ".Time");
		lives = Main.getMe().getConfig().getDouble(arenaName + ".Lives");
		maxPlayers = Main.getMe().getConfig().getInt(arenaName + ".Maximum Players");
		minPlayers = Main.getMe().getConfig().getInt(arenaName + ".Minimum Players");
		world = Bukkit.getWorld(Main.getMe().getConfig().getString(arenaName + ".Coordinates.World"));
		xCoordinate = Main.getMe().getConfig().getInt(arenaName + ".Coordinates.X");
		yCoordinate = Main.getMe().getConfig().getInt(arenaName + ".Coordinates.Y");
		zCoordinate = Main.getMe().getConfig().getInt(arenaName + ".Coordinates.Z");
		
		if (world == null) { // Make sure the world isn't null.
			
			System.out.println("[SBF]: ERROR! WORLD IS INVALID!");
			return false;
			
		}
		
		return true;
	}

	public static void setArenaConfig(String arenaName, double xCoordinate, double yCoordinate, double zCoordinate,
			String world) {
		
		Main.getMe().getConfig().set(arenaName + ".Coordinates.World", world);
		Main.getMe().getConfig().set(arenaName + ".Coordinates.X", xCoordinate);
		Main.getMe().getConfig().set(arenaName + ".Coordinates.Y", yCoordinate);
		Main.getMe().getConfig().set(arenaName + ".Coordinates.Z", zCoordinate);
		Main.getMe().saveConfig(); // Useful for setting config files in game. Doing it from a text editor is still way easier, however.
		
	}

	public void startGame(Player player, final String arenaName, boolean forceStart) {
		
		Main.getMe().activeArenas.get(arenaName).participants.add(player);
		Main.getMe().originalInventory.put(player.getName(), new SaveInventory(player));
		Main.getMe().originalLocations.put(player.getName(), player.getLocation());
		player.getInventory().clear();
		Main.getMe().activeArenas.get(arenaName).teleportToArena(player, arenaName);
		time = Main.getMe().getConfig().getInt(arenaName + ".Time");
		Main.getMe().activeArenas.get(arenaName).gameStarting = true;
		player.sendMessage(Main.getMe().sbf + ChatColor.GREEN + "You have joined the wait queue");
		
		if (Main.getMe().activeArenas.get(arenaName).participants.size() >= Main.getMe().getConfig()
				.getInt(arenaName + ".Minimum Players") || forceStart) { // Make sure there are enough players to begin the match.
			
			timeToStart = Main.getMe().activeArenas.get(arenaName).time;
			Bukkit.getScheduler().cancelTask(gameTimer); // make sure there already isn't a timer running for this match. Redundant, but safe.
			
			gameTimer = player.getServer().getScheduler().scheduleSyncRepeatingTask(Main.getMe(), new Runnable() {
				// This runs real time, i.e. each pass of the loop takes one second.
				
				public void run() {

					if (time == timeToStart) {
						
						for (Player p : Bukkit.getOnlinePlayers()) {
							
							if (Main.getMe().activeArenas.get(arenaName).participants.contains(p)) {
								
								p.sendMessage(Main.getMe().sbf + ChatColor.GOLD
										+ "There are enough players and the match will begin in " + timeToStart
										+ " seconds!");
								
								System.out.println("There are enough players and the game will begin!"); // tells console.
								
							}
						}
						
						timeToStart--; // Decrements the timer.
						
					} else if (timeToStart < time && timeToStart > 5) {
						
						timeToStart--;
						
					} else if (timeToStart == 5) {
						
						for (Player z : Bukkit.getOnlinePlayers()) {
							
							if (Main.getMe().activeArenas.get(arenaName).participants.contains(z)) {
								
								z.sendMessage(Main.getMe().sbf + ChatColor.GOLD + "The game is starting in 5 seconds!");

							}
						}
						
						timeToStart--;
						
					} else if (timeToStart < 5 && timeToStart != 0) {
						
						timeToStart--;
						
					} else if (timeToStart <= 0) {
						
						for (Player q : Bukkit.getOnlinePlayers()) {
							
							if (Main.getMe().activeArenas.get(arenaName).participants.contains(q)) {
								
								Snowballs = Main.getMe().getConfig().getInt(arenaName + ".Snowballs");
								
								int RemainingSnowballs = 0;
								Speed = Main.getMe().getConfig().getInt(arenaName + ".Speed");
								Jump = Main.getMe().getConfig().getInt(arenaName + ".Jump");
								
								if (Snowballs > 16) { // Stacks of snowballs are of size 16, so we find out how many stacks to give a player.
									
									int Stacks = Snowballs / 16;
									
									if (Snowballs % 16 != 0) { // And then we add the remainder. I.e. if there are 40 snowballs to be given, its 2 full stacks
																						// and one half stack.
										
										RemainingSnowballs = Snowballs % 16;
										
									}

									for (int i = 0; i <= Stacks; i++) {
										
										q.getInventory().addItem(new ItemStack(Material.SNOW_BALL, 16));
										q.getInventory().addItem(new ItemStack(Material.SNOW_BALL, RemainingSnowballs));
										
									}
									
								} else if (Snowballs <= 16 && Snowballs > 0) {
									
									q.getInventory().addItem(new ItemStack(Material.SNOW_BALL, Snowballs));
									
								}
								
								if (Speed > 0 && Speed <= 4) { // Make sure the value for the speed potion is legitimate.
									
									q.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50000, Speed));
									
								}
								
								if (Jump > 0 && Jump <= 4) { // Same as speed potion.
									
									q.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 50000, Jump));
									
								}
								
								q.sendMessage(Main.getMe().sbf + ChatColor.GOLD
										+ "The game has begun!  Hit other players with your snowballs to eliminate them!");
								
								q.sendMessage(Main.getMe().sbf + ChatColor.GOLD
										+ "For this match, unlimited snowballs was set to: "
										+ Main.getMe().getConfig().getString(arenaName + ".Unlimited")); // Says true or false, depending on the config
								// If false, you have only the snowballs initially given by the start of the match.
								
								
								timeToStart = Main.getMe().activeArenas.get(arenaName).time;
								Main.getMe().activeArenas.get(arenaName).gameActive = true;
								Main.getMe().activeArenas.get(arenaName).gameStarting = false;
								
							}

						}
						Bukkit.getScheduler().cancelTask(gameTimer); // End this timer so it doesn't run infinitely.
					}
				}
			}, 0L, 20L);
		}
	}
}
