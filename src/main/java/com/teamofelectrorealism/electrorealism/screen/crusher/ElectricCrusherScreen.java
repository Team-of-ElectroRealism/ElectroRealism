package com.teamofelectrorealism.electrorealism.screen.crusher;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ElectricCrusherScreen extends AbstractContainerScreen<ElectricCrusherMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "textures/gui/alloy_furnace/alloy_furnace_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.parse("textures/gui/sprites/container/furnace/burn_progress.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "textures/gui/common/power_icon.png");

    public ElectricCrusherScreen(ElectricCrusherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        // Gets rid of labels
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        pGuiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(pGuiGraphics, x, y);
        renderProgressPower(pGuiGraphics, x, y);
    }

    // This works for the classic fire texture
    private void renderProgressPower(GuiGraphics pGuiGraphics, int x, int y) {
        int fireHeight = Mth.ceil(menu.getPowerProgress() * 13.0F) + 1; // Scale to max 14 pixels
        if (fireHeight > 0) {
            pGuiGraphics.blit(POWER_TEXTURE, x + 56, y + 36 + 14 - fireHeight, 0, 14 - fireHeight, 14, fireHeight, 14, 14); // Your original placement
        }
    }

    private void renderProgressArrow(GuiGraphics pGuiGraphics, int x, int y) {
        if(menu.isCrushing()) {
            int arrowWidth = Mth.ceil(menu.getCrushingProgress() * 24.0F);
            pGuiGraphics.blit(ARROW_TEXTURE, x + 79, y + 34, 0, 0, arrowWidth, 16, 24, 16);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
