package de.teamlapen.vampirism.client.gui.screens;

import de.teamlapen.vampirism.REFERENCE;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.util.ItemOrdering;
import de.teamlapen.vampirism.client.ClientConfigHelper;
import de.teamlapen.vampirism.client.core.ModKeys;
import de.teamlapen.vampirism.client.gui.screens.radial.edit.ReorderingGuiRadialMenu;
import de.teamlapen.vampirism.core.ModRegistries;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.network.ServerboundActionBindingPacket;
import de.teamlapen.vampirism.util.RegUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EditSelectActionScreen<T extends IFactionPlayer<T>> extends ReorderingGuiRadialMenu<IAction<?>> {

    public static void show() {
        FactionPlayerHandler.get(Minecraft.getInstance().player).getCurrentFactionPlayer().ifPresent(factionPlayer -> Minecraft.getInstance().setScreen(new EditSelectActionScreen(factionPlayer)));
    }

    private static void drawActionPart(@Nullable IAction<?> action, GuiGraphics graphics, int posX, int posY, int size, boolean transparent) {
        if (action == null) return;
        graphics.blit(getActionIcon(action), posX, posY, 0, 0, 0, 16, 16, 16, 16);
    }

    private static ResourceLocation getActionIcon(IAction<?> action) {
        ResourceLocation id = RegUtil.id(action);
        return new ResourceLocation(id.getNamespace(), "textures/actions/" + id.getPath() + ".png");
    }

    private static <T extends IFactionPlayer<T>> boolean isEnabled(T player, @NotNull IAction<?> item) {
        return player.getActionHandler().isActionUnlocked((IAction) item);
    }

    private static <T extends IFactionPlayer<T>> ItemOrdering<IAction<?>> getOrdering(T player) {
        return new ItemOrdering<>(ClientConfigHelper.getActionOrder(player.getFaction()).stream().filter(s -> s.showInSelectAction(player.asEntity())).toList(), new ArrayList<>(), () -> ModRegistries.ACTIONS.stream().filter(action -> action.matchesFaction(player.getFaction())).filter(s -> s.showInSelectAction(player.asEntity())).toList());
    }

    private static <T extends IFactionPlayer<T>> void saveOrdering(T player, ItemOrdering<IAction<?>> ordering) {
        ClientConfigHelper.saveActionOrder(player.getFaction().getID(), ordering.getOrdering());
    }

    private KeyBindingList keyBindingList;

    public EditSelectActionScreen(T player) {
        super(getOrdering(player), action -> action.getName().plainCopy(), EditSelectActionScreen::drawActionPart, (ordering) -> saveOrdering(player, ordering), (item) -> EditSelectActionScreen.isEnabled(player, item));
    }

    @Override
    protected void init() {
        super.init();

        this.keyBindingList = this.addRenderableWidget(new KeyBindingList(this.width - 140, 20, 140, this.height - 60));

        this.addRenderableWidget(new ResetButton(this.width - 140, this.height - 40, 140, 20, (context) -> this.resetKeyBindings()));
        this.addRenderableWidget(new ExtendedButton(this.width - 140, this.height - 20, 140, 20, Component.translatable("text.vampirism.open_settings"), (context) -> {
            Minecraft.getInstance().setScreen(new KeyBindsScreen(this, getMinecraft().options));
        }));
    }

    private void resetKeyBindings() {
        ModKeys.ACTION_KEYS.keySet().forEach(key -> {
            FactionPlayerHandler.get(getMinecraft().player).setBoundAction(key, null, false, false);
            VampirismMod.proxy.sendToServer(new ServerboundActionBindingPacket(key));
        });
        this.keyBindingList.clearActions();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int p_296369_, int p_296477_, float p_294317_) {
        super.renderBackground(graphics, p_296369_, p_296477_, p_294317_);
        graphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        int i = 32;
        graphics.blit(BACKGROUND_LOCATION, this.width - 140, 0, 0, 0.0F, 0.0F, 140, this.height, i, i);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        graphics.drawCenteredString(this.font, Component.translatable("text.vampirism.key_shortcuts"), this.width - 70, 5, -1);
    }

    public class KeyBindingList extends ContainerObjectSelectionList<KeyBindingList.KeyBindingSetting> {

        public KeyBindingList(int x, int y, int pWidth, int pHeight) {
            super(Minecraft.getInstance(), pWidth, pHeight, y, 20);
            this.setRenderBackground(false);
            this.setX(x);
            FactionPlayerHandler handler = FactionPlayerHandler.get(Minecraft.getInstance().player);
            replaceEntries(ModKeys.ACTION_KEYS.entrySet().stream().map(pair -> new KeyBindingSetting(pair.getKey(), pair.getValue(), handler.getBoundAction(pair.getKey()))).sorted(Comparator.comparingInt((KeyBindingSetting o) -> o.index)).toList());
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        @Override
        public Optional<GuiEventListener> getChildAt(double pMouseX, double pMouseY) {
            return super.getChildAt(pMouseX, pMouseY);
        }


        @Override
        protected int getScrollbarPosition() {
            return this.getRight() - 6;
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        public int getRowLeft() {
            return super.getRowLeft() - 2;
        }

        @Override
        protected int getRowTop(int pIndex) {
            return super.getRowTop(pIndex);
        }

        public void clearActions() {
            this.children().forEach(entry -> entry.switchAction(null));
        }

        private class KeyBindingSetting extends ContainerObjectSelectionList.Entry<KeyBindingSetting> {

            private static final WidgetSprites REMOVE_ICON = new WidgetSprites(new ResourceLocation(REFERENCE.MODID, "widget/remove"), new ResourceLocation(REFERENCE.MODID, "widget/remove_highlighted"));
            private static final WidgetSprites BUTTON = new WidgetSprites(new ResourceLocation("widget/button"), new ResourceLocation("widget/button_highlighted"));

            private final int index;
            private final KeyMapping keyMapping;
            private IAction<?> action;
            private StringWidget stringWidget;
            private ImageWidget imageWidget;
            private ImageButton imageButton;

            public KeyBindingSetting(int index, KeyMapping keyMapping, IAction<?> action) {
                this.index = index;
                this.keyMapping = keyMapping;
                this.stringWidget = new StringWidget(0,2,80, 20, keyMapping.getTranslatedKeyMessage(), Minecraft.getInstance().font);
                this.imageButton = new ImageButton(115,2,16,16, REMOVE_ICON,(a) -> switchAction(null));
                applyAction(action);
            }

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                if (this.imageButton.mouseClicked(pMouseX - getX(), pMouseY - getY() - ((index-1) * 20), pButton)) {
                    return true;
                } else if (movingItem != null) {
                    switchAction(movingItem.get());
                    movingItem = null;
                    removeDummyItems();
                    return true;
                }
                return false;
            }

            private void switchAction(@Nullable IAction<?> action) {
                applyAction(action);
                VampirismMod.proxy.sendToServer(new ServerboundActionBindingPacket(this.index, this.action));
                FactionPlayerHandler.get(Minecraft.getInstance().player).setBoundAction(this.index, this.action, false, false);
            }

            private void applyAction(@Nullable IAction<?> action) {
                this.action = action;
                if (action != null) {
                    this.imageWidget = ImageWidget.texture(16, 16, getActionIcon(action), 16, 16);
                    this.imageWidget.setPosition(90, 2);
                    this.imageButton.visible = true;
                } else {
                    this.imageWidget = ImageWidget.texture(16, 16, null,16, 16);
                    this.imageWidget.setPosition(90, 2);
                    this.imageWidget.visible = false;
                    this.imageButton.visible = false;
                }
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(stringWidget, imageWidget, imageButton);
            }

            @Override
            public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean p_93531_, float pPartialTick) {
                pGuiGraphics.pose().pushPose();
                pGuiGraphics.pose().translate(pLeft, pTop,0);
                stringWidget.render(pGuiGraphics, pMouseX - pLeft, pMouseY - pTop, pPartialTick);
                imageWidget.render(pGuiGraphics, pMouseX - pLeft, pMouseY - pTop, pPartialTick);
                imageButton.render(pGuiGraphics, pMouseX - pLeft, pMouseY - pTop, pPartialTick);
                pGuiGraphics.pose().popPose();
            }

            @Override
            public void renderBack(@NotNull GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
                if (movingItem != null) {
                    pGuiGraphics.blitSprite(BUTTON.get(true, pIsMouseOver), pLeft, pTop, pWidth, pHeight+5);
                }
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(stringWidget, imageWidget, imageButton);
            }
        }
    }
}
