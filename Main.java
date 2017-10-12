package me.ssscrazy.snowball;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

public class Main extends JavaPlugin {
	
	public static Main me; // Here, we are initializing some default values that we need later on.
	public ArrayList<Player> participants = new ArrayList<Player>(); // This is all of the participants in the arena.
	public ArrayList<Player> deadParticipants = new ArrayList<Player>(); // This is all the dead participants.
	
	public Permission setValues = new Permission("sbf.set.all"); // These are various permissions. Only players with these permission
	public Permission forceStart = new Permission("sbf.start"); // nodes may enact the associated commands.
	public Permission forceStop = new Permission("sbf.stop"); // The full list of what the permissions do can be found on my spigot page,
	public Permission play = new Permission("sbf.play"); // but they are probably mostly self-explanatory
	public Permission full = new Permission("sbf.full");
	public Permission create = new Permission("sbf.create");
	
	public Inventory inventory = null;
	
	public HashMap<String, Location> originalLocations = new HashMap<String, Location>();
	public HashMap<String, Arena> activeArenas = new HashMap<String, Arena>();
	public HashMap<String, SaveInventory> originalInventory = new HashMap<String, SaveInventory>();
	
	public World world = null;
	public String sbf = ChatColor.BLUE + "[" + ChatColor.DARK_GREEN + "SBF" + ChatColor.BLUE + "]" + ChatColor.GRAY
			+ ": " + ChatColor.RESET; // Messages sent by this plugin are formatted according to these colors. It looks quite
						  // pretty in game.
	int gameTimer = 0;
	int timeToStart = 0;
	public String arenaName = "default";

	public static Main getMe() { // Allows us to refer to this plugin.
		return me;
	}

	public String nameOfArena(Player player) { // Returns the arena a player is a member of (the string), or null if none
						   // At all times a player should be in at most 1 arena.
						   // Should be coupled with isPlayerInArena because that will return false if a player isn't
						   // in an arena.
		
		Map<String, Arena> map1 = activeArenas; // Our map of all arenas.
		
		for (String p : map1.keySet()) {
			
			participants = activeArenas.get(p).participants; // Look at each p and get the participants list.
			
			if (participants.contains(player)) { // if the list contains the player, return this string.
				return p;
			} else { // Otherwise keep looping.
				continue;
			}
		}
		return null;
	}

	public boolean isPlayerInArena(Player player) { // Returns true if a player is in an arena. False otherwise.
		
		Map<String, Arena> map1 = activeArenas;
		
		for (String p : map1.keySet()) { // Similar logic as for finding the specific arena a player is in.
			participants = activeArenas.get(p).participants;
			if (participants.contains(player)) {
				return true;
			} else {
				continue;
			}
		}
		return false;
	}

	public void onEnable() { // This function runs whenever this plugin boots for the first time on a server.
		
		System.out.println("[SBF]: Plugin Enabling...");
		getServer().getPluginManager().registerEvents(new EventListener(), this); // This enables our EventListener class.
		me = this; // Now 'me' refers to this specific instance. Useful later.
		
		if (!new File(getDataFolder(), "config.yml").exists()) { // If we don't have a config file, generate a default one.
			saveDefaultConfig();
		}
		
		System.out.println("[SBF]: Plugin Enabled.");
		
	}

	public void onDisable() {

		System.out.println("Plugin disabling..."); // Runs whenever the plugin disables.

	}

	public void endGame(Player player, String arenaName) { // Runs when we want to end the game in a specific arena for a specific player.
		
		int playersRemaining = 0; // initialize this variable.
		Player winner = player; 
		
		for (Player allplayers : Bukkit.getOnlinePlayers()) { // Go through all online players
			if (activeArenas.get(arenaName).participants.contains(allplayers)) { // If this arena contains that player,
				playersRemaining = playersRemaining + 1; // We know there is one more player in the game than we knew prior.
			}
		}
		
		if (playersRemaining <= 1) { // If we have one or fewer players, then the game is over.
			
			activeArenas.get(arenaName).gameActive = false; // Set this arena's match to being over.
			
			for (Player allplayers : Bukkit.getOnlinePlayers()) {
				
				if (activeArenas.get(arenaName).participants.contains(allplayers)) {
					winner = allplayers;
				}
			}
			
			if (winner != player) { // This is so that we can use this same function for the multiple reasons a game might end.
						// such as if a player quits the match.
				
				winner.sendMessage(sbf + ChatColor.GREEN + "Congratulations on winning the snowball fight!");
				
				activeArenas.get(arenaName).participants.remove(winner); // Remove the player from the participants list.
				winner.setMaxHealth(20D); // Return their health to normal
				winner.setHealth(20D);
				winner.teleport(originalLocations.get(winner.getName())); // Return them to their original location.
				winner.removePotionEffect(PotionEffectType.SPEED); // Remove the potion effects given to the player while
				winner.removePotionEffect(PotionEffectType.JUMP);  // in the match.
				originalLocations.remove(winner.getName()); // Remove the player from the list, to save memory.
				originalInventory.get(winner.getName()).Load(); // Load their inventory.
				originalInventory.remove(winner.getName()); // Conserve memory.
				activeArenas.get(arenaName).gameActive = false; // Make sure everyone knows the match is over. (redundant)
				activeArenas.remove(arenaName); // remove this arena from the list.
				
			} else { // This runs when there is no clear winner, but the match should still end.
				
				activeArenas.get(arenaName).gameActive = false;
				activeArenas.get(arenaName).participants.remove(player);
				activeArenas.remove(arenaName);
				
			}
			
		} else {
			return;
		}

	}

	public void onHelpRequest(Player player) { // Whenever a player types a help command, these messages are sent to them.
		player.sendMessage(sbf + ChatColor.GOLD + "Help page for Snowball Fight!");
		player.sendMessage(sbf + ChatColor.GOLD + "Plugin made by ssscrazy!");
		player.sendMessage(ChatColor.GRAY + "/sbf help: Displays this page");
		player.sendMessage(ChatColor.GRAY + "/sbf info : Displays information on the plugin.");
		player.sendMessage(ChatColor.GRAY + "/sbf list : Displays all arenas.");
		if (player.hasPermission("sbf.set.all")) {
			player.sendMessage(
					ChatColor.GRAY + "/sbf set center  : Sets the center of the snowball arena at your location");
			player.sendMessage(ChatColor.GRAY + "/sbf set lives : Set the number of lives for each game");
			player.sendMessage(ChatColor.GRAY + "/sbf set maxplayers : Set the maximum number of players in a match.");
			player.sendMessage(ChatColor.GRAY + "/sbf set minplayers : Set the minimum players to play a match");
			player.sendMessage(ChatColor.GRAY
					+ "/sbf set time : Set the time before match starts after the minimum players has been reached.");
			player.sendMessage(
					ChatColor.GRAY + "/sbf set unlimited : Set whether an arena has unlimited snowballs or not.");
		}
		if (player.hasPermission("sbf.start")) {
			player.sendMessage(ChatColor.GRAY + "/sbf start : Force starts a snowball fight match.");
		}
		if (player.hasPermission("sbf.stop")) {
			player.sendMessage(ChatColor.GRAY + "/sbf stop : Force stops a snowball fight match.");
		}
		if (player.hasPermission("sbf.play")) {
			player.sendMessage(ChatColor.GRAY + "/sbf play : Joins the wait queue to play a snowball fight match!");
			player.sendMessage(ChatColor.GRAY + "/sbf quit : Leaves the game or wait match for the snowball fight!");
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		// Basically this code processes all the commands a player can type. Most of these are self-explanatory. I'll go in depth
		// on a couple of commands, however.
		if (sender instanceof Player) { // Make sure it is NOT the console sending a command.
			
			Player player = (Player) sender;
			int length = args.length;
			
			if (commandLabel.equalsIgnoreCase("sbf")) {
				
				if (length == 0) {
					onHelpRequest(player);
					return false;
					
				} else if (length == 1) {
					
					if (args[0].equalsIgnoreCase("stop")) {
						
						player.sendMessage(sbf + ChatColor.RED + "Specify an arena to stop!");

					} else if (args[0].equalsIgnoreCase("help")) {
						
						onHelpRequest(player);
						return true;

					} else if (args[0].equalsIgnoreCase("start")) {
						
						player.sendMessage(sbf + ChatColor.RED + "Specify an arena to start!");

					} else if (args[0].equalsIgnoreCase("info")) {
						
						player.sendMessage(sbf + ChatColor.GREEN + "Version: " + getDescription().getVersion() + "!");
						player.sendMessage(sbf + ChatColor.GREEN + "Made by ssscrazy!");
						return true;

					} else if (args[0].equalsIgnoreCase("play") || args[0].equalsIgnoreCase("join")) {
						
						if (player.hasPermission("sbf.play")) {
							player.sendMessage(sbf + ChatColor.RED + "Specify an arena to join.");
						}
						
					} else if (args[0].equalsIgnoreCase("quit") || args[0].equalsIgnoreCase("leave")) {
						
						if (Main.getMe().isPlayerInArena(player)) { // First of all, make sure we are in an arena.
							
							player.teleport(Main.getMe().originalLocations.get(player.getName()));
							Main.getMe().originalInventory.get(player.getName()).Load();
							Main.getMe().originalInventory.remove(player);
							Main.getMe().originalLocations.remove(player);
							player.removePotionEffect(PotionEffectType.SPEED); // Set them back to their prior state, and remove them
							player.removePotionEffect(PotionEffectType.JUMP); // from all the lists.
							player.setMaxHealth(20D);
							player.setHealth(20D);
							player.sendMessage(sbf + ChatColor.YELLOW + "You have succesfully left your match.");
							
							if (Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player)).gameActive) {
								
								Main.getMe().endGame(player, Main.getMe().nameOfArena(player)); // See if we should end the game.
								
							} else if (Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player))
								   .gameStarting) { // See if the game was in the process of starting.
								
								Main.getMe().participants = Main.getMe().activeArenas
										.get(Main.getMe().nameOfArena(player)).participants;
								
								int playersRemaining = 0;
								
								for (Player p : Main.getMe().participants) { // Tell all the participants that this player quit.
									
									playersRemaining = playersRemaining + 1;
									p.sendMessage(ChatColor.GREEN + player.getName() + " has left the match.");
									
								}
								
								if (playersRemaining <= 1) { // If this was the only player, end the match. I.e. stop the start-timer.
									
									Main.getMe().gameTimer = Main.getMe().activeArenas
											.get(Main.getMe().nameOfArena(player)).gameTimer;
									Bukkit.getScheduler().cancelTask(Main.getMe().gameTimer);
									Main.getMe().activeArenas.remove(Main.getMe().nameOfArena(player));
									
								} else {
									
									Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player))
										.participants.remove(player);
									
								}
							}

						} else {
							
							player.sendMessage(sbf + ChatColor.RED + "You are not in a match.");
							
						}
						
					} else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("arenas")) {
						
						player.sendMessage(sbf + ChatColor.GRAY
								+ "Green = Open  Yellow = Full  Red = Started  White = No Participants");
						
						int pAmount = 0;
						
						if (Main.getMe().getConfig().getStringList("Arenas") == null) {
							
							player.sendMessage(sbf + ChatColor.GRAY + "No arenas available.");
							return false;
							
						}
						
						List<String> listOfArenas = Main.getMe().getConfig().getStringList("Arenas");
						
						for (String p : listOfArenas) { // Go through all of the arenas,
														// And pull the necessary info that a player would want to see
														// The amount of players, the state of the game (starting, full, in progress, empty)
							
							arenaName = p;
							boolean gameStarting = false;
							boolean gameActive = false;
							
							if (activeArenas.containsKey(p)) {
								
								pAmount = activeArenas.get(p).participants.size() + 1;
								gameStarting = activeArenas.get(p).gameStarting;
								gameActive = activeArenas.get(p).gameActive;
								
							}

							if (gameStarting
									&& pAmount <= this.getConfig().getInt(p + ".Maximum Players")) {
								
								player.sendMessage(ChatColor.GREEN + arenaName + ": " + pAmount + "/"
										+ this.getConfig().getInt(p + ".Maximum Players"));
								
							} else if (gameStarting
									&& pAmount > this.getConfig().getInt(p + ".Maximum Players")) {
								
								player.sendMessage(ChatColor.YELLOW + arenaName + ": " + pAmount
										+ "/" + this.getConfig().getInt(p + ".Maximum Players"));
								
							} else if (gameActive) {
								
								player.sendMessage(ChatColor.RED + arenaName + ": " + pAmount + "/"
										+ this.getConfig().getInt(p + ".Maximum Players"));
								
							} else if (!activeArenas.containsKey(p)) {
								
								player.sendMessage(ChatColor.WHITE + arenaName + ": 0/10");
								
							}
						}
					}

				} else if (length == 2) {
					
					if (args[0].equalsIgnoreCase("set") && player.hasPermission("sbf.set.all")) {

						if (args[1].equalsIgnoreCase("lives")) {
							player.sendMessage(
									sbf + ChatColor.GRAY + "Specify an arena and a number between 1 and 10.");
							player.sendMessage(sbf + ChatColor.GRAY + "Example: /sbf set lives snow 5");
							return false;
						} else if (args[1].equalsIgnoreCase("maxplayers")) {
							player.sendMessage(sbf + ChatColor.GRAY + "Specify an arena and the amount");
							player.sendMessage(sbf + ChatColor.GRAY + "Example: /sbf set snow maxplayers 5");
							return false;
						} else if (args[1].equalsIgnoreCase("minplayers")) {
							player.sendMessage(ChatColor.GRAY + "Specify an arena and a positive number.");
							player.sendMessage(sbf + ChatColor.GRAY + "Example: /sbf set minplayers snow 2");
							return false;
						}

					} else if (args[0].equalsIgnoreCase("play") || args[0].equalsIgnoreCase("join")) {
						
						arenaName = args[1];
						
						if (player.hasPermission("sbf.play")) { // Make sure the player is allowed to play.
							
							if (this.getConfig().contains(arenaName)) {
								
								if (activeArenas.containsKey(arenaName)) {
									
									if (isPlayerInArena(player)) { // Ensures a player can't join more than one arena.
										
										player.sendMessage(sbf + ChatColor.RED + "You are alerady in an arena!");
										return false;
										
									} else if (activeArenas.get(arenaName).gameStarting) {
										
										if (activeArenas.get(arenaName).participants.size() >= this.getConfig().getInt(arenaName + ".Maximum Players")) {
											// Checks if an arena is full.
											
											if (player.hasPermission("sbf.full")) { // This means they can join full arenas (donator perk).
												
												activeArenas.get(arenaName).startGame(player, arenaName, false);
												return true;
												
											} else {
												
												player.sendMessage(sbf + ChatColor.RED
														+ "The arena is full, please try another arena.");
												return false;
												
											}
											
										} else {
											
											activeArenas.get(arenaName).startGame(player, arenaName, false);
											return true;
											
										}
										
									} else if (activeArenas.get(arenaName).gameActive) { // We can't join a match mid-game.
										
										player.sendMessage(sbf + ChatColor.RED
												+ "Game has already started, please try another arena.");
										return false;
										
									}
									
								} else { // We are good to go! join the arena and go through the start process.
									
									activeArenas.put(arenaName, new Arena(arenaName));
									activeArenas.get(arenaName).startGame(player, arenaName, false);
									return true;
									
								}
								
							} else {
								
								player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
								return false;
								
							}
							
						} else {
							
							player.sendMessage(sbf + ChatColor.RED + "You do not have permission to play!");
							return false;
							
						}
						
					} else if (args[0].equalsIgnoreCase("create") && player.hasPermission("sbf.create")) { // Allows a creator to set
																																											// Config settings from within the game.
						
						arenaName = args[1];
						
						if (this.getConfig().contains(arenaName)) { // Avoids duplicate arena names.
							
							player.sendMessage(sbf + ChatColor.RED + "The specified arena already exists.");
							return false;
							
						} else {
							
							this.getConfig().set(arenaName, ""); // Default values, that can be set via commands or editing the yml file.
							this.getConfig().set(arenaName + ".Lives", 3);
							this.getConfig().set(arenaName + ".Maximum Players", 10);
							this.getConfig().set(arenaName + ".Minimum Players", 4);
							this.getConfig().set(arenaName + ".Time", 10);
							this.getConfig().set(arenaName + ".Unlimited", "true");
							this.getConfig().set(arenaName + ".Snowballs", 1);
							this.getConfig().set(arenaName + ".Speed", 0);
							this.getConfig().set(arenaName + ".Jump", 0);
							this.saveConfig();
							player.sendMessage(sbf + ChatColor.GREEN + "The arena " + arenaName + " has been created.");
							return true;
						}

					} else if (args[0].equalsIgnoreCase("start")) {
						
						arenaName = args[1];
						
						if (activeArenas.containsKey(arenaName)) {
							
							int time = activeArenas.get(arenaName).time;
							
							if (time < timeToStart && timeToStart != 0) { // If the game hasn't been started, force start it now.
								
								player.sendMessage(sbf + ChatColor.YELLOW + "The game has been force started.");
								activeArenas.get(arenaName).startGame(player, arenaName, true);
								return true;
								
							} else {
								
								player.sendMessage(sbf + ChatColor.YELLOW
										+ "The game has skipped the wait timer and has started.");
								
							}
							
						} else { // We can't start an empty match - avoids infinite matches.
							
							player.sendMessage(sbf + ChatColor.RED + "The specified arena has no players.");
							
						}
						
					} else if (args[0].equalsIgnoreCase("stop")) { // ends a match prematurely.
						
						if (player.hasPermission("sbf.stop")) {
							
							arenaName = args[1];
							
							if (activeArenas.containsKey(arenaName)) { // makes sure this arena is active to be stopped.
								
								Bukkit.getScheduler().cancelTask(activeArenas.get(arenaName).gameTimer); // End this specific timer.
								
								for (Player p : Bukkit.getOnlinePlayers()) {
									
									if (isPlayerInArena(p)) { // Goes through the removal process on each member of the match.
										
										activeArenas.get(arenaName).participants.remove(p);
										p.teleport(originalLocations.get(p.getName()));
										originalInventory.get(p.getName()).Load();
										originalInventory.remove(p.getName());
										originalLocations.remove(p.getName());
										p.setMaxHealth(20D);
										p.setHealth(20D);
										p.removePotionEffect(PotionEffectType.SPEED);
										p.removePotionEffect(PotionEffectType.JUMP);
										p.sendMessage(sbf + ChatColor.GREEN + "You have been removed from the game!");
									
									} else {
										
										continue;
										
									}
								}
								
								player.sendMessage(sbf + ChatColor.YELLOW + "You have stopped the arena.");
								activeArenas.remove(arenaName);
								
							} else {
								player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
							}
							
						} else {
							player.sendMessage(sbf + ChatColor.RED + "You do not have permission to stop an arena.");
						}
						
					} else {
						player.sendMessage(sbf + ChatColor.GRAY + "Invalid Command!");
					}

				} else if (length == 3) {
					
					if (args[0].equalsIgnoreCase("set") && player.hasPermission("sbf.set.all")) {
						
						if (args[1].equalsIgnoreCase("center")) {
							
							arenaName = args[2];
							Location center = player.getLocation();
							Double xCoordinate = center.getX();
							Double yCoordinate = center.getY();
							Double zCoordinate = center.getZ();
							String worldName = player.getWorld().getName();
							
							if (this.getConfig().contains(arenaName)) { // Sets an arena spawn point to your current coordinates.
								
								Arena.setArenaConfig(arenaName, xCoordinate, yCoordinate, zCoordinate, worldName);
								player.sendMessage(sbf + ChatColor.GREEN + "The center of " + arenaName
										+ " has been set at your coordinates.");
								
								this.saveConfig();

								return true;
								
							} else {
								
								player.sendMessage(sbf + ChatColor.RED + "No such arena exists!");
								return false;

							}
							
						} else {
							
							player.sendMessage(sbf + ChatColor.RED
									+ "Specify both an arena and an argument along with your command.");
							
							return false;
							
						}
						
					} else if (args[0].equalsIgnoreCase("set") && !player.hasPermission("sbf.set.all")) {
						
						player.sendMessage(sbf + ChatColor.RED + "You do not have permission to set configurations!");
						return false;

					} else {
						
						player.sendMessage(sbf + ChatColor.RED + "Invalid Command!");
						return false;
						
					}

				} else if (length == 4) {
					
					if (args[0].equalsIgnoreCase("set")) {
						
						if (player.hasPermission("sbf.set.all")) {
							
							if (args[1].equalsIgnoreCase("lives")) {
								
								arenaName = args[2];
								
								if (this.getConfig().contains(arenaName)) {
									
									try {
										
										int lives = Integer.parseInt(args[3]); // Makes sure we don't set our values as something other than a number.
										
										if (lives >= 1 && lives <= 10) {
											
											this.getConfig().set(arenaName + ".Lives", lives);
											this.saveConfig();
											player.sendMessage(sbf + ChatColor.GREEN + "You have set lives to " + lives
													+ " in " + arenaName);
											return true;
											
										} else {
											
											player.sendMessage(sbf + ChatColor.RED
													+ "You specified an unsupported number of lives.  Only use numbers between 1 and 10.");
											return false;
											
										}
									}

									catch (NumberFormatException nfe) {
										
										player.sendMessage(sbf + ChatColor.RED + "Invalid argument!"); // Avoids errors.
										return false;
										
									}
									
								} else {
									
									player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
									
								}
								
							} else if (args[1].equalsIgnoreCase("maxplayers")) {
								
								arenaName = args[2];
								
								if (this.getConfig().contains(arenaName)) {
									
									try {
										
										int maxPlayers = Integer.parseInt(args[3]);
										
										if (maxPlayers >= this.getConfig().getInt(arenaName + ".Minimum Players")) {
											
											this.getConfig().set(arenaName + ".Maximum Players", maxPlayers);
											this.saveConfig();
											player.sendMessage(sbf + ChatColor.GREEN + "You set the maximum players to "
													+ maxPlayers + " in " + arenaName);
											return true;

										} else {
											
											player.sendMessage(sbf + ChatColor.RED
													+ "Error! You tried to set max players lower than minimum players!");
											return false;
											
										}

									} catch (NumberFormatException nfe) {
										
										player.sendMessage(sbf + ChatColor.RED + "Invalid argument!");
										return false;
										
									}
									
								} else {
									player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
									return false;
								}
								
							} else if (args[1].equalsIgnoreCase("minplayers")) {
								
								arenaName = args[2];
								
								if (this.getConfig().contains(arenaName)) {
									
									try {
										
										int minPlayers = Integer.parseInt(args[3]);
										
										if (minPlayers > this.getConfig().getInt(arenaName + ".Maximum Players")) {
											player.sendMessage(sbf + ChatColor.RED
													+ "Error! You cannot have a higher value for minimum players than maximum players!");
											
										} else {
											
											this.getConfig().set(arenaName + ".Minimum Players", minPlayers);
											this.saveConfig();
											player.sendMessage(sbf + ChatColor.GREEN
													+ "You have set the minimum number of players to start a match to "
													+ minPlayers + " in " + arenaName);
											return true;
											
										}

									} catch (NumberFormatException nfe) {
										
										player.sendMessage(sbf + ChatColor.RED + "Invalid argument!");
										return false;
										
									}
									
								} else {
									
									player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
									return false;
									
								}
								
							} else if (args[1].equalsIgnoreCase("time")) {
								
								arenaName = args[2];
								
								if (this.getConfig().contains(arenaName)) {
									
									try {
										
										int time = Integer.parseInt(args[3]);
										
										if (time >= 1 && time <= 500) {
											
											this.getConfig().set(arenaName + ".Time", time);
											this.saveConfig();
											player.sendMessage(sbf + ChatColor.GREEN + "You have set the time to "
													+ time + " in " + arenaName);
											return true;
											
										} else {
											
											player.sendMessage(sbf + ChatColor.RED
													+ "You specified an unsupported amount of seconds.  Only use numbers between 1 and 500.");
											return false;
											
										}
										
									} catch (NumberFormatException nfe) {
										
										player.sendMessage(sbf + ChatColor.RED + "Invalid argument!");
										return false;

									}
									
								} else {
									
									player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
									return false;
									
								}

							} else if (args[1].equalsIgnoreCase("unlimited")) {
								
								arenaName = args[2];
								
								if (this.getConfig().contains(arenaName)) {
									
									if (args[3].equalsIgnoreCase("true") || args[3].equalsIgnoreCase("false")) {
										
										this.getConfig().set(arenaName + ".Unlimited", args[3].toLowerCase());
										this.saveConfig();
										player.sendMessage(sbf + ChatColor.GREEN
												+ "You have succesfully set unlimited snowballs to " + args[3] + " in "
												+ arenaName);
										
									} else {
										
										player.sendMessage(sbf + ChatColor.RED
												+ "This command only accepts arguments of true and false.");
										
									}
									
								} else {
									player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
								}
								
							} else if (args[1].equalsIgnoreCase("speed")) {
								
								arenaName = args[2];
								
								if (this.getConfig().contains(arenaName)) {
									
									try {
										int speed = Integer.parseInt(args[3]);
										
										if (speed >= 0 && speed <= 4) {
											
											this.getConfig().set(arenaName + ".Speed", speed);
											this.saveConfig();
											player.sendMessage(sbf + ChatColor.GREEN
													+ "You have succesfully set speed effect to " + speed);
											
										} else {
											
											player.sendMessage(sbf + ChatColor.RED
													+ "Invalid number!  Only accepts numbers between 0 and 4.");
											
										}

									} catch (NumberFormatException nfe) {
										
										player.sendMessage(sbf + ChatColor.RED + "Invalid argument!");

									}
									
								} else {
									player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
								}
								
							} else if (args[1].equalsIgnoreCase("snowballs")) {
								
								arenaName = args[2];
								
								if (this.getConfig().contains(arenaName)) {
									
									try {
										
										int snowballs = Integer.parseInt(args[3]);
										
										if (snowballs >= 0 && snowballs <= 999) {
											
											this.getConfig().set(arenaName + ".Snowballs", snowballs);
											this.saveConfig();
											player.sendMessage(sbf + ChatColor.GREEN
													+ "You have succesfully set starting snowballs to " + snowballs);
											
										} else {
											player.sendMessage(sbf + ChatColor.RED
													+ "Invalid number of snowballs!  Only specify numbers between 0 and 999.");
										}
										
									} catch (NumberFormatException nfe) {
										player.sendMessage(sbf + ChatColor.RED
												+ "Invalid argument! This setting only accepts numerical values.");
									}
									
								} else {
									player.sendMessage(sbf + ChatColor.RED + "Invalid arena!");
								}
								
							} else if (args[1].equalsIgnoreCase("jump")) {
								
								arenaName = args[2];
								
								if (this.getConfig().contains(arenaName)) {
									
									try {
										
										int jump = Integer.parseInt(args[3]);
										
										if (jump >= 0 && jump <= 4) {
											
											this.getConfig().set(arenaName + ".Jump", jump);
											this.saveConfig();
											player.sendMessage(sbf + ChatColor.GREEN
													+ "You have succesfully set jump boost to" + jump);
											return true;
											
										} else {
											player.sendMessage(sbf + ChatColor.RED
													+ "Invalid jump boost! Only accepts numbers between 0 and 4.");
											return false;
										}
										
									} catch (NumberFormatException nfe) {
										player.sendMessage(sbf + ChatColor.RED + "Invalid argument!");
										return false;
										
									}
								}
							}
							
						} else {
							
							player.sendMessage(sbf + ChatColor.RED + "Insufficient permissions!");
							
						}
					} else {
						
						player.sendMessage(sbf + ChatColor.RED + "Invalid command!");
						
					}
				} else {
					
					player.sendMessage(sbf + ChatColor.RED + "Too many arguments!");
					return false;
					
				}
			}
		}
		return false;
	}

}
