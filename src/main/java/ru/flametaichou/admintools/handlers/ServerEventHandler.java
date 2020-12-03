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
import static net.minecraft.util.StringUtils.isNullOrEmpty;

public class ServerEventHandler {

    private static final String linkString = "\",{\"text\":\"{LINK}\",\"color\":\"blue\",\"underlined\":\"true\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"Открыть ссылку\",\"color\":\"aqua\"}},\"clickEvent\":{\"action\":\"open_url\",\"value\":\"{LINK}\"}},\"";

    private long nextAutomessageTime = 0;
    private static Random random = new Random();
    /*list of messages prepared for display in the chat*/
    public final List<String> automessagesList = new ArrayList<String>();


    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.side == Side.SERVER) {
            if ( MinecraftServer.getSystemTimeMillis() > nextAutomessageTime) {
                String message = "";
                try {
                    /* setting the time when the next message will appear */
                    nextAutomessageTime = MinecraftServer.getSystemTimeMillis() + ConfigHelper.automessageInterval * 1000;

                    if (this.automessagesList != null && !automessagesList.isEmpty()) {

                        int messageNum = randomBetween(0, automessagesList.size() - 1);

                        message = automessagesList.get(messageNum);
                        if (!isNullOrEmpty(message)) {
                            IChatComponent component = IChatComponent.Serializer.func_150699_a(StatCollector.translateToLocal(message));
                            MinecraftServer.getServer().getConfigurationManager().sendChatMsg(component);
                        }
                    }

                } catch (Exception e) {
                    Logger.error("Error on sending automessage: " + e.getMessage());
                    Logger.error("Message text: " + message);
                }
            }
        }
    }

    /**
     * Convert the specified messages from the config to those prepared for
     * display in the chat on a timer
     * @param originalMessages
     * @return number of successfully converted messages
     */
    public int setChatFormatAutomessages(String[] originalMessages) {
        this.automessagesList.clear();

        if (originalMessages == null || originalMessages.length == 0) {
            return 0;
        }

        int sz = originalMessages.length;
        for (int i = 0; i < sz; i++) {
            String original = originalMessages[i];
            addMessageToAutomessageList(original);
        }

        return this.automessagesList.size();//sucess converted messages
    }

    /**
     * Add one new message to the list displayed in the chat
     * @param original
     * @return
     */
    public boolean addMessageToAutomessageList(String original) {
        if (!isNullOrEmpty(original)) {
            String converted = convertMessage(original);
            if (!isNullOrEmpty(converted)) {
                this.automessagesList.add(converted);
                return true;
            }
        }
        return false;
    }

    /**
     * Converting the original message from the config to the chat format
     * @param original
     * @return
     */
    public String convertMessage(String original) {
        String message = "";
        try {
            message = original.replace("\"", "\\\"");
            List<String> links = findLinks(message);
            for (String link : links) {
                message = message.replace(link, linkString.replace("{LINK}", link));
            }
            message = "[\"" + message + "\"]";
            message = message.replace("&", "§");
        } catch (Exception e) {
            Logger.error("Error on convert automessage: " + e.getMessage());
            Logger.error("Message text: " + message + " Original: " + original);
        }
        return message;
    }

    private static List<String> findLinks(String message) {
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
