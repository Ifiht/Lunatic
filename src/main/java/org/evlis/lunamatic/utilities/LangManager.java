package org.evlis.lunamatic.utilities;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.evlis.lunamatic.GlobalVars;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LangManager {
    private static LangManager instance;

    private final File languageFolder;
    private final Map<String, FileConfiguration> langs = new HashMap<>();
    private final String defaultLanguage;
    private static Plugin plugin;
    private static Logger logger;

    public LangManager(File pluginFolder, String defaultLanguage) {
        this.languageFolder = new File(pluginFolder, "lang");
        this.defaultLanguage = GlobalVars.lang;

        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
        }
    }

    public static void initialize(Plugin plugin_inst, File pluginFolder, String defaultLanguage) {
        instance = new LangManager(pluginFolder, defaultLanguage);
        plugin = plugin_inst;
        // initialize logger
        logger = plugin.getLogger();
    }

    public static LangManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LangManager is not initialized. Call initialize() first.");
        }
        return instance;
    }

    public void loadTranslations() {
        File[] files = languageFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String language = file.getName().replaceAll("\\.v\\d+\\.yml$", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                langs.put(language, config);
            }
        }
    }


    public void saveDefaultTranslations() {
        // Pattern for filenames: en_US.v3.yml
        Pattern versionPattern = Pattern.compile(".*\\.v(\\d+)\\.yml$");

        try {
            // Get the plugin JAR file
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            JarFile jarFile = new JarFile(jarPath);

            // First, scan language folder and clean up old files
            File[] existingFiles = languageFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (existingFiles != null) {
                for (File existing : existingFiles) {
                    Matcher matcher = versionPattern.matcher(existing.getName());
                    if (!matcher.matches() || // No version number
                            Integer.parseInt(matcher.group(1)) < GlobalVars.lang_vers) { // Old version
                        existing.delete();
                        logger.info("Deleted outdated file: " + existing.getName());
                    }
                }
            }

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // Check for files in the /lang/ folder
                if (entry.getName().startsWith("lang/") && entry.getName().endsWith(".yml")) {
                    String fileName = entry.getName().replace("lang/", "");
                    File translationFile = new File(languageFolder, fileName);

                    boolean shouldCopy = false;

                    if (!translationFile.exists()) {
                        shouldCopy = true;
                    } else {
                        // Check version from filename
                        Matcher matcher = versionPattern.matcher(translationFile.getName());
                        if (!matcher.matches()) {
                            shouldCopy = true; // No version in filename = outdated
                        } else {
                            int version = Integer.parseInt(matcher.group(1));
                            if (version < GlobalVars.lang_vers) {
                                shouldCopy = true;
                            }
                        }
                    }

                    if (shouldCopy) {
                        try (InputStream inputStream = getClass().getResourceAsStream("/" + entry.getName())) {
                            if (inputStream != null) {
                                Files.copy(inputStream, translationFile.toPath());
                                logger.info("Updated translation file: " + fileName);
                            }
                        }
                    }
                }
            }

            jarFile.close();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public String getTranslation(String key) {
        return getTranslation(key, defaultLanguage);
    }

    public String getTranslation(String key, String language) {
        // Special handling for language version key
        if ("lang_ver".equals(key)) {
            // Extract version from filename (e.g. zh_CN.v3.yml)
            Pattern versionPattern = Pattern.compile(".*\\.v(\\d+)\\.yml$");
            for (String langFile : langs.keySet()) {
                if (langFile.startsWith(language)) {
                    Matcher matcher = versionPattern.matcher(langFile);
                    if (matcher.find()) {
                        return "v" + matcher.group(1);
                    }
                }
            }
            return "v1"; // Default version if not found
        }

        FileConfiguration languageConfig = langs.get(language);
        String translation = null;

        if (languageConfig != null) {
            translation = languageConfig.getString(key);
        }

        if (translation == null) {
            // Attempt fallback to default language
            FileConfiguration defaultConfig = langs.get(defaultLanguage);
            if (defaultConfig != null) {
                translation = defaultConfig.getString(key);
            }
        }

        if (translation == null) {
            // Final fallback: return a placeholder or hardcoded string
            logger.severe("Translation missing! Key> " + key + " in language: " + language);
            translation = "langErr: " + key;
        }

        return translation;
    }

    public boolean doesTranslationExist(String language) {
        if (langs.containsKey(language)) {
            return true;
        }

        File translationFile = new File(languageFolder, language + ".yml");
        return translationFile.exists();
    }
}
