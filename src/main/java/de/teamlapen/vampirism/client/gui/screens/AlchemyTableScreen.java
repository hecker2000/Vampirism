package de.teamlapen.vampirism.client.gui.screens;

import de.teamlapen.vampirism.REFERENCE;
import de.teamlapen.vampirism.api.util.VResourceLocation;
import de.teamlapen.vampirism.inventory.AlchemyTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AlchemyTableScreen extends AbstractContainerScreen<AlchemyTableMenu> {

    private static final ResourceLocation BREWING_STAND_LOCATION = VResourceLocation.mod("textures/gui/container/alchemy_table.png");
    private static final ResourceLocation BLAZE_CHARGE_SPRITE = VResourceLocation.mod("container/alchemy_table/blaze_charge");
    private static final ResourceLocation OIL_SPRITE = VResourceLocation.mod("container/alchemy_table/oil");
    private static final ResourceLocation PROGRESS_SPRITE = VResourceLocation.mod("container/alchemy_table/progress");

    private static final int[] BUBBLELENGTHS = new int[]{29, 24, 20, 16, 11, 6, 0};

    public AlchemyTableScreen(@NotNull AlchemyTableMenu p_i51105_1_, @NotNull Inventory p_i51105_2_, @NotNull Component p_i51105_3_) {
        super(p_i51105_1_, p_i51105_2_, p_i51105_3_);
        this.imageHeight = 181;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
        super.render(graphics, p_230430_2_, p_230430_3_, p_230430_4_);
        this.renderTooltip(graphics, p_230430_2_, p_230430_3_);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float p_230450_2_, int p_230450_3_, int p_230450_4_) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        graphics.blit(BREWING_STAND_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
        int k = this.menu.getFuel();
        int l = Mth.clamp((18 * k + 20 - 1) / 20, 0, 18);
        if (l > 0) {
            graphics.blitSprite(BLAZE_CHARGE_SPRITE,18,4,0,0, i+33, j + 60, l, 4);
        }

        int i1 = this.menu.getBrewingTicks();
        if (i1 > 0) {
            float j1 = 1.0F - ((float) i1 / 600.0F);
            if (j1 > 0) {
                graphics.blitSprite(PROGRESS_SPRITE,28, 8, 0,0, i + 73, j + 57, (int) (j1 * 28), 8);
                int color = this.menu.getColor();
                graphics.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, ((color) & 0xFF) / 255f, 1F);
                graphics.blitSprite(OIL_SPRITE, 32,32,0,0, i + 104, j + 36, (int) (j1 * 32), 32);
                graphics.setColor(1,1,1,1);
            }
        }

    }
}
