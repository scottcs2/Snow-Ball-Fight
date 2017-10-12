package me.ssscrazy.snowball;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class EventListener implements Listener { // Whenever one of the events listed here is triggered in game, we process
																								 // The event and see if it matters to our plugin. If it does, we handle it accordingly.
	
	@EventHandler
	public void OnRespawn(PlayerRespawnEvent event) {
		
		Player player = event.getPlayer();
		
		if (Main.getMe().deadParticipants.contains(player)) { // Check if the player was in our game and died there.
			
			player.setMaxHealth(20D); // Give them full health
			player.setHealth(20D);
			event.setRespawnLocation(Main.getMe().originalLocations.get(player.getName())); // Move them to their original location.
			Main.getMe().originalLocations.remove(player.getName());

			event.getPlayer().sendMessage(
					Main.getMe().sbf + ChatColor.RED + "You lost all of your lives! Good luck next game!"); // They lost, so tell them.
			Main.getMe().originalInventory.get(player.getName()).Load(); // Give them their inventory.
			Main.getMe().originalInventory.remove(player.getName());
			Main.getMe().deadParticipants.remove(player);
			
		} else {
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST) // This means I let other minecraft functions deal with the player's death before I do.
	public void OnDeath(PlayerDeathEvent event) {
		
		Player player = event.getEntity();
		
		if (Main.getMe().isPlayerInArena(player)) {
			
			Main.getMe().deadParticipants.add(player); // Add them to the list of dead players.
			// This is necessary because we can't return the items to a dead player, or teleport them. We must wait for them to respawn first!
			
			String arenaToCheck = Main.getMe().nameOfArena(player);
			Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player)).participants.remove(player);
			Main.getMe().endGame(player, arenaToCheck);

		}
	}

	@EventHandler
	public void OnSnowballHit(EntityDamageByEntityEvent event) {
		
		if (!(event.getEntity() instanceof Player))
			return;
		
		Player hit = (Player) event.getEntity();
		
		if (event.getDamager() instanceof Snowball) {

			Snowball snowball = (Snowball) event.getDamager();
			
			if (!(snowball.getShooter() instanceof Player))
				return;
			
			Player shooter = (Player) snowball.getShooter();
			if (Main.getMe().isPlayerInArena(hit)) {
				
				event.setCancelled(true);
				shooter.setLevel(shooter.getLevel() + 1); // A rudimentary scoreboard, showing how many people you have hit.
				hit.setHealth(hit.getHealth() - 2D);
				
				if (hit.getHealth() == 0D) {
					
					shooter.sendMessage(
							Main.getMe().sbf + ChatColor.GREEN + "You have defeated " + hit.getName() + "!");
					hit.sendMessage(
							Main.getMe().sbf + ChatColor.RED + "You have been defeated by " + shooter.getName());
					
					for (Player p : Bukkit.getOnlinePlayers()) {
						
						if (Main.getMe().isPlayerInArena(p) && p != hit && p != shooter) {
							
							p.sendMessage(ChatColor.GREEN + "" + hit + " has been eliminated by" + shooter);
							
						}
						
					}
					
				} else if (hit.getHealth() != 0D) {
					
					shooter.sendMessage(Main.getMe().sbf + ChatColor.GREEN + "You hit " + hit.getName() + "!");
					
					hit.sendMessage(
							Main.getMe().sbf + ChatColor.RED + "You have been hit by " + shooter.getName() + "!");
					
					hit.sendMessage(Main.getMe().sbf + ChatColor.YELLOW + "You have " + hit.getHealth() / 2
							+ " lives remaining.");

				}
				
			} else {
				return;
			}
			
		}
		
		if (!(event.getDamager() instanceof Snowball) && Main.getMe().isPlayerInArena(hit)) { // If someone gets hurt
																																												// By something other than a snowball, ignore it.
			
			event.setCancelled(true);
			
		}
	}

	@EventHandler
	public void OnFoodDecrease(FoodLevelChangeEvent event) { // Make sure we keep feeding our players. There will be no starving here.
		
		if (event.getEntity() instanceof Player) {
			
			Player player = (Player) event.getEntity();
			
			if (Main.getMe().isPlayerInArena(player)) {
				
				event.setFoodLevel(Integer.MAX_VALUE);
				
			} else {
				
				return;
				
			}
			
		} else {
			
			return;
			
		}
		
	}

	@EventHandler
	public void EntityDamage(EntityDamageEvent event) {
		
		if (event.getEntity() instanceof Player) {
			
			Player player = (Player) event.getEntity();
			DamageCause cause = event.getCause();
			
			if (Main.getMe().isPlayerInArena(player)) {
				
				if (cause != DamageCause.PROJECTILE) {
					
					event.setCancelled(true);

				} else if (cause == null) {
					
					event.setCancelled(true);
					
				}
				
			} else {
				return;
			}
			
		} else {
			return;
		}
	}

	@EventHandler
	public void HealthRegen(EntityRegainHealthEvent event) { // Cancel all health regen while in the arena. You have set lives, and 
																													// Shall not gain more.
		
		if (event.getEntity() instanceof Player) {
			
			Player player = (Player) event.getEntity();
			
			if (Main.getMe().isPlayerInArena(player)) {
				
				event.setCancelled(true);
				
			} else {
				return;
			}
			
		} else {
			return;
		}
	}

	@EventHandler
	public void PlayerLeave(PlayerQuitEvent event) { // This is the most annoying to handle, if someone prematurely leaves.
																										// If there is no server lag, we can handle this and restore their items and position.
																									// known caveat though, if the server lags, this may not be handled and the player will
																									// lose their items.
		
		Player player = event.getPlayer();
		
		if (Main.getMe().isPlayerInArena(player)) {
			
			player.teleport(Main.getMe().originalLocations.get(player.getName()));
			Main.getMe().originalInventory.get(player.getName()).Load();
			Main.getMe().originalInventory.remove(player);
			Main.getMe().originalLocations.remove(player);
			player.removePotionEffect(PotionEffectType.JUMP);
			player.removePotionEffect(PotionEffectType.SPEED);
			player.setMaxHealth(20D);
			player.setHealth(20D);
			
			if (Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player)).gameActive) {
				
				Main.getMe().endGame(player, Main.getMe().nameOfArena(player));
				
			} else if (Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player)).gameStarting) {
				
				Main.getMe().participants = Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player)).participants;
				int playersRemaining = 0;
				
				for (Player p : Main.getMe().participants) {
					
					playersRemaining = playersRemaining + 1;
					p.sendMessage(ChatColor.GREEN + player.getName() + " has left the match.");
					
				}
				
				if (playersRemaining <= 1) {
					
					Main.getMe().gameTimer = Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player)).gameTimer;
					Bukkit.getScheduler().cancelTask(Main.getMe().gameTimer);
					Main.getMe().activeArenas.remove(Main.getMe().nameOfArena(player));
					
				} else {
					
					Main.getMe().activeArenas.get(Main.getMe().nameOfArena(player)).participants.remove(player);
					
				}
			}
			
		}

	}

	@EventHandler
	public void OnThrow(ProjectileLaunchEvent event) {
		
		if (!(event.getEntity() instanceof Snowball))
			return;
		
		if (event.getEntity() == null)
			return;
		
		if (event.getEntity() instanceof Snowball) {
			
			if (event.getEntity().getShooter() instanceof Player) {
				
				Player shooter = (Player) event.getEntity().getShooter();
				
				if (Main.getMe().isPlayerInArena(shooter)) {
					
					if(Main.getMe().getConfig().getString(Main.getMe().nameOfArena(shooter) + ".Unlimited").equalsIgnoreCase("true")) {
						shooter.getInventory().addItem(new ItemStack(Material.SNOW_BALL, 1));
						
					} else {
						
						return;
						
					}
				}
				
			} else {
				
				return;
				
			}
		} else {
			
			return;
			
		}
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event) { // This stops players from using commands to cheat while in the match.
		
		Player player = event.getPlayer();
		
		if (Main.getMe().isPlayerInArena(player)) {
			
			if (event.getMessage().equalsIgnoreCase("/sbf") || event.getMessage().equalsIgnoreCase("/sbf help")
					|| event.getMessage().equalsIgnoreCase("/sbf join")
					|| event.getMessage().equalsIgnoreCase("/sbf quit")
					|| event.getMessage().equalsIgnoreCase("/sbf play")
					|| event.getMessage().equalsIgnoreCase("/sbf leave")
					|| event.getMessage().equalsIgnoreCase("/sbf start")
					|| event.getMessage().equalsIgnoreCase("/sbf stop")
					|| event.getMessage().equalsIgnoreCase("/sbf info") || player.isOp()) {
				
				return;
				
			} else {
				
				event.setCancelled(true);
				player.sendMessage(Main.getMe().sbf + ChatColor.RED
						+ "You cannot use commands except snowball fight commands while in the arena!");
				
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) { // We can't build or break blocks during a snow ball fight.
		
		Player player = event.getPlayer();
		
		if (Main.getMe().isPlayerInArena(player)) {
			
			event.setCancelled(true);
			player.sendMessage(Main.getMe().sbf + ChatColor.RED + "You cannot place blocks while in a snowball match!");
			
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) { // We can't build or break blocks during a snow ball fight.
		
		Player player = event.getPlayer();
		
		if (Main.getMe().isPlayerInArena(player)) {
			
			event.setCancelled(true);
			player.sendMessage(Main.getMe().sbf + ChatColor.RED + "You cannot break blocks while in a snowball match!");
			
		}

	}
}
