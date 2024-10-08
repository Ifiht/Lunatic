package org.evlis.lunamatic.triggers;

import io.papermc.paper.world.MoonPhase;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.evlis.lunamatic.utilities.PlayerMessage;

public class NightEffects {

    public static void ApplyMoonlight(Player player, MoonPhase moonPhase, Integer timeTilDawn) {
        if (moonPhase == MoonPhase.FULL_MOON) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, timeTilDawn, 0));
            PlayerMessage.Send(player, "You feel lucky!", NamedTextColor.DARK_GREEN);
        } else if (moonPhase == MoonPhase.NEW_MOON) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, timeTilDawn, 0));
            PlayerMessage.Send(player, "You feel wary...", NamedTextColor.DARK_PURPLE);
        }

    }
}
