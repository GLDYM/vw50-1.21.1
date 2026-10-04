package com.sqzj.vw50.client;

import com.mojang.logging.LogUtils;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class E33ChatCompat {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STORE_CLASS = "com.niuqu.chatbubble.store.ChatMessageStore";
    private static final String CONFIG_CLASS = "com.niuqu.chatbubble.config.ChatBubbleConfig";
    private static final List<ComponentAttachment> COMPONENT_DATA = new ArrayList<>();
    private static final List<MessageAttachment> MESSAGE_DATA = new ArrayList<>();
    private static final List<RepeatAttachment> REPEAT_DATA = new ArrayList<>();
    private static final List<HitBox> HIT_BOXES = new ArrayList<>();
    private static volatile boolean e33ConfigResolved;
    private static Object e33EnabledValue;
    private static Method e33EnabledGetter;
    private static Object frameScreen;

    private E33ChatCompat() {
    }

    public static void onMessageDisplayed(GuiMessage message, GuiMessageExtraData data) {
        if (data.canPlusOne && !data.repeatText.isBlank()) {
            rememberRepeat(data.repeatText, data);
        }

        if (!data.isRedEnvelope && !data.isRedEnvelopeFinishNotice) return;
        remember(message.content(), data);
        mirrorSystemMessage(message, data);
    }

    public static void clearRepeatMarks() {
        REPEAT_DATA.clear();
    }

    public static void clearHitBoxes() {
        frameScreen = null;
        HIT_BOXES.clear();
    }

    public static void beginFrame(Object screen) {
        frameScreen = screen;
        HIT_BOXES.clear();
    }

    public static boolean isBubbleScreen(Object screen) {
        return screen != null && screen.getClass().getName().equals("com.niuqu.chatbubble.render.ChatBubbleScreen");
    }

    public static boolean isChatHudShifted() {
        return isE33ChatEnabled();
    }

    public static GuiMessageExtraData getMessageData(Object message) {
        Iterator<MessageAttachment> messageIterator = MESSAGE_DATA.iterator();
        while (messageIterator.hasNext()) {
            MessageAttachment attachment = messageIterator.next();
            Object candidate = attachment.message().get();
            if (candidate == null) {
                messageIterator.remove();
            } else if (candidate == message) {
                return attachment.data();
            }
        }

        Component content = getMessageContent(message);
        if (content == null) return null;

        Iterator<ComponentAttachment> iterator = COMPONENT_DATA.iterator();
        while (iterator.hasNext()) {
            ComponentAttachment attachment = iterator.next();
            Component candidate = attachment.component().get();
            if (candidate == null) {
                iterator.remove();
            } else if (candidate == content) {
                return attachment.data();
            }
        }
        return null;
    }

    public static Component getMessageContent(Object message) {
        if (message == null) return null;
        try {
            return (Component) message.getClass().getMethod("content").invoke(message);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    public static boolean isLegacyEnvelopePlaceholder(Object message) {
        Component content = getMessageContent(message);
        if (content == null) return false;
        String label = Component.translatable("red_envelope.chat.card_title").getString();
        String text = content.getString();
        int senderEnd = text.indexOf("> ");
        String marker = " [" + label + "]";
        boolean oldCardLabel = senderEnd > 1 && text.startsWith(marker, senderEnd + 1)
            && text.substring(senderEnd + 1 + marker.length()).isBlank();
        int oldSenderEnd = text.indexOf('>');
        boolean oldSenderOnly = text.startsWith("<") && oldSenderEnd > 1
            && text.substring(oldSenderEnd + 1).isBlank();
        if (!oldCardLabel && !oldSenderOnly) return false;
        try {
            if (!Boolean.TRUE.equals(message.getClass().getMethod("isSystem").invoke(message))) return false;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
        return true;
    }

    public static GuiMessageExtraData getRepeatData(Object message) {
        Iterator<RepeatAttachment> iterator = REPEAT_DATA.iterator();
        while (iterator.hasNext()) {
            RepeatAttachment attachment = iterator.next();
            Object candidate = attachment.message().get();
            if (candidate == null) {
                iterator.remove();
            } else if (candidate == message) {
                return attachment.data();
            }
        }
        return null;
    }

    public static boolean recordHitBox(GuiGraphics graphics, Object screen, int left, int top, int width, int height,
                                       UUID envelopeId, String repeatText, boolean closeClaimList,
                                       double mouseX, double mouseY) {
        if (screen != frameScreen || width <= 0 || height <= 0) return false;
        HitBox hitBox = project(graphics, left, top, width, height, envelopeId, repeatText, closeClaimList);
        HIT_BOXES.add(hitBox);
        return hitBox.contains(mouseX, mouseY);
    }

    public static boolean isMouseOver(GuiGraphics graphics, int left, int top, int width, int height,
                                      double mouseX, double mouseY) {
        return project(graphics, left, top, width, height, null, null, false).contains(mouseX, mouseY);
    }

    public static boolean handleMouseClick(Object screen, double mouseX, double mouseY, int button) {
        if (button != 0 || screen != frameScreen) return false;
        for (int i = HIT_BOXES.size() - 1; i >= 0; i--) {
            HitBox hitBox = HIT_BOXES.get(i);
            if (!hitBox.contains(mouseX, mouseY)) continue;
            if (hitBox.closeClaimList()) {
                ClientRedEnvelopeManager.closeClaimList();
            } else if (hitBox.envelopeId() != null) {
                HookChatComponent.handleEnvelopeClick(hitBox.envelopeId());
            } else if (hitBox.repeatText() != null) {
                HookChatComponent.handleRepeatClick(hitBox.repeatText());
            }
            return true;
        }
        return false;
    }

    private static HitBox project(GuiGraphics graphics, int left, int top, int width, int height,
                                  UUID envelopeId, String repeatText, boolean closeClaimList) {
        Matrix4f matrix = graphics.pose().last().pose();
        Vector3f first = matrix.transformPosition(left, top, 0.0F, new Vector3f());
        Vector3f second = matrix.transformPosition(left + width, top + height, 0.0F, new Vector3f());
        return new HitBox(Math.min(first.x, second.x), Math.min(first.y, second.y),
            Math.max(first.x, second.x), Math.max(first.y, second.y), envelopeId, repeatText, closeClaimList);
    }

    private static void remember(Component component, GuiMessageExtraData data) {
        COMPONENT_DATA.removeIf(attachment -> {
            Component candidate = attachment.component().get();
            return candidate == null || candidate == component;
        });
        COMPONENT_DATA.add(new ComponentAttachment(new WeakReference<>(component), data));
    }

    private static void remember(Object message, GuiMessageExtraData data) {
        MESSAGE_DATA.removeIf(attachment -> {
            Object candidate = attachment.message().get();
            return candidate == null || candidate == message;
        });
        MESSAGE_DATA.add(new MessageAttachment(new WeakReference<>(message), data));
    }

    private static boolean hasMessage(Object message) {
        Iterator<MessageAttachment> iterator = MESSAGE_DATA.iterator();
        while (iterator.hasNext()) {
            Object candidate = iterator.next().message().get();
            if (candidate == null) {
                iterator.remove();
            } else if (candidate == message) {
                return true;
            }
        }
        return false;
    }

    private static boolean updateExistingEnvelope(UUID id, boolean finishNotice, GuiMessageExtraData data) {
        Iterator<MessageAttachment> iterator = MESSAGE_DATA.iterator();
        while (iterator.hasNext()) {
            MessageAttachment attachment = iterator.next();
            if (attachment.message().get() == null) {
                iterator.remove();
                continue;
            }
            GuiMessageExtraData existing = attachment.data();
            if (!id.equals(existing.redEnvelopeId) || existing.isRedEnvelopeFinishNotice != finishNotice) continue;
            existing.redEnvelopeSnapshot = data.redEnvelopeSnapshot;
            return true;
        }
        return false;
    }

    private static void rememberRepeat(String repeatText, GuiMessageExtraData data) {
        try {
            Class<?> store = Class.forName(STORE_CLASS);
            Object storedMessages = store.getMethod("getMessages").invoke(null);
            if (!(storedMessages instanceof List<?> messages)) return;
            for (int i = messages.size() - 1; i >= 0; i--) {
                Object candidate = messages.get(i);
                Component content = getMessageContent(candidate);
                if (content == null || !content.getString().trim().equals(repeatText)) continue;
                Object system = candidate.getClass().getMethod("isSystem").invoke(candidate);
                if (Boolean.TRUE.equals(system)) continue;
                REPEAT_DATA.removeIf(attachment -> {
                    Object previous = attachment.message().get();
                    return previous == null || attachment.data().repeatText.equals(repeatText);
                });
                REPEAT_DATA.add(new RepeatAttachment(new WeakReference<>(candidate), data));
                return;
            }
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Unable to associate a repeat marker with an E33Chat message", exception);
        }
    }

    private static void mirrorSystemMessage(GuiMessage message, GuiMessageExtraData data) {
        if (data.redEnvelopeSnapshot == null || !isE33ChatEnabled()) return;
        if (updateExistingEnvelope(data.redEnvelopeId, data.isRedEnvelopeFinishNotice, data)) return;
        String sender = data.redEnvelopeSnapshot.senderName();
        if (sender == null || sender.isBlank()) sender = "Server";
        Component storedContent = message.content();
        if (data.isRedEnvelope && storedContent.getString().isBlank()) {
            storedContent = Component.literal("\u200B" + storedContent.getString());
        }

        try {
            Class<?> store = Class.forName(STORE_CLASS);
            Object newStoredMessages = store.getMethod("getMessages").invoke(null);
            if (newStoredMessages instanceof List<?> messages) {
                String contentText = storedContent.getString();
                for (int i = messages.size() - 1; i >= 0; i--) {
                    Object existing = messages.get(i);
                    if (hasMessage(existing)) continue;
                    Component existingContent = getMessageContent(existing);
                    if (existingContent == null) continue;
                    if (!existingContent.getString().equals(contentText)
                        && !isLegacyEnvelopeContent(existingContent, data)) continue;
                    if (!Boolean.TRUE.equals(existing.getClass().getMethod("isSystem").invoke(existing))) continue;
                    remember(existing, data);
                    remember(existingContent, data);
                    return;
                }
            }
            Method addMessage = store.getMethod("addMessage", Component.class, UUID.class, Component.class,
                boolean.class, String.class, boolean.class, String.class, boolean.class);
            addMessage.invoke(null, storedContent, new UUID(0L, 0L), Component.literal(sender),
                true, null, false, null, false);
            Object storedMessages = store.getMethod("getMessages").invoke(null);
            if (storedMessages instanceof List<?> messages) {
                for (int i = messages.size() - 1; i >= 0; i--) {
                    Object added = messages.get(i);
                    if (getMessageContent(added) != storedContent) continue;
                    remember(storedContent, data);
                    remember(added, data);
                    break;
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Unable to mirror a VW50 chat item into E33Chat", exception);
        }
    }

    private static boolean isLegacyEnvelopeContent(Component content, GuiMessageExtraData data) {
        if (!data.isRedEnvelope || data.redEnvelopeSnapshot == null) return false;
        String sender = data.redEnvelopeSnapshot.senderName();
        if (sender == null || sender.isBlank()) sender = "Server";
        String label = Component.translatable("red_envelope.chat.card_title").getString();
        String senderPrefix = "<" + sender + ">";
        String text = content.getString();
        return text.startsWith(senderPrefix + " [" + label + "]")
            || text.startsWith(senderPrefix) && text.substring(senderPrefix.length()).isBlank();
    }

    private static boolean isE33ChatEnabled() {
        if (!e33ConfigResolved) {
            synchronized (E33ChatCompat.class) {
                if (!e33ConfigResolved) resolveE33EnabledValue();
            }
        }
        if (e33EnabledGetter == null) return false;
        try {
            return Boolean.TRUE.equals(e33EnabledGetter.invoke(e33EnabledValue));
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Unable to read E33Chat's enabled setting", exception);
            return false;
        }
    }

    private static void resolveE33EnabledValue() {
        try {
            Class<?> config = Class.forName(CONFIG_CLASS);
            e33EnabledValue = config.getField("ENABLED").get(null);
            e33EnabledGetter = e33EnabledValue.getClass().getMethod("get");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            e33EnabledValue = null;
            e33EnabledGetter = null;
        } finally {
            e33ConfigResolved = true;
        }
    }

    private record ComponentAttachment(WeakReference<Component> component, GuiMessageExtraData data) {
    }

    private record MessageAttachment(WeakReference<Object> message, GuiMessageExtraData data) {
    }

    private record RepeatAttachment(WeakReference<Object> message, GuiMessageExtraData data) {
    }

    private record HitBox(float left, float top, float right, float bottom, UUID envelopeId,
                          String repeatText, boolean closeClaimList) {
        boolean contains(double x, double y) {
            return x >= this.left && x < this.right && y >= this.top && y < this.bottom;
        }
    }
}
