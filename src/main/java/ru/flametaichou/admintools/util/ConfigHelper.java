package ru.flametaichou.admintools.util;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import ru.flametaichou.admintools.AdminTools;

public class ConfigHelper {

    private static Configuration configuration;

    public static boolean debugMode = false;

    public static boolean automessageEnabled;
    public static int automessageInterval;
    public static String[] automessageStrings;

    public static void setupConfig(Configuration config) {
        if (configuration == null) {
            configuration = config;
        }
        try {
            config.load();
            automessageEnabled = config.getBoolean("AutomessageEnabled", "Automessage", false, "Enable automatic messages for players?");
            automessageInterval = config.getInt("AutomessageInterval", "Automessage", 600, 1,99999,"Automatic messages interval (in seconds).");
            automessageStrings = config.getStringList(
                    "AutomessageStrings",
                    "Automessage",
                    new String[]{
                            "&2Hint: &fthis is a first hint.",
                            "&2Hint: &fthis is a second hint.",
                            "&2Hint: &fthis is a third hint."
                    },
                    "Automatic messages list.");
        } catch (Exception e) {
            Logger.error("Error on loadinc config: " + e.getMessage());
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
        try {
            AdminTools.serverEventHandler.setChatFormatAutomessages(automessageStrings);
        }
        catch (Exception e) {
            Logger.error("On Convert Messages from config to ChatFormat " + e.getMessage());
        }
    }

    public static void addMessage(String message) {
        Property prop = configuration.get("Automessage", "AutomessageStrings", new String[]{});
        String[] newValue = new String[automessageStrings.length + 1];
        for (int i = 0; i < automessageStrings.length; i++) {
            newValue[i] = automessageStrings[i];
        }
        newValue[automessageStrings.length] = message;
        prop.set(newValue);
        configuration.save();
        automessageStrings = newValue;
        AdminTools.serverEventHandler.addMessageToAutomessageList(message);
    }

    public static void reloadConfig() {
        try {
            configuration.load();
            automessageEnabled = configuration.getBoolean("AutomessageEnabled", "Automessage", false, "Enable automatic messages for players?");
            automessageInterval = configuration.getInt("AutomessageInterval", "Automessage", 600, 1,99999,"Automatic messages interval (in seconds).");
            automessageStrings = configuration.getStringList("AutomessageStrings", "Automessage", new String[]{}, "Automatic messages list.");
            AdminTools.serverEventHandler.setChatFormatAutomessages(automessageStrings);
        } catch (Exception e) {
            Logger.error("Error on reloading config: " + e.getMessage());
        }
    }
}
