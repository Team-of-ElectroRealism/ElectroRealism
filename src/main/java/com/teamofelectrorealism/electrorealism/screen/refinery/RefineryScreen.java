package com.teamofelectrorealism.electrorealism.screen.refinery;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.screen.AbstractModScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class RefineryScreen extends AbstractModScreen<RefineryMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "textures/gui/crusher/electric_crusher_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.parse("textures/gui/sprites/container/furnace/burn_progress.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "textures/gui/icons/icon_power.png");

    public RefineryScreen(RefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return GUI_TEXTURE;
    }

    @Override
    protected void renderProgressWidgets(GuiGraphics guiGraphics, int x, int y, float partialTick, int mouseX, int mouseY) {
        renderProgressArrow(guiGraphics, x, y);
        renderProgressPower(guiGraphics, x, y);
    }

    private void renderProgressPower(GuiGraphics guiGraphics, int x, int y) {
        int powerHeight = Mth.ceil(menu.getPowerProgress() * 13.0F) + 1; // Scale to max 14 pixels
        if (powerHeight > 0) {
            guiGraphics.blit(POWER_TEXTURE, x + 57, y + 37 + 14 - powerHeight, 0, 14 - powerHeight, 14, powerHeight, 14, 14);
        }
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if(menu.isRefining()) {
            int arrowWidth = Mth.ceil(menu.getRefiningProgress() * 24.0F);
            guiGraphics.blit(ARROW_TEXTURE, x + 79, y + 34, 0, 0, arrowWidth, 16, 24, 16);
        }
    }
}
