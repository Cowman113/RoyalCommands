package org.royaldev.royalcommands.rcommands;

import org.apache.commons.lang.StringUtils;
import org.royaldev.royalcommands.MessageColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.royaldev.royalcommands.RUtils;
import org.royaldev.royalcommands.RoyalCommands;
import org.royaldev.royalcommands.UnZip;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdPluginManager implements CommandExecutor {

    private RoyalCommands plugin;

    public CmdPluginManager(RoyalCommands instance) {
        plugin = instance;
    }

    /**
     * Gets the names of all plugins that depend on the specified plugin.
     * <p/>
     * This will not return plugins that are disabled.
     *
     * @param dep Plugin to find dependencies of
     * @return List of dependencies, may be empty - never null
     */
    private List<String> getDependedOnBy(Plugin dep) {
        return getDependedOnBy(dep.getName());
    }

    /**
     * Gets the names of all plugins that depend on the specified plugin.
     * <p/>
     * This will not return plugins that are disabled.
     *
     * @param name Plugin name to find dependencies of
     * @return List of dependencies, may be empty - never null
     */
    private List<String> getDependedOnBy(String name) {
        final List<String> dependedOnBy = new ArrayList<String>();
        for (Plugin pl : plugin.getServer().getPluginManager().getPlugins()) {
            if (pl == null) continue;
            if (!pl.isEnabled()) continue;
            PluginDescriptionFile pdf = pl.getDescription();
            if (pdf == null) continue;
            List<String> depends = pdf.getDepend();
            if (depends == null) continue;
            for (String depend : depends) if (name.equalsIgnoreCase(depend)) dependedOnBy.add(pl.getName());
        }
        return dependedOnBy;
    }

    private String getCustomTag(String name) {
        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("custom_plugin_tags");
        if (cs == null) return null;
        for (String key : cs.getKeys(false)) {
            if (!key.equalsIgnoreCase(name)) continue;
            return cs.getString(key);
        }
        return null;
    }

    public String updateCheck(String name, String currentVersion) throws Exception {
        String tag = getCustomTag(name);
        if (tag == null) tag = name.toLowerCase();
        String pluginUrlString = "http://dev.bukkit.org/server-mods/" + tag + "/files.rss";
        URL url = new URL(pluginUrlString);
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openConnection().getInputStream());
        doc.getDocumentElement().normalize();
        NodeList nodes = doc.getElementsByTagName("item");
        Node firstNode = nodes.item(0);
        if (firstNode.getNodeType() == 1) {
            Element firstElement = (Element) firstNode;
            NodeList firstElementTagName = firstElement.getElementsByTagName("title");
            Element firstNameElement = (Element) firstElementTagName.item(0);
            NodeList firstNodes = firstNameElement.getChildNodes();
            return firstNodes.item(0).getNodeValue();
        }
        return currentVersion;
    }

    public boolean onCommand(final CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("pluginmanager")) {
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            String subcmd = args[0];
            final PluginManager pm = plugin.getServer().getPluginManager();
            if (subcmd.equalsIgnoreCase("load")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.load")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide the name of the jar to load!");
                    return true;
                }
                File f = new File(plugin.getDataFolder().getParentFile() + File.separator + args[1]);
                if (!f.exists()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That file does not exist!");
                    return true;
                }
                if (!f.canRead()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Can't read that file!");
                    return true;
                }
                Plugin p;
                try {
                    p = pm.loadPlugin(f);
                    if (p == null) {
                        cs.sendMessage(MessageColor.NEGATIVE + "Could not load plugin: plugin was invalid.");
                        cs.sendMessage(MessageColor.NEGATIVE + "Make sure it ends with .jar!");
                        return true;
                    }
                    pm.enablePlugin(p);
                } catch (InvalidPluginException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That file is not a plugin!");
                    return true;
                } catch (UnknownDependencyException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Missing dependency: " + e.getMessage());
                    return true;
                } catch (InvalidDescriptionException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That plugin contained an invalid description!");
                    return true;
                }
                if (p.isEnabled())
                    cs.sendMessage(MessageColor.POSITIVE + "Loaded and enabled " + MessageColor.NEUTRAL + p.getName() + MessageColor.POSITIVE + " successfully.");
                else
                    cs.sendMessage(MessageColor.NEGATIVE + "Could not load and enable " + MessageColor.NEUTRAL + p.getName() + MessageColor.NEGATIVE + ".");
                return true;
            } else if (subcmd.equalsIgnoreCase("disable")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.disable")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide the name of the plugin to disable!");
                    return true;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such plugin!");
                    return true;
                }
                if (!p.isEnabled()) {
                    cs.sendMessage(MessageColor.NEUTRAL + p.getName() + MessageColor.NEGATIVE + " is already disabled!");
                }
                final List<String> depOnBy = getDependedOnBy(p);
                if (!depOnBy.isEmpty()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Could not unload " + MessageColor.NEUTRAL + p.getName() + MessageColor.NEGATIVE + " because it is depended on by the following:");
                    StringBuilder sb = new StringBuilder();
                    for (String dep : depOnBy) {
                        sb.append(MessageColor.NEUTRAL);
                        sb.append(dep);
                        sb.append(MessageColor.RESET);
                        sb.append(", ");
                    }
                    cs.sendMessage(sb.substring(0, sb.length() - 4)); // "&r, " = 4
                    return true;
                }
                pm.disablePlugin(p);
                if (!p.isEnabled())
                    cs.sendMessage(MessageColor.POSITIVE + "Disabled " + MessageColor.NEUTRAL + p.getName() + MessageColor.POSITIVE + " successfully!");
                else cs.sendMessage(MessageColor.NEGATIVE + "Could not disabled that plugin!");
                return true;
            } else if (subcmd.equalsIgnoreCase("enable")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.enable")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide the name of the plugin to enable!");
                    return true;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such plugin!");
                    return true;
                }
                if (p.isEnabled()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Plugin is already enabled!");
                    return true;
                }
                pm.enablePlugin(p);
                if (p.isEnabled())
                    cs.sendMessage(MessageColor.POSITIVE + "Successfully enabled " + MessageColor.NEUTRAL + p.getName() + MessageColor.POSITIVE + "!");
                else
                    cs.sendMessage(MessageColor.NEGATIVE + "Could not enable " + MessageColor.NEUTRAL + p.getName() + MessageColor.NEGATIVE + ".");
                return true;
            } else if (subcmd.equalsIgnoreCase("reload")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.reload")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide the name of the plugin to reload!");
                    return true;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such plugin!");
                    return true;
                }
                pm.disablePlugin(p);
                pm.enablePlugin(p);
                cs.sendMessage(MessageColor.POSITIVE + "Reloaded " + MessageColor.NEUTRAL + p.getName() + MessageColor.POSITIVE + ".");
                return true;
            } else if (subcmd.equalsIgnoreCase("update")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.update")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 3) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide the name of the plugin to update and its filename!");
                    return true;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such plugin!");
                    return true;
                }
                List<String> depOnBy = getDependedOnBy(p);
                if (!depOnBy.isEmpty()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Could not unload " + MessageColor.NEUTRAL + p.getName() + MessageColor.NEGATIVE + " because it is depended on by the following:");
                    StringBuilder sb = new StringBuilder();
                    for (String dep : depOnBy) {
                        sb.append(MessageColor.NEUTRAL);
                        sb.append(dep);
                        sb.append(MessageColor.RESET);
                        sb.append(", ");
                    }
                    cs.sendMessage(sb.substring(0, sb.length() - 4)); // "&r, " = 4
                    return true;
                }
                File f = new File(plugin.getDataFolder().getParentFile() + File.separator + args[2]);
                if (!f.exists()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That file does not exist!");
                    return true;
                }
                if (!f.canRead()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Can't read that file!");
                    return true;
                }
                pm.disablePlugin(p);
                try {
                    p = pm.loadPlugin(f);
                    if (p == null) {
                        cs.sendMessage(MessageColor.NEGATIVE + "Could not load plugin: plugin was invalid.");
                        cs.sendMessage(MessageColor.NEGATIVE + "Make sure it ends with .jar!");
                        return true;
                    }
                    pm.enablePlugin(p);
                } catch (InvalidPluginException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That file is not a plugin!");
                    return true;
                } catch (UnknownDependencyException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Missing dependency: " + e.getMessage());
                    return true;
                } catch (InvalidDescriptionException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That plugin contained an invalid description!");
                    return true;
                }
                cs.sendMessage(MessageColor.POSITIVE + "Updated " + MessageColor.NEUTRAL + p.getName() + MessageColor.POSITIVE + " successfully.");
                return true;
            } else if (subcmd.equalsIgnoreCase("reloadall")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.reloadall")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                for (Plugin p : pm.getPlugins()) {
                    pm.disablePlugin(p);
                    pm.enablePlugin(p);
                }
                cs.sendMessage(MessageColor.POSITIVE + "Reloaded all plugins!");
                return true;
            } else if (subcmd.equalsIgnoreCase("list")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.list")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                Plugin[] ps = pm.getPlugins();
                StringBuilder list = new StringBuilder();
                int enabled = 0;
                int disabled = 0;
                for (Plugin p : ps) {
                    String name = p.getName();
                    if (!p.isEnabled()) {
                        name = name + " (disabled)";
                        disabled += 1;
                    } else enabled += 1;
                    list.append(MessageColor.NEUTRAL);
                    list.append(name);
                    list.append(MessageColor.RESET);
                    list.append(", ");
                }
                cs.sendMessage(MessageColor.POSITIVE + "Plugins (" + MessageColor.NEUTRAL + enabled + ((disabled > 0) ? MessageColor.POSITIVE + "/" + MessageColor.NEUTRAL + disabled + " disabled" : "") + MessageColor.POSITIVE + "): " + list.substring(0, list.length() - 4));
                return true;
            } else if (subcmd.equalsIgnoreCase("info")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.info")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide the name of the plugin!");
                    return true;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such plugin!");
                    return true;
                }
                PluginDescriptionFile pdf = p.getDescription();
                if (pdf == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Can't get information from " + MessageColor.NEUTRAL + p.getName() + MessageColor.NEGATIVE + ".");
                    return true;
                }
                String version = pdf.getVersion();
                List<String> authors = pdf.getAuthors();
                String site = pdf.getWebsite();
                List<String> softDep = pdf.getSoftDepend();
                List<String> dep = pdf.getDepend();
                String name = pdf.getName();
                String desc = pdf.getDescription();
                if (name != null && !name.isEmpty())
                    cs.sendMessage(MessageColor.POSITIVE + "Name: " + MessageColor.NEUTRAL + name);
                if (version != null && !version.isEmpty())
                    cs.sendMessage(MessageColor.POSITIVE + "Version: " + MessageColor.NEUTRAL + version);
                if (site != null && !site.isEmpty())
                    cs.sendMessage(MessageColor.POSITIVE + "Site: " + MessageColor.NEUTRAL + site);
                if (desc != null && !desc.isEmpty())
                    cs.sendMessage(MessageColor.POSITIVE + "Description: " + MessageColor.NEUTRAL + desc.replaceAll("\r?\n", ""));
                if (authors != null && !authors.isEmpty())
                    cs.sendMessage(MessageColor.POSITIVE + "Author" + ((authors.size() > 1) ? "s" : "") + ": " + MessageColor.NEUTRAL + RUtils.join(authors, MessageColor.RESET + ", " + MessageColor.NEUTRAL));
                if (softDep != null && !softDep.isEmpty())
                    cs.sendMessage(MessageColor.POSITIVE + "Soft Dependencies: " + MessageColor.NEUTRAL + RUtils.join(softDep, MessageColor.RESET + ", " + MessageColor.NEUTRAL));
                if (dep != null && !dep.isEmpty())
                    cs.sendMessage(MessageColor.POSITIVE + "Dependencies: " + MessageColor.NEUTRAL + RUtils.join(dep, MessageColor.RESET + ", " + MessageColor.NEUTRAL));
                cs.sendMessage(MessageColor.POSITIVE + "Enabled: " + MessageColor.NEUTRAL + ((p.isEnabled()) ? "Yes" : "No"));
                return true;
            } else if (subcmd.equalsIgnoreCase("commands")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.commands")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide the name of the plugin!");
                    return true;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such plugin!");
                    return true;
                }
                Map<String, Map<String, Object>> commands = p.getDescription().getCommands();
                if (commands == null) {
                    cs.sendMessage(MessageColor.NEUTRAL + p.getName() + MessageColor.NEGATIVE + " has no registered commands.");
                    return true;
                }
                for (String command : commands.keySet()) {
                    Object odesc = commands.get(command).get("description");
                    String desc = (odesc != null) ? odesc.toString() : "";
                    cs.sendMessage(MessageColor.NEUTRAL + "/" + command + ((desc.equals("")) ? "" : MessageColor.POSITIVE + " - " + desc));
                }
                return true;
            } else if (subcmd.equalsIgnoreCase("help") || subcmd.equals("?")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.help")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                cs.sendMessage(MessageColor.POSITIVE + "RoyalCommands PluginManager Help");
                cs.sendMessage(MessageColor.POSITIVE + "================================");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " load [jar]" + MessageColor.POSITIVE + " - Loads and enables a new plugin");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " disable [plugin]" + MessageColor.POSITIVE + " - Disables an already loaded plugin");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " enable [plugin]" + MessageColor.POSITIVE + " - Enables a disabled plugin");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " reload [plugin]" + MessageColor.POSITIVE + " - Disables then enables a plugin");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " reloadall" + MessageColor.POSITIVE + " - Reloads every plugin");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " delete [jar]" + MessageColor.POSITIVE + " - Tries to delete the specified jar");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " update [plugin] [jar]" + MessageColor.POSITIVE + " - Disables the plugin and loads the new jar");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " commands [plugin]" + MessageColor.POSITIVE + " - Lists all registered commands and their description of a plugin");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " list" + MessageColor.POSITIVE + " - Lists all the plugins");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " info [plugin]" + MessageColor.POSITIVE + " - Displays information about a plugin");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " updatecheck [plugin] (tag)" + MessageColor.POSITIVE + " - Attempts to check for the newest version of a plugin; may not always work correctly");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " updatecheckall" + MessageColor.POSITIVE + " - Attempts to check for newest version of all plugins");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " download [tag] (recursive)" + MessageColor.POSITIVE + " - Attempts to download a plugin from BukkitDev using its tag - recursive can be \"true\" if you would like the plugin to search for jars in all subdirectories of an archive downloaded");
                cs.sendMessage("* " + MessageColor.NEUTRAL + "/" + label + " findtag [search]" + MessageColor.POSITIVE + " - Searches BukkitDev for a tag to use in download");
                return true;
            } else if (subcmd.equalsIgnoreCase("download")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.download")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide plugin tag!");
                    cs.sendMessage(MessageColor.NEGATIVE + "http://dev.bukkit.org/server-mods/" + MessageColor.NEUTRAL + "royalcommands" + MessageColor.NEGATIVE + "/");
                    return true;
                }
                final boolean recursive = args.length > 2 && args[2].equalsIgnoreCase("true");
                String customTag = getCustomTag(args[1]);
                final String tag = (customTag == null) ? args[1].toLowerCase() : customTag;
                final String commandUsed = label;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        cs.sendMessage(MessageColor.POSITIVE + "Getting download link...");
                        String pluginUrlString = "http://dev.bukkit.org/server-mods/" + tag + "/files.rss";
                        String file;
                        try {
                            URL url = new URL(pluginUrlString);
                            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openConnection().getInputStream());
                            doc.getDocumentElement().normalize();
                            NodeList nodes = doc.getElementsByTagName("item");
                            Node firstNode = nodes.item(0);
                            if (firstNode.getNodeType() == 1) {
                                Element firstElement = (Element) firstNode;
                                NodeList firstElementTagName = firstElement.getElementsByTagName("link");
                                Element firstNameElement = (Element) firstElementTagName.item(0);
                                NodeList firstNodes = firstNameElement.getChildNodes();
                                String link = firstNodes.item(0).getNodeValue();
                                URL dpage = new URL(link);
                                BufferedReader br = new BufferedReader(new InputStreamReader(dpage.openStream()));
                                String inputLine;
                                StringBuilder content = new StringBuilder();
                                while ((inputLine = br.readLine()) != null) content.append(inputLine);
                                br.close();
                                file = StringUtils.substringBetween(content.toString(), "<li class=\"user-action user-action-download\"><span><a href=\"", "\">Download</a></span></li>");
                            } else throw new Exception();
                        } catch (Exception e) {
                            cs.sendMessage(MessageColor.NEGATIVE + "Could not fetch download link! Either this plugin has no downloads, or you specified an invalid tag.");
                            cs.sendMessage(MessageColor.NEGATIVE + "Tag: http://dev.bukkit.org/server-mods/" + MessageColor.NEUTRAL + "plugin-name" + MessageColor.NEGATIVE + "/");
                            return;
                        }
                        BufferedInputStream bis;
                        try {
                            bis = new BufferedInputStream(new URL(file).openStream());
                        } catch (MalformedURLException e) {
                            cs.sendMessage(MessageColor.NEGATIVE + "The received download link was invalid!");
                            return;
                        } catch (IOException e) {
                            cs.sendMessage(MessageColor.NEGATIVE + "An internal input/output error occurred. Please try again.");
                            return;
                        }
                        Pattern p = Pattern.compile("https?://dev\\.bukkit\\.org/media/files[\\d/]+([\\w\\W]+)");
                        Matcher m = p.matcher(file);
                        m.find();
                        String fileName = m.group(1).trim();
                        cs.sendMessage(MessageColor.POSITIVE + "Creating temporary folder...");
                        File f = new File(System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString() + File.separator + fileName);
                        while (f.getParentFile().exists()) // make sure we get our own directory
                            f = new File(System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString() + File.separator + fileName);
                        if (!fileName.endsWith(".zip") && !fileName.endsWith(".jar")) {
                            cs.sendMessage(MessageColor.NEGATIVE + "The file wasn't a zip or jar file, so it was not downloaded.");
                            cs.sendMessage(MessageColor.NEGATIVE + "Filename: " + MessageColor.NEUTRAL + fileName);
                            return;
                        }
                        f.getParentFile().mkdirs();
                        BufferedOutputStream bos;
                        try {
                            bos = new BufferedOutputStream(new FileOutputStream(f));
                        } catch (FileNotFoundException e) {
                            cs.sendMessage(MessageColor.NEGATIVE + "The temporary download folder was not found. Make sure that " + MessageColor.NEUTRAL + System.getProperty("java.io.tmpdir") + MessageColor.NEGATIVE + " is writable.");
                            return;
                        }
                        int b;
                        cs.sendMessage(MessageColor.POSITIVE + "Downloading file " + MessageColor.NEUTRAL + fileName + MessageColor.POSITIVE + "...");
                        try {
                            while ((b = bis.read()) != -1) bos.write(b);
                            bos.flush();
                            bos.close();
                        } catch (IOException e) {
                            cs.sendMessage(MessageColor.NEGATIVE + "An internal input/output error occurred. Please try again.");
                            return;
                        }
                        if (fileName.endsWith(".zip")) {
                            cs.sendMessage(MessageColor.POSITIVE + "Decompressing zip...");
                            UnZip.decompress(f.getAbsolutePath(), f.getParent());
                        }
                        for (File fi : RUtils.listFiles(f.getParentFile(), recursive)) {
                            if (!fi.getName().endsWith(".jar")) continue;
//                          String extraFile = (f.getParent().equals(fi.getParent())) ? "" : fi.getParentFile().getName() + File.separator;
                            cs.sendMessage(MessageColor.POSITIVE + "Moving " + MessageColor.NEUTRAL + fi.getName() + MessageColor.POSITIVE + " to plugins folder...");
                            boolean s = fi.renameTo(new File(plugin.getDataFolder().getParentFile() + File.separator + fi.getName()));
                            if (!s)
                                cs.sendMessage(MessageColor.NEGATIVE + "Couldn't move " + MessageColor.NEUTRAL + fi.getName() + MessageColor.NEGATIVE + "!");
                        }
                        cs.sendMessage(MessageColor.POSITIVE + "Removing temporary folder...");
                        RUtils.deleteDirectory(f.getParentFile());
                        cs.sendMessage(MessageColor.POSITIVE + "Downloaded plugin. Use " + MessageColor.NEUTRAL + "/" + commandUsed + " load" + MessageColor.POSITIVE + " to enable it.");
                    }
                };
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);
                return true;
            } else if (subcmd.equalsIgnoreCase("updatecheckall")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.updatecheckall")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        for (Plugin p : pm.getPlugins()) {
                            String version = p.getDescription().getVersion();
                            if (version == null) continue;
                            String checked;
                            try {
                                checked = updateCheck(p.getName(), version);
                            } catch (Exception e) {
                                continue;
                            }
                            if (checked.contains(version)) continue;
                            cs.sendMessage(MessageColor.NEUTRAL + p.getName() + MessageColor.POSITIVE + " may have an update. C: " + MessageColor.NEUTRAL + version + MessageColor.POSITIVE + " N: " + MessageColor.NEUTRAL + checked);
                        }
                        cs.sendMessage(MessageColor.POSITIVE + "Finished checking for updates.");
                    }
                };
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);
                return true;
            } else if (subcmd.equalsIgnoreCase("updatecheck")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.updatecheck")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please provide the name of the plugin!");
                    return true;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such plugin!");
                    return true;
                }
                String tag = (args.length > 2) ? RoyalCommands.getFinalArg(args, 2) : p.getName();
                try {
                    tag = URLEncoder.encode(tag, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Tell the developer enc1.");
                    return true;
                }
                if (p == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such plugin!");
                    return true;
                }
                if (p.getDescription() == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Plugin has no description!");
                    return true;
                }
                String version = p.getDescription().getVersion();
                if (version == null) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Plugin has not set a version!");
                    return true;
                }
                try {
                    String checked = updateCheck(tag, version);
                    cs.sendMessage(MessageColor.POSITIVE + "Current version is " + MessageColor.NEUTRAL + version + MessageColor.POSITIVE + "; newest version is " + MessageColor.NEUTRAL + checked + MessageColor.POSITIVE + ".");
                } catch (Exception e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Could not check for update!");
                }
                return true;
            } else if (subcmd.equalsIgnoreCase("findtag") || subcmd.equalsIgnoreCase("search")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.findtag")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please specify a search term!");
                    return true;
                }
                String search = RoyalCommands.getFinalArg(args, 1);
                try {
                    search = URLEncoder.encode(search, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Tell the developer enc1.");
                    return true;
                }
                URL u;
                try {
                    u = new URL("http://dev.bukkit.org/search/?scope=projects&search=" + search);
                } catch (MalformedURLException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Malformed search term!");
                    return true;
                }
                BufferedReader br;
                try {
                    br = new BufferedReader(new InputStreamReader(u.openStream()));
                } catch (IOException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Internal input/output error. Please try again.");
                    return true;
                }
                String inputLine;
                StringBuilder content = new StringBuilder();
                try {
                    while ((inputLine = br.readLine()) != null) content.append(inputLine);
                } catch (IOException e) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Internal input/output error. Please try again.");
                    return true;
                }
                cs.sendMessage(MessageColor.POSITIVE + "Project name" + MessageColor.NEUTRAL + " - tag");
                for (int i = 0; i < 5; i++) {
                    String project = StringUtils.substringBetween(content.toString(), " row-joined-to-next\">", "</tr>");
                    String base = StringUtils.substringBetween(project, "<td class=\"col-search-entry\">", "</td>");
                    if (base == null) {
                        if (i == 0) cs.sendMessage(MessageColor.NEGATIVE + "No results found.");
                        return true;
                    }
                    Pattern p = Pattern.compile("<h2><a href=\"/server-mods/([\\W\\w]+)/\">([\\w\\W]+)</a></h2>");
                    Matcher m = p.matcher(base);
                    if (m == null) {
                        if (i == 0) cs.sendMessage(MessageColor.NEGATIVE + "No results found.");
                        return true;
                    }
                    m.find();
                    String name = m.group(2).replaceAll("</?\\w+>", "");
                    String tag = m.group(1);
                    int beglen = StringUtils.substringBefore(content.toString(), base).length();
                    content = new StringBuilder(content.substring(beglen + project.length()));
                    cs.sendMessage(MessageColor.POSITIVE + name + MessageColor.NEUTRAL + " - " + tag);
                }
                return true;
            } else if (subcmd.equalsIgnoreCase("delete")) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.pluginmanager.delete")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please specify the filename to delete!");
                    return true;
                }
                String toDelete = args[1];
                if (!toDelete.endsWith(".jar")) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please only specify jar files!");
                    return true;
                }
                if (toDelete.contains(File.separator)) {
                    cs.sendMessage(MessageColor.NEGATIVE + "Please don't try to leave the plugins directory!");
                    return true;
                }
                File f = new File(plugin.getDataFolder().getParentFile() + File.separator + toDelete);
                if (!f.exists()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "No such file!");
                    return true;
                }
                boolean success = f.delete();
                if (!success)
                    cs.sendMessage(MessageColor.NEGATIVE + "Could not delete " + MessageColor.NEUTRAL + f.getName() + MessageColor.POSITIVE + ".");
                else
                    cs.sendMessage(MessageColor.POSITIVE + "Deleted " + MessageColor.NEUTRAL + f.getName() + MessageColor.POSITIVE + ".");
                return true;
            } else {
                cs.sendMessage(MessageColor.NEGATIVE + "Unknown subcommand!");
                cs.sendMessage(MessageColor.NEGATIVE + "Try " + MessageColor.NEUTRAL + "/" + label + " help");
                return true;
            }
        }

        return false;
    }
}
