package de.teamlapen.vampirism.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import de.teamlapen.vampirism.client.gui.recipebook.WeaponTableRecipeBookGui;
import de.teamlapen.vampirism.inventory.container.WeaponTableContainer;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.client.gui.recipebook.IRecipeShownListener;
import net.minecraft.client.gui.recipebook.RecipeBookGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Gui for the weapon table. Only draws the background and the lava status
 */
@OnlyIn(Dist.CLIENT)
public class WeaponTableScreen extends ContainerScreen<WeaponTableContainer> implements IRecipeShownListener {

    private static final ResourceLocation TABLE_GUI_TEXTURES = new ResourceLocation(REFERENCE.MODID, "textures/gui/weapon_table.png");
    private static final ResourceLocation TABLE_GUI_TEXTURES_LAVA = new ResourceLocation(REFERENCE.MODID, "textures/gui/weapon_table_lava.png");
    private static final ResourceLocation TABLE_GUI_TEXTURES_MISSING_LAVA = new ResourceLocation(REFERENCE.MODID, "textures/gui/weapon_table_missing_lava.png");
    private static final ResourceLocation RECIPE_BUTTON_TEXTURE = new ResourceLocation("textures/gui/recipe_button.png");
    private final RecipeBookGui recipeBookGui = new WeaponTableRecipeBookGui();
    private boolean widthTooNarrow;

    public WeaponTableScreen(WeaponTableContainer inventorySlotsIn, PlayerInventory inventoryPlayer, ITextComponent name) {
        super(inventorySlotsIn, inventoryPlayer, name);
        this.xSize = 196;
        this.ySize = 191;
    }

    @Override
    public void func_230430_a_(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        super.func_230430_a_(stack, mouseX, mouseY, partialTicks);
        this.func_230446_a_(stack);
        if (this.recipeBookGui.isVisible() && this.widthTooNarrow) {
            this.func_230450_a_(stack, partialTicks, mouseX, mouseY);
            this.recipeBookGui.func_230430_a_(stack, mouseX, mouseY, partialTicks);
        } else {
            this.recipeBookGui.func_230430_a_(stack, mouseX, mouseY, partialTicks);
            super.func_230430_a_(stack, mouseX, mouseY, partialTicks);
            this.recipeBookGui.func_230477_a_(stack, this.guiLeft, this.guiTop, true, partialTicks);
        }
        this.func_230459_a_(stack, mouseX, mouseY);
        this.recipeBookGui.func_238924_c_(stack, this.guiLeft, this.guiTop, mouseX, mouseY);
        this.func_212932_b(this.recipeBookGui);
    }

    @Override
    public void func_231023_e_() {
        super.func_231023_e_();
        this.recipeBookGui.tick();
    }

    @Override
    public boolean func_231044_a_(double mouseX, double mouseY, int p_mouseClicked_5_) {
        if (this.recipeBookGui.func_231044_a_(mouseX, mouseY, p_mouseClicked_5_)) {
            return true;
        } else {
            return this.widthTooNarrow && this.recipeBookGui.isVisible() || super.func_231044_a_(mouseX, mouseY, p_mouseClicked_5_);
        }
    }

    @Override
    public void func_231164_f_() {
        this.recipeBookGui.removed();
        super.func_231164_f_();
    }

    @Override
    protected boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
        return (!this.widthTooNarrow || !this.recipeBookGui.isVisible()) && super.isPointInRegion(x, y, width, height, mouseX, mouseY);
    }

    @Override
    protected void func_230450_a_(MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        int i = this.guiLeft;
        int j = (this.field_230709_l_ - this.ySize) / 2;
        this.field_230706_i_.getTextureManager().bindTexture(TABLE_GUI_TEXTURES);
        this.func_238474_b_(stack, i, j, 0, 0, this.xSize, this.ySize);
        if (container.hasLava()) {
            this.field_230706_i_.getTextureManager().bindTexture(TABLE_GUI_TEXTURES_LAVA);
            this.func_238474_b_(stack, i, j, 0, 0, this.xSize, this.ySize);
        }
        if (container.isMissingLava()) {
            this.field_230706_i_.getTextureManager().bindTexture(TABLE_GUI_TEXTURES_MISSING_LAVA);
            this.func_238474_b_(stack, i, j, 0, 0, this.xSize, this.ySize);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        boolean flag = mouseX < (double)guiLeftIn || mouseY < (double)guiTopIn || mouseX >= (double)(guiLeftIn + this.xSize) || mouseY >= (double)(guiTopIn + this.ySize);
        return this.recipeBookGui.func_195604_a(mouseX, mouseY, this.guiLeft, this.guiTop, this.xSize, this.ySize, mouseButton) && flag;
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
        this.recipeBookGui.slotClicked(slotIn);
    }

    @Override
    public void recipesUpdated() {
        this.recipeBookGui.recipesUpdated();
    }

    @Override
    protected void func_231160_c_() {
        super.func_231160_c_();
        this.widthTooNarrow = this.field_230708_k_ < 379;
        this.recipeBookGui.init(this.field_230708_k_, this.field_230709_l_, this.field_230706_i_, this.widthTooNarrow, this.container);
        this.guiLeft = this.recipeBookGui.updateScreenPosition(this.widthTooNarrow, this.field_230708_k_, this.xSize - 18);
        this.field_230705_e_.add(this.recipeBookGui);
        this.setFocusedDefault(this.recipeBookGui);
        this.func_230480_a_(new ImageButton(this.guiLeft + 5, this.field_230709_l_ / 2 - 49, 20, 18, 0, 0, 19, RECIPE_BUTTON_TEXTURE, (p_214076_1_) -> {
            this.recipeBookGui.initSearchBar(this.widthTooNarrow);
            this.recipeBookGui.toggleVisibility();
            this.guiLeft = this.recipeBookGui.updateScreenPosition(this.widthTooNarrow, this.field_230708_k_, this.xSize - 18);
            ((ImageButton) p_214076_1_).setPosition(this.guiLeft + 5, this.field_230709_l_ / 2 - 49);
        }));
    }

    @Override
    public RecipeBookGui getRecipeGui() {
        return recipeBookGui;
    }

}
