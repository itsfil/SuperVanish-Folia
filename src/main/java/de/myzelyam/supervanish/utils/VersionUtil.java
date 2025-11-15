package de.myzelyam.supervanish.utils;

import de.myzelyam.supervanish.SuperVanish;

import java.util.regex.Pattern;

public class VersionUtil {

    private final SuperVanish plugin;
    private final String minecraftVersion;
    private final int majorVersion;
    private final int minorVersion;

    public VersionUtil(SuperVanish plugin) {
        this.plugin = plugin;
        // saves versions in the format x.x.x (e.g. 1.20.1)
        minecraftVersion = plugin.getServer().getBukkitVersion().split(Pattern.quote("-"))[0];
        String[] parts = minecraftVersion.split("\\.");
        this.majorVersion = parts.length > 0 ? parseVersion(parts[0]) : 1;
        this.minorVersion = parts.length > 1 ? parseVersion(parts[1]) : 0;
    }

    private int parseVersion(String version) {
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int compareVersions(String version1, String version2) {
        String[] levels1 = version1.split("\\.");
        String[] levels2 = version2.split("\\.");
        int length = Math.max(levels1.length, levels2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < levels1.length ? parseVersion(levels1[i]) : 0;
            int v2 = i < levels2.length ? parseVersion(levels2[i]) : 0;
            int compare = Integer.compare(v1, v2);
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }

    public boolean isOneDotX(int majorRelease) {
        return majorVersion == 1 && minorVersion == majorRelease;
    }

    public boolean isOneDotXOrHigher(int majorRelease) {
        // Support Minecraft 2.x and higher
        if (majorVersion > 1) {
            return true;
        }
        // Check if current version is 1.x where x >= majorRelease
        return majorVersion == 1 && minorVersion >= majorRelease;
    }
}
