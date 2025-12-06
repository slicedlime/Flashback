package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.Utils;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.exporting.AsyncFileDialogs;
import com.moulberry.flashback.exporting.ExportJobQueue;
import com.moulberry.flashback.exporting.ExportSettings;
import com.moulberry.flashback.screen.select_replay.SelectReplayScreen;
import imgui.flashback.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class MainMenuBar {

    public static void render() {
        if (ImGui.beginMainMenuBar()) {
            renderInner();
            ImGui.endMainMenuBar();
        }
    }

    public static void renderInner() {
        FlashbackConfigV1 config = Flashback.getConfig();

        if (ImGui.beginMenu(I18n.get("flashback.menu.file") + "##File")) {
            if (ImGui.menuItem(I18n.get("flashback.menu.file.export_video") + "##ExportVideo")) {
                StartExportWindow.open();
            }
            if (ImGui.menuItem(I18n.get("flashback.menu.file.export_default", ExportJobQueue.count()) + "###QueueDefaults")) {
                config = StartExportWindow.getDefaultConfig();

                int numBitrate;
                if (config.internalExport.useMaximumBitrate) {
                    numBitrate = 0;
                } else {
                    numBitrate = StartExportWindow.stringToBitrate("20m");
                }

                VideoContainer[] containers = StartExportWindow.getSupportedContainers(config);
                if (config.internalExport.container == null || !Arrays.asList(containers).contains(config.internalExport.container)) {
                    config.internalExport.container = containers[0];
                }
                VideoCodec[] codecs = config.internalExport.container.getSupportedVideoCodecs(config.internalExport.transparentBackground);
                if (config.internalExport.videoCodec == null || !Arrays.asList(codecs).contains(config.internalExport.videoCodec)) {
                    config.internalExport.videoCodec = codecs[0];
                }

                String defaultName = StartExportWindow.getDefaultFilename(null, config.internalExport.container.extension(), config);
                String defaultExportPathString = config.internalExport.defaultExportPath;

                final ExportSettings settings;
                if (config.internalExport.container == VideoContainer.PNG_SEQUENCE) {
                    settings = StartExportWindow.getExportSettings(defaultName, config, defaultExportPathString, numBitrate);
                } else {
                    settings = StartExportWindow.getExportSettings(defaultName, config, Path.of(defaultExportPathString).resolve(defaultName).toString(), numBitrate);
                }

                if (settings != null) {
                    Utils.exportSequenceCount += 1;
                    ExportJobQueue.queuedJobs.add(settings);
                }
            }
            if (!ExportJobQueue.queuedJobs.isEmpty()) {
                String name = I18n.get("flashback.menu.file.export_queue", ExportJobQueue.count());
                if (ImGui.menuItem(name + "###QueuedJobs")) {
                    ExportQueueWindow.open();
                }
            }
            if (ImGui.menuItem(I18n.get("flashback.export_screenshot") + "##ExportScreenshot")) {
                ExportScreenshotWindow.open();
            }
            ImGui.separator();
            if (ImGui.menuItem(I18n.get("flashback.select_replay.open") + "##Open")) {
                Flashback.openReplayFromFileBrowser();
            }
            if (!config.internal.recentReplays.isEmpty()) {
                if (ImGui.beginMenu(I18n.get("flashback.open_recent_replay") + "##OpenRecentReplay")) {
                    Path replayFolder = Flashback.getReplayFolder();
                    for (String recentReplay : config.internal.recentReplays) {
                        Path path = Path.of(recentReplay);

                        if (Files.exists(path)) {
                            String display = path.toString();

                            try {
                                Path relative = replayFolder.relativize(path);
                                String relativeStr = relative.toString();
                                if (!relativeStr.contains("..")) {
                                    display = relativeStr;
                                }
                            } catch (Exception ignored) {}

                            if (ImGui.menuItem(display)) {
                                Flashback.openReplayWorld(path);
                                break;
                            }
                        }
                    }
                    ImGui.endMenu();
                }
            }
            if (ImGui.menuItem(I18n.get("flashback.exit_replay") + "##ExitReplay")) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level != null) {
                    minecraft.level.disconnect(Component.empty());
                }
                minecraft.disconnectWithProgressScreen();
                minecraft.setScreen(new SelectReplayScreen(new TitleScreen()));
            }
            ImGui.endMenu();
        }
        if (ImGui.menuItem(I18n.get("flashback.preferences") + "##Preferences")) {
            PreferencesWindow.open();
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.get("flashback.player_list") + "##PlayerList")) {
            toggleWindow("player_list");
        }
        if (ImGui.menuItem(I18n.get("flashback.movement") + "##Movement")) {
            toggleWindow("movement");
        }
        if (ImGui.menuItem(I18n.get("flashback.render_filter") + "##RenderFilter")) {
            toggleWindow("render_filter");
        }

        ImGui.separator();

        if (ImGui.menuItem(I18n.get("flashback.hide_replay_ui") + "##HideReplayUI")) {
            Minecraft.getInstance().options.hideGui = true;
        }
    }

    private static void toggleWindow(String windowName) {
        var openedWindows = Flashback.getConfig().internal.openedWindows;
        boolean playerListIsOpen = openedWindows.contains(windowName);
        if (playerListIsOpen) {
            openedWindows.remove(windowName);
        } else {
            openedWindows.add(windowName);
        }
        Flashback.getConfig().delayedSaveToDefaultFolder();
    }

}
