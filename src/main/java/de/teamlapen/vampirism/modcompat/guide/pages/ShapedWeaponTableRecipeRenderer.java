package de.teamlapen.vampirism.modcompat.guide.pages;

import amerifrance.guideapi.api.impl.Book;
import amerifrance.guideapi.api.impl.abstraction.CategoryAbstract;
import amerifrance.guideapi.api.impl.abstraction.EntryAbstract;
import amerifrance.guideapi.api.util.GuiHelper;
import amerifrance.guideapi.gui.GuiBase;
import de.teamlapen.lib.lib.util.ItemStackUtil;
import de.teamlapen.vampirism.inventory.ShapedHunterWeaponRecipe;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;


/**
 * Renders the items of a shaped weapon table recipe into the GUI rendered by {@link BasicWeaponTableRecipeRenderer}
 */
public class ShapedWeaponTableRecipeRenderer extends BasicWeaponTableRecipeRenderer<ShapedHunterWeaponRecipe> {
    public ShapedWeaponTableRecipeRenderer(ShapedHunterWeaponRecipe recipe) {
        super(recipe);
    }

    @Override
    public void draw(Book book, CategoryAbstract category, EntryAbstract entry, int guiLeft, int guiTop, int mouseX, int mouseY, GuiBase guiBase, FontRenderer fontRendererObj) {
        super.draw(book, category, entry, guiLeft, guiTop, mouseX, mouseY, guiBase, fontRendererObj);
        for (int y = 0; y < recipe.recipeHeight; y++) {
            for (int x = 0; x < recipe.recipeWidth; x++) {
                int stackX = (x + 1) * 17 + (guiLeft + 29);
                int stackY = (y + 1) * 17 + (guiTop + 30);
                ItemStack stack = recipe.recipeItems[y * recipe.recipeWidth + x];
                if (!ItemStackUtil.isEmpty(stack)) {
                    if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                        List<ItemStack> subItems = new ArrayList<ItemStack>();
                        stack.getItem().getSubItems(stack.getItem(), stack.getItem().getCreativeTab(), subItems);
                        stack = subItems.get(getRandomizedCycle(x, subItems.size()));
                    }

                    GuiHelper.drawItemStack(stack, stackX, stackY);
                    if (GuiHelper.isMouseBetween(mouseX, mouseY, stackX, stackY, 15, 15)) {
                        tooltips = GuiHelper.getTooltip(stack);
                    }
                }
            }
        }
    }
}
