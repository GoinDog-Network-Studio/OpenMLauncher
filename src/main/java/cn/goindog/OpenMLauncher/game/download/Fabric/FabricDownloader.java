package cn.goindog.OpenMLauncher.game.download.Fabric;

import cn.goindog.OpenMLauncher.events.GameEvents.DownloadFinishEvent;
import cn.goindog.OpenMLauncher.events.GameEvents.DownloadFinishEventListener;
import cn.goindog.OpenMLauncher.game.download.Vanilla.VanillaDownloader;
import cn.goindog.OpenMLauncher.game.download.Vanilla.VanillaInstallProfile;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;

public class FabricDownloader {
    private Collection listeners;
    public void addDownloadFinishListener(DownloadFinishEventListener listener) {
        if (listeners == null) {
            listeners = new HashSet();
        }
        listeners.add(listener);
    }

    public void removeDownloadFinishListener(DownloadFinishEventListener listener) {
        if (listeners == null) return;
        listeners.remove(listener);
    }

    protected void fireWorkspaceStarted(String type) {
        if (listeners == null) return;
        DownloadFinishEvent event = new DownloadFinishEvent(this, type);
        notifyListeners(event);
    }

    private void notifyListeners(DownloadFinishEvent event) {
        for (Object o : listeners) {
            DownloadFinishEventListener listener = (DownloadFinishEventListener) o;
            listener.DownloadFinishEvent(event);
        }
    }
    private String installerUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.0/fabric-installer-1.0.0.jar";

    /**
     * 下载Fabric版Minecraft
     * @param profile Fabric安装配置实例
     * @throws IOException 对文件进行IO操作
     */
    public void build(FabricInstallProfile profile) throws IOException {
        PrivateInstallDownload(profile);
    }

    private void PrivateInstallDownload(FabricInstallProfile profile) throws IOException {
        System.out.println("[INFO]Fabric Downloader: Downloading Installer");
        File installer = new File(
                System.getProperty("oml.gameDir")
                        + File.separator
                        + "versions"
                        + File.separator
                        + "fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion()
                        + File.separator
                        + "installer.jar"
        );
        FileUtils.writeByteArrayToFile(
                installer,
                IOUtils.toByteArray(new URL(installerUrl))
        );
        PrivateInstall(profile);
    }

    private void PrivateInstall(FabricInstallProfile profile) throws IOException {
        String installerPath =
                System.getProperty("oml.gameDir")
                        + File.separator
                        + "versions"
                        + File.separator
                        + "fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion()
                        + File.separator
                        + "installer.jar";
        StringBuilder command = new StringBuilder();
        command.append("java -jar \"")
                .append(installerPath)
                .append("\" ")
                .append("client ")
                .append("-dir ")
                .append(System.getProperty("oml.gameDir"))
                .append(" -mcversion ")
                .append(profile.getGameVersion())
                .append(" -loader ")
                .append(profile.getFabricVersion());
        createLauncherProfile();
        try {
            Process process = Runtime.getRuntime().exec(command.toString());
            String inStr = consumeInputStream(process.getInputStream());
            String errStr = consumeInputStream(process.getErrorStream());
            int proc = process.waitFor();
            if (proc == 0) {
                new VanillaDownloader().setGameDownloadDir(
                        System.getProperty("oml.gameDir")
                                + File.separator
                                + "versions"
                                + File.separator
                                + "fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion()
                                + File.separator
                ).build(
                        new VanillaInstallProfile()
                                .setVersion(profile.getGameVersion())
                                .setVersionName("fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion())
                );
                JsonObject vanillaJson = new Gson().fromJson(
                        FileUtils.readFileToString(
                                new File(
                                        System.getProperty("oml.gameDir")
                                                + File.separator
                                                + "versions"
                                                + File.separator
                                                + "fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion()
                                                + File.separator
                                                + profile.getGameVersion() + ".json"
                                ),
                                StandardCharsets.UTF_8
                        ),
                        JsonObject.class
                );
                JsonObject fabricJson = new Gson().fromJson(
                        FileUtils.readFileToString(
                                new File(
                                        System.getProperty("oml.gameDir")
                                                + File.separator
                                                + "versions"
                                                + File.separator
                                                + "fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion()
                                                + File.separator
                                                + "fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion() + ".json"
                                ),
                                StandardCharsets.UTF_8
                        ),
                        JsonObject.class
                );
                JsonObject merge = mergeVersionJson(fabricJson, vanillaJson);
                merge.add(
                        "clientVersion",
                        merge.get("inheritsFrom")
                );
                merge.remove("inheritsFrom");
                FileUtils.writeStringToFile(
                        new File(
                                System.getProperty("oml.gameDir")
                                        + File.separator
                                        + "versions"
                                        + File.separator
                                        + "fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion()
                                        + File.separator
                                        + "fabric-loader-" + profile.getFabricVersion() + "-" + profile.getGameVersion() + ".json"
                        ),
                        merge.toString(),
                        StandardCharsets.UTF_8
                );
            }
            fireWorkspaceStarted("Download Finish");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject mergeVersionJson(JsonObject fabricJson, JsonObject vanillaJson) {

        for (String key : vanillaJson.keySet()) {
            if (!fabricJson.has(key)) {
                fabricJson.add(key, vanillaJson.get(key));
            }
        }
        for (JsonElement element : vanillaJson.getAsJsonArray("libraries")) {
            fabricJson.getAsJsonArray("libraries").add(element);
        }
        for (JsonElement element : vanillaJson.getAsJsonObject("arguments").getAsJsonArray("game")) {
            fabricJson.getAsJsonObject("arguments").getAsJsonArray("game").add(element);
        }
        if (
                fabricJson.getAsJsonObject("arguments").has("jvm")
                        &&
                        vanillaJson.getAsJsonObject("arguments").has("jvm")
        ) {
            for (JsonElement element : vanillaJson.getAsJsonObject("arguments").getAsJsonArray("jvm")) {
                fabricJson.getAsJsonObject("arguments").getAsJsonArray("jvm").add(element);
            }
        }
        return fabricJson;
    }

    private static void createLauncherProfile() throws IOException {
        JsonObject obj = new Gson().fromJson("{}", JsonObject.class);
        JsonObject profilesObj = new Gson().fromJson("{}", JsonObject.class);
        JsonObject profileDefault = new Gson().fromJson("{}", JsonObject.class);
        profileDefault.addProperty("gameDir", System.getProperty("oml.gameDir"));
        profileDefault.addProperty("lastVersionId", "1.20.1");
        profileDefault.addProperty("name", "(Default)");
        profilesObj.add("(Default)", profileDefault);
        obj.add("profile", profilesObj);
        String profileDir = System.getProperty("oml.gameDir") + File.separator + "launcher_profiles.json";
        FileUtils.writeStringToFile(new File(profileDir), profilesObj.toString(), StandardCharsets.UTF_8);
    }

    private static String consumeInputStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = br.readLine()) != null) {
            System.out.println("[INFO][by Fabric Installer]Fabric Downloader: " + s);
            sb.append(s);
        }
        return sb.toString();
    }

    public JsonArray getFabricVersion(String vanillaVersion) {
        try {
            JsonArray canInstallFabricVersions = new Gson().fromJson(
                    IOUtils.toString(
                            new URL("https://meta.fabricmc.net/v2/versions/game"),
                            StandardCharsets.UTF_8
                    ),
                    JsonArray.class
            );
            for (JsonElement element : canInstallFabricVersions) {
                String version = element.getAsJsonObject().get("version").getAsString();
                if (version.equals(vanillaVersion)) {
                    JsonArray loaderList = new Gson().fromJson(
                            IOUtils.toString(
                                    new URL("https://meta.fabricmc.net/v2/versions/loader"),
                                    StandardCharsets.UTF_8
                            ),
                            JsonArray.class
                    );
                    JsonArray fabricVersionList = new JsonArray();
                    for (JsonElement loader : loaderList) {
                        String loaderVersion = loader.getAsJsonObject().get("version").getAsString();
                        fabricVersionList.add(loaderVersion);
                    }
                    return fabricVersionList;
                } else {
                    if (canInstallFabricVersions.asList().indexOf(element) == canInstallFabricVersions.size() - 1) {
                        break;
                    }
                }
            }
            return new JsonArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
