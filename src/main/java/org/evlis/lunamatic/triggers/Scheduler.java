package org.evlis.lunamatic.triggers;

import io.papermc.paper.world.MoonPhase;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.evlis.lunamatic.GlobalVars;
import org.evlis.lunamatic.utilities.PlayerMessage;
import org.evlis.lunamatic.utilities.ResetFlags;
import org.evlis.lunamatic.utilities.TotoroDance;
import org.evlis.lunamatic.utilities.LangManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class Scheduler {
    private LangManager getTranslationManager() {
        return LangManager.getInstance(); // Always fetch the latest instance
    }

    public void GetOmens(Plugin plugin) {
        GlobalRegionScheduler globalRegionScheduler = plugin.getServer().getGlobalRegionScheduler();
        // generate new dice
        Random r = new Random();
        // get methods for Harvest moon
        TotoroDance totoroDance = new TotoroDance();
        // initialize logger
        Logger logger = plugin.getLogger();

        globalRegionScheduler.runAtFixedRate(plugin, (t)-> {
            for (World world : Bukkit.getWorlds()) {
                // Check if the world has active players
                List<Player> playerList = world.getPlayers();
                if (playerList.isEmpty()) {
                    continue; // Skip worlds with no active players
                }
                if (GlobalVars.disabledWorlds.contains(world.getName())) {
                    continue; // Skip worlds on the disabled list
                }

                long time = world.getTime();
                // Check if it's the start of the day (0 ticks, 6am)
                if (time >= 0 && time < 20) {
                    if (GlobalVars.debug) {
                        logger.info(getTranslationManager().getTranslation("sched_daydef_reset"));
                    }
                    // Reset defaults every dawn
                    ResetFlags.resetAll();
                    ResetFlags.resetTickSpeed(world);

                    // get the moon phase tonight
                    @NotNull MoonPhase moonPhase = world.getMoonPhase();
                    // handle debugging flag
                    if (moonPhase == MoonPhase.FULL_MOON && GlobalVars.fullMoonEnabled) {
                        // Do a dice roll to check if we're getting a harvest moon?
                        int chance = r.nextInt(GlobalVars.harvestMoonDieSides);
                        if (chance == 0 && GlobalVars.harvestMoonEnabled) {
                            GlobalVars.harvestMoonToday = true;
                            PlayerMessage.Send(playerList, getTranslationManager().getTranslation("harvest_moon_tonight"), NamedTextColor.GOLD);
                        } else {
                            PlayerMessage.Send(playerList, getTranslationManager().getTranslation("full_moon_tonight"), NamedTextColor.YELLOW);
                        }
                    } else if (moonPhase == MoonPhase.NEW_MOON && GlobalVars.newMoonEnabled) {
                        // Do a dice roll to check if the players are THAT unlucky..
                        int chance = r.nextInt(GlobalVars.bloodMoonDieSides);
                        if (chance == 0 && GlobalVars.bloodMoonEnabled) {
                            GlobalVars.bloodMoonToday = true;
                            PlayerMessage.Send(playerList, getTranslationManager().getTranslation("blood_moon_tonight"), NamedTextColor.DARK_RED);
                        } else {
                            PlayerMessage.Send(playerList, getTranslationManager().getTranslation("new_moon_tonight"), NamedTextColor.DARK_GRAY);
                        }
                    }
                }
                // Execute immediately after sunset starts
                if (time >= 12010 && time < 12030) {
                    if (GlobalVars.harvestMoonToday || GlobalVars.harvestMoonNow) {
                        if (GlobalVars.harvestMoonToday && !GlobalVars.harvestMoonNow) {
                            GlobalVars.harvestMoonNow = true;
                            // Ensure global var reset
                            globalRegionScheduler.runDelayed(plugin, (scheduledTask) -> {
                                ResetFlags.resetTickSpeed(world);
                            }, 24000 - (int)time);
                            globalRegionScheduler.runDelayed(plugin, (scheduledTask) -> ResetFlags.resetAll(), 24000 - (int)time);
                            totoroDance.setRandomTickSpeed(world, 30);
                            totoroDance.setClearSkies(world, (24000 - (int)time));
                            PlayerMessage.Send(playerList, getTranslationManager().getTranslation("grass_growing"), NamedTextColor.GOLD);
                        } else { // if for some reason both flags are still true, we have an invalid state
                            logger.warning(getTranslationManager().getTranslation("sched_invalid_harv"));
                            GlobalVars.harvestMoonToday = false;
                            GlobalVars.harvestMoonNow = false;
                        }
                    }
                }
                // Execute exactly at the start of night
                if (time >= 12980 && time < 13000) {
                    @NotNull MoonPhase moonPhase = world.getMoonPhase();
                    for (Player p : playerList) {
                        NightEffects.ApplyMoonlight(plugin, p, moonPhase, (24000 - (int)time));
                    }
                    if (GlobalVars.bloodMoonToday || GlobalVars.bloodMoonNow) {
                        if (GlobalVars.bloodMoonToday && !GlobalVars.bloodMoonNow) {
                            GlobalVars.bloodMoonNow = true;
                            // Ensure global var reset
                            globalRegionScheduler.runDelayed(plugin, (scheduledTask) -> ResetFlags.resetAll(), 24000 - (int)time);
                        } else { // if for some reason both flags are still true, we have an invalid state
                            logger.warning(getTranslationManager().getTranslation("sched_invalid_blood"));
                            GlobalVars.bloodMoonToday = false;
                            GlobalVars.bloodMoonNow = false;
                        }
                    }
                }
            }
        }, 1L, 20L); // Check every 20 ticks (1 second)
    }
}
