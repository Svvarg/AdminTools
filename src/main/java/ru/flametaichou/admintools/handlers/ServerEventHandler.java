package ru.flametaichou.admintools.handlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import ru.flametaichou.admintools.util.ConfigHelper;
import ru.flametaichou.admintools.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ServerEventHandler {

    private static final String linkString = "\",{\"text\":\"{LINK}\",\"color\":\"blue\",\"underlined\":\"true\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"Открыть ссылку\",\"color\":\"aqua\"}},\"clickEvent\":{\"action\":\"open_url\",\"value\":\"{LINK}\"}},\"";

    private long lastAutomessageTime = 0;
    private static Random random = new Random();

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.side == Side.SERVER) {
            if (MinecraftServer.getSystemTimeMillis() - lastAutomessageTime > ConfigHelper.automessageInterval * 1000) {
                String message = "";
                try {
                    lastAutomessageTime = MinecraftServer.getSystemTimeMillis();

                    int messageNum = randomBetween(0, ConfigHelper.automessageStrings.length - 1);

                    message = ConfigHelper.automessageStrings[messageNum];

                    message = message.replace("\"", "\\\"");
                    List<String> links = findLinks(message);
                    for (String link : links) {
                        message = message.replace(link, linkString.replace("{LINK}", link));
                    }
                    message = "[\"" + message + "\"]";
                    message = message.replace("&", "§");

                    IChatComponent component = IChatComponent.Serializer.func_150699_a(StatCollector.translateToLocal(message));
                    MinecraftServer.getServer().getConfigurationManager().sendChatMsg(component);
                } catch (Exception e) {
                    Logger.error("Error on sending automessage: " + e.getMessage());
                    Logger.error("Message text: " + message);
                }
            }
        }
    }

    private List<String> findLinks(String message) {
        List<Integer> linkPositions = new ArrayList<Integer>();

        List<String> links = new ArrayList<String>();

        String[] protocols = new String[]{"http://", "https://"};

        for (String protocol : protocols) {
            while (true) {
                int linkPos = message.indexOf(protocol);
                if (linkPos == -1) {
                    break;
                }
                if (!linkPositions.contains(linkPos)) {
                    linkPositions.add(linkPos);
                    String tempString = message.substring(linkPos, message.length());
                    String[] parts = tempString.split(" ");
                    if (parts.length > 0) {
                        if (!links.contains(parts[0])) {
                            links.add(parts[0]);
                        }
                    } else {
                        if (!links.contains(tempString.trim())) {
                            links.add(tempString.trim());
                        }
                    }
                } else {
                    break;
                }
            }
        }

        return links;
    }

    private static int randomBetween(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }
}
