package cn.goindog.OpenMLauncher.game.download.Quilt;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;

public class QuiltDownloader {
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
    public void build(QuiltDownloadProfile profile) throws IOException, URISyntaxException {
        Thread vanillaDownload = new Thread(() -> {
            try {
                new VanillaDownloader().build(
                        new VanillaInstallProfile()
                                .setVersion(profile.getVanillaVersion())
                                .setVersionName(
                                        "quilt-loader-" + profile.getQuiltVersion() + "-" + profile.getVanillaVersion()
                                )
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        vanillaDownload.start();

        FileUtils.writeByteArrayToFile(
                new File(
                        System.getProperty("oml.gameDir")
                                + File.separator
                                + "versions"
                                + File.separator
                                + "quilt-loader-"
                                + profile.getQuiltVersion()
                                + "-"
                                + profile.getVanillaVersion()
                                + File.separator
                                + "installer.jar"
                ),
                IOUtils.toByteArray(
                        new URI("https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/0.9.1/quilt-installer-0.9.1.jar")
                )
        );
        cliInstall(
                new File(
                        System.getProperty("oml.gameDir")
                                + File.separator
                                + "versions"
                                + File.separator
                                + "quilt-loader-"
                                + profile.getQuiltVersion()
                                + "-"
                                + profile.getVanillaVersion()
                                + File.separator
                                + "installer.jar"
                ),
                profile
        );
    }

    public void cliInstall(File downloader,QuiltDownloadProfile profile) {
        StringBuilder installCommand = new StringBuilder();
        installCommand.append("java -jar \"")
                .append(downloader.getAbsolutePath())
                .append("\" install client ")
                .append(profile.getVanillaVersion())
                .append(" ")
                .append(profile.getQuiltVersion())
                .append(" --install-dir=")
                .append(System.getProperty("oml.gameDir"));

        try {
            Process process = Runtime.getRuntime().exec(installCommand.toString());
            String input = consumeInputStream(process.getInputStream());
            String error = consumeInputStream(process.getErrorStream());

            int code = process.waitFor();

            if (code == 0) {
                FileUtils.writeByteArrayToFile(
                        new File(
                                System.getProperty("oml.gameDir")
                                        + File.separator
                                        + "versions"
                                        + File.separator
                                        + "quilt-loader-"
                                        + profile.getQuiltVersion()
                                        + "-"
                                        + profile.getVanillaVersion()
                                        + File.separator
                                        + profile.getVanillaVersion()
                                        + ".json"
                        ),
                        IOUtils.toByteArray(
                                new URL("https://bmclapi2.bangbang93.com/version/" + profile.getVanillaVersion() + "/json")
                        )
                );
                JsonObject vanillaVersionJson = new Gson().fromJson(
                        FileUtils.readFileToString(
                                new File(
                                        System.getProperty("oml.gameDir")
                                                + File.separator
                                                + "versions"
                                                + File.separator
                                                + "quilt-loader-"
                                                + profile.getQuiltVersion()
                                                + "-"
                                                + profile.getVanillaVersion()
                                                + File.separator
                                                + profile.getVanillaVersion()
                                                + ".json"
                                ),
                                StandardCharsets.UTF_8
                        ),
                        JsonObject.class
                );
                JsonObject quiltVersionJson = new Gson().fromJson(
                        FileUtils.readFileToString(
                                new File(
                                        System.getProperty("oml.gameDir")
                                                + File.separator
                                                + "versions"
                                                + File.separator
                                                + "quilt-loader-"
                                                + profile.getQuiltVersion()
                                                + "-"
                                                + profile.getVanillaVersion()
                                                + File.separator
                                                + "quilt-loader-"
                                                + profile.getQuiltVersion()
                                                + "-"
                                                + profile.getVanillaVersion()
                                                + ".json"
                                ),
                                StandardCharsets.UTF_8
                        ),
                        JsonObject.class
                );
                JsonObject mergedJson = versionJsonMerge(quiltVersionJson, vanillaVersionJson);
                FileUtils.writeStringToFile(
                        new File(
                                System.getProperty("oml.gameDir")
                                        + File.separator
                                        + "versions"
                                        + File.separator
                                        + "quilt-loader-"
                                        + profile.getQuiltVersion()
                                        + "-"
                                        + profile.getVanillaVersion()
                                        + File.separator
                                        + "quilt-loader-"
                                        + profile.getQuiltVersion()
                                        + "-"
                                        + profile.getVanillaVersion()
                                        + ".json"
                        ),
                        mergedJson.toString(),
                        StandardCharsets.UTF_8
                );
            }
            fireWorkspaceStarted("Download Finish");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject versionJsonMerge(JsonObject forgeVersionJson, JsonObject vanillaVersionJson) {
        for (String key : vanillaVersionJson.keySet()) {
            if (!forgeVersionJson.has(key)) {
                forgeVersionJson.add(key, vanillaVersionJson.get(key));
            }
        }
        for (JsonElement element : vanillaVersionJson.getAsJsonArray("libraries")) {
            JsonArray libs = forgeVersionJson.getAsJsonArray("libraries");
            libs.add(element);
        }
        for (JsonElement element : vanillaVersionJson.getAsJsonObject("arguments").getAsJsonArray("game")) {
            forgeVersionJson.getAsJsonObject("arguments").getAsJsonArray("game").add(element);
        }
        if (forgeVersionJson.getAsJsonObject("arguments").has("jvm")) {
            for (JsonElement element : vanillaVersionJson.getAsJsonObject("arguments").getAsJsonArray("jvm")) {
                forgeVersionJson.getAsJsonObject("arguments").getAsJsonArray("jvm").add(element);
            }
        }
        forgeVersionJson.add("clientVersion", forgeVersionJson.get("inheritsFrom"));
        forgeVersionJson.remove("inheritsFrom");
        return forgeVersionJson;
    }

    private static String consumeInputStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = br.readLine()) != null) {
            System.out.println(s);
            sb.append(s);
        }
        return sb.toString();
    }

    public JsonArray getQuiltVersion(String vanillaVersion) {

        try {
            JsonArray canInstallFabricVersions = new Gson().fromJson(
                    IOUtils.toString(
                            new URL("https://meta.quiltmc.org/v3/versions/game"),
                            StandardCharsets.UTF_8
                    ),
                    JsonArray.class
            );
            for (JsonElement element : canInstallFabricVersions) {
                String version = element.getAsJsonObject().get("version").getAsString();
                if (version.equals(vanillaVersion)) {
                    JsonArray loaderList = new Gson().fromJson(
                            IOUtils.toString(
                                    new URL("https://meta.quiltmc.org/v3/versions/loader"),
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
