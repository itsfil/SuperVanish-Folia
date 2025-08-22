package de.myzelyam.supervanish.hooks;

import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.api.vanish.PostPlayerHideEvent;
import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Best-effort InvSee++ hook for silent chest/opening integration.
 * Uses reflection to avoid hard dependency mismatches at runtime.
 */
public class InvseePlusPlusHook extends PluginHook {

    private boolean errorLogged = false;
    private final Set<UUID> alreadyHiddenBeforeVanishing = new HashSet<>();

    public InvseePlusPlusHook(SuperVanish superVanish) {
        super(superVanish);
    }

    @Override
    public void onPluginEnable(Plugin plugin) {
        // Respect the config flag before doing any hook-specific work
        if (!superVanish.getSettings().getBoolean("HookOptions.EnableInvSeePlusPlusHook", true)) return;
        super.onPluginEnable(plugin);
        // no extra initialization required
    }

    private Object getApi() throws Exception {
        if (plugin == null) return null;
        // plugin.getApi()
        Method getApi = plugin.getClass().getMethod("getApi");
        return getApi.invoke(plugin);
    }

    private Boolean getSilentStatus(Object api, Player p) {
        if (api == null) return null;
        try {
            for (Method m : api.getClass().getMethods()) {
                String name = m.getName().toLowerCase();
                Class<?>[] params = m.getParameterTypes();
                if (name.contains("silent") && params.length == 1 &&
                        (params[0].isAssignableFrom(Player.class) || params[0].isAssignableFrom(UUID.class))) {
                    Object res = m.invoke(api, p);
                    if (res instanceof Boolean) return (Boolean) res;
                }
            }
        } catch (Throwable t) {
            if (!errorLogged) {
                superVanish.logException(t);
                errorLogged = true;
            }
        }
        return null;
    }

    private boolean setSilentStatus(Object api, Player p, boolean value) {
        if (api == null) return false;
        try {
            for (Method m : api.getClass().getMethods()) {
                String name = m.getName().toLowerCase();
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2 &&
                        (params[0].isAssignableFrom(Player.class) || params[0].isAssignableFrom(UUID.class)) &&
                        (params[1] == boolean.class || params[1] == Boolean.class) &&
                        name.contains("set") && name.contains("silent")) {
                    m.invoke(api, p, value);
                    return true;
                }
            }
            // fallback: method with (Player, boolean) and 'silent' anywhere
            for (Method m : api.getClass().getMethods()) {
                String name = m.getName().toLowerCase();
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2 &&
                        (params[0].isAssignableFrom(Player.class) || params[0].isAssignableFrom(UUID.class)) &&
                        (params[1] == boolean.class || params[1] == Boolean.class) &&
                        name.contains("silent")) {
                    m.invoke(api, p, value);
                    return true;
                }
            }
        } catch (Throwable t) {
            if (!errorLogged) {
                superVanish.logException(t);
                errorLogged = true;
            }
        }
        return false;
    }

    @EventHandler
    public void onVanish(PostPlayerHideEvent e) {
        try {
            Player p = e.getPlayer();
            Object api = getApi();
            Boolean current = getSilentStatus(api, p);
            if (current != null && current) {
                alreadyHiddenBeforeVanishing.add(p.getUniqueId());
            } else {
                if (!p.hasPermission("sv.silentchest")) return;
                setSilentStatus(api, p, true);
            }
        } catch (NoSuchMethodError | NoClassDefFoundError ex) {
            if (!errorLogged) {
                superVanish.logException(ex);
                errorLogged = true;
            }
        } catch (Exception ex) {
            if (!errorLogged) {
                superVanish.logException(ex);
                errorLogged = true;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReappear(PlayerShowEvent e) {
        try {
            Player p = e.getPlayer();
            Object api = getApi();
            if (!alreadyHiddenBeforeVanishing.remove(p.getUniqueId())) {
                setSilentStatus(api, p, false);
            }
        } catch (Throwable ex) {
            if (!errorLogged) {
                superVanish.logException(ex);
                errorLogged = true;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {
        try {
            Player p = e.getPlayer();
            Object api = getApi();
            if (superVanish.getVanishStateMgr().isVanished(p.getUniqueId())) {
                Boolean current = getSilentStatus(api, p);
                if (current != null && current) {
                    alreadyHiddenBeforeVanishing.add(p.getUniqueId());
                } else {
                    if (!p.hasPermission("sv.silentchest")) return;
                    setSilentStatus(api, p, true);
                }
            }
        } catch (Throwable ex) {
            if (!errorLogged) {
                superVanish.logException(ex);
                errorLogged = true;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        try {
            Player p = e.getPlayer();
            Object api = getApi();
            if (superVanish.getVanishStateMgr().isVanished(p.getUniqueId())) {
                if (!alreadyHiddenBeforeVanishing.remove(p.getUniqueId())) {
                    setSilentStatus(api, p, false);
                }
            }
        } catch (Throwable ex) {
            if (!errorLogged) {
                superVanish.logException(ex);
                errorLogged = true;
            }
        }
    }

    /**
     * Best-effort openPlayerInventory integration. Attempts to call invsee api methods
     * to open the target's inventory for vanished player. Returns true on success.
     */
    public boolean openPlayerInventory(Player vanished, Player target) {
        try {
            Object api = getApi();
            if (api == null) return false;
            // try common method patterns: getSpecialInventory(target, boolean) then openInventory(vanished, inventory)
            Object specialInv = null;
            for (Method m : api.getClass().getMethods()) {
                String name = m.getName().toLowerCase();
                Class<?>[] params = m.getParameterTypes();
                if (name.contains("special") && params.length == 2) {
                    try {
                        specialInv = m.invoke(api, target, true);
                        break;
                    } catch (Throwable ignored) {}
                }
            }
            // fallback: look for a method that returns an object inventory-like for a player
            if (specialInv == null) {
                for (Method m : api.getClass().getMethods()) {
                    if (m.getReturnType() != void.class && m.getParameterCount() == 1 &&
                            m.getParameterTypes()[0].isAssignableFrom(Player.class)) {
                        try {
                            Object res = m.invoke(api, target);
                            if (res != null) {
                                specialInv = res;
                                break;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
            // try to find 'openInventory' or 'open' on api
            for (Method m : api.getClass().getMethods()) {
                String name = m.getName().toLowerCase();
                Class<?>[] params = m.getParameterTypes();
                if ((name.contains("open") || name.contains("show")) && params.length == 2 &&
                        (params[0].isAssignableFrom(Player.class) || params[0].isAssignableFrom(vanished.getClass()))) {
                    try {
                        if (specialInv != null) {
                            m.invoke(api, vanished, specialInv);
                            return true;
                        } else {
                            // try (viewer, target) signature
                            if (params[1].isAssignableFrom(Player.class)) {
                                m.invoke(api, vanished, target);
                                return true;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            if (!errorLogged) {
                superVanish.logException(t);
                errorLogged = true;
            }
        }
        return false;
    }
}
