package org.evlis.lunamatic.utilities;

import org.bukkit.World;
import org.evlis.lunamatic.GlobalVars;

public class ResetFlags {

    static TotoroDance totoroDance = new TotoroDance();

    public static void resetAll() {
        GlobalVars.harvestMoonToday = false;
        GlobalVars.harvestMoonNow = false;
        GlobalVars.bloodMoonToday = false;
        GlobalVars.bloodMoonNow = false;
    }

    public static void resetTickSpeed(World world) {
        totoroDance.setRandomTickSpeed(world, 3);
    }
}
