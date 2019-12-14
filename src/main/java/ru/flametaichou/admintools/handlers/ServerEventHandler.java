package ru.flametaichou.admintools.handlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import ru.flametaichou.admintools.ConfigHelper;

import java.util.Random;

public class ServerEventHandler {

    private long lastAutomessageTime = 0;
    private static Random random = new Random();

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.side == Side.SERVER) {
            if (MinecraftServer.getSystemTimeMillis() - lastAutomessageTime > ConfigHelper.automessageInterval * 1000) {
                lastAutomessageTime = MinecraftServer.getSystemTimeMillis();

                int messageNum = randomBetween(0, ConfigHelper.automessageStrings.length - 1);
                MinecraftServer.getServer().getConfigurationManager().sendChatMsg(new ChatComponentTranslation(ConfigHelper.automessageStrings[messageNum].replace("&", "§")));
            }
        }
    }

    private static int randomBetween(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }
}
