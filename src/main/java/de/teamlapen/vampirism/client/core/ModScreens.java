package de.teamlapen.vampirism.client.core;

import de.teamlapen.vampirism.api.client.VIngameOverlays;
import de.teamlapen.vampirism.client.gui.overlay.*;
import de.teamlapen.vampirism.client.gui.screens.*;
import de.teamlapen.vampirism.client.gui.screens.taskboard.TaskBoardScreen;
import de.teamlapen.vampirism.core.ModMenus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.NotNull;

public class ModScreens {
    /**
     * Call on serial loading queue to avoid possible issues with parallel access
     */
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.HUNTER_TABLE.get(), HunterTableScreen::new);
        event.register(ModMenus.ALCHEMICAL_CAULDRON.get(), AlchemicalCauldronScreen::new);
        event.register(ModMenus.WEAPON_TABLE.get(), WeaponTableScreen::new);
        event.register(ModMenus.HUNTER_TRAINER.get(), HunterTrainerScreen::new);
        event.register(ModMenus.HUNTER_BASIC.get(), HunterBasicScreen::new);
        event.register(ModMenus.ALTAR_INFUSION.get(), AltarInfusionScreen::new);
        event.register(ModMenus.BLOOD_GRINDER.get(), BloodGrinderScreen::new);
        event.register(ModMenus.MINION.get(), MinionScreen::new);
        event.register(ModMenus.TASK_MASTER.get(), TaskBoardScreen::new);
        event.register(ModMenus.EXTENDED_POTION_TABLE.get(), PotionTableScreen::new);
        event.register(ModMenus.VAMPIRISM.get(), VampirismContainerScreen::new);
        event.register(ModMenus.ALCHEMICAL_TABLE.get(), AlchemyTableScreen::new);
        event.register(ModMenus.VAMPIRE_BEACON.get(), VampireBeaconScreen::new);
        event.register(ModMenus.REVERT_BACK.get(), InjectionChairRevertBackScreen::new);
    }

    @SuppressWarnings("removal")
    static void registerScreenOverlays(@NotNull RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, VIngameOverlays.FACTION_LEVEL_ID, VIngameOverlays.FACTION_LEVEL_ELEMENT);
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, VIngameOverlays.FACTION_RAID_BAR_ID, VIngameOverlays.FACTION_RAID_BAR_ELEMENT);
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, VIngameOverlays.BLOOD_BAR_ID, VIngameOverlays.BLOOD_BAR_ELEMENT);
        event.registerAboveAll(VIngameOverlays.ACTION_COOLDOWN_ID, VIngameOverlays.ACTION_COOLDOWN_ELEMENT);
        event.registerAboveAll(VIngameOverlays.ACTION_DURATION_ID, VIngameOverlays.ACTION_DURATION_ELEMENT);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VIngameOverlays.RAGE, new RageOverlay());
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VIngameOverlays.BAT, new BatOverlay());
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VIngameOverlays.DISGUISE, new DisguiseOverlay());
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VIngameOverlays.NEARBY_VAMPIRE, new NearbyVampireOverlay());
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VIngameOverlays.SUN, new SunOverlay());
    }
}
