package ru.flametaichou.admintools;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ATEventHandler {

    Map<String, Map<String, Integer>> playerMobsMap = new HashMap<String, Map<String, Integer>>();
    Map<String, Integer> deathMap = new HashMap<String, Integer>();

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            if (Objects.isNull(deathMap.get(player.getDisplayName()))) {
                deathMap.put(player.getDisplayName(), 1);
            } else {
                deathMap.put(player.getDisplayName(), deathMap.get(player.getDisplayName()) + 1);
            }

        } else if (Objects.nonNull(event.source.getEntity()) && event.source.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.source.getEntity();
            if (event.entityLiving instanceof EntityLiving) {
                String mobName = event.entityLiving.getCommandSenderName();

                if (Objects.isNull(playerMobsMap.get(player.getDisplayName()))) {
                    Map<String, Integer> mobsMap = new HashMap<String, Integer>();
                    mobsMap.put(mobName, 1);
                    playerMobsMap.put(player.getDisplayName(), mobsMap);
                } else {
                    Map<String, Integer> mobsMap = playerMobsMap.get(player.getDisplayName());
                    if (Objects.isNull(mobsMap.get(mobName))) {
                        mobsMap.put(mobName, 1);
                    } else {
                        mobsMap.put(mobName, mobsMap.get(mobName) + 1);
                    }
                    playerMobsMap.put(player.getDisplayName(), mobsMap);
                }
            }
        }
    }

}
