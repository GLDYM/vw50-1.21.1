package com.sqzj.vw50.misc;

import net.minecraft.client.GuiMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class GuiMessageAttachment {

    private static final WeakHashMap<GuiMessage, GuiMessageExtraData> EXTRA_DATA = new WeakHashMap<>();
    private static final WeakHashMap<GuiMessage.Line, GuiMessageExtraData> LINE_DATA = new WeakHashMap<>();
    private static final WeakHashMap<GuiMessage, List<GuiMessage.Line>> MESSAGE_LINES = new WeakHashMap<>();

    public static void put(GuiMessage message, GuiMessageExtraData data) {
        EXTRA_DATA.put(message, data);
    }

    public static GuiMessageExtraData get(GuiMessage message) {
        return EXTRA_DATA.get(message);
    }

    public static void put(GuiMessage.Line line, GuiMessageExtraData data) {
        LINE_DATA.put(line, data);
    }

    public static void putLines(GuiMessage message, List<GuiMessage.Line> lines, GuiMessageExtraData data) {
        List<GuiMessage.Line> previousLines = MESSAGE_LINES.put(message, new ArrayList<>(lines));
        if (previousLines != null) {
            previousLines.forEach(LINE_DATA::remove);
        }
        lines.forEach(line -> LINE_DATA.put(line, data));
    }

    public static GuiMessageExtraData get(GuiMessage.Line line) {
        return LINE_DATA.get(line);
    }

    public static void remove(GuiMessage message) {
        EXTRA_DATA.remove(message);
        List<GuiMessage.Line> lines = MESSAGE_LINES.remove(message);
        if (lines != null) {
            lines.forEach(LINE_DATA::remove);
        }
    }

    public static void clearRepeatMarks() {
        EXTRA_DATA.forEach((message, extraData) -> extraData.canPlusOne = false);
    }

    public static void clear() {
        EXTRA_DATA.clear();
        LINE_DATA.clear();
        MESSAGE_LINES.clear();
    }

}
