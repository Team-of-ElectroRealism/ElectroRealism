package com.teamofelectrorealism.electrorealism.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// Use generics: T must be a subclass of AbstractModMenu
public abstract class AbstractModScreen<T extends AbstractModMenu> extends AbstractContainerScreen<T> {

    protected AbstractModScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * @return The ResourceLocation of the main GUI texture for this screen.
     */
    protected abstract ResourceLocation getGuiTexture();

    /**
     * Subclasses must implement this to render their specific progress bars,
     * power indicators, heat displays, etc.
     *
     * @param guiGraphics The GuiGraphics instance.
     * @param x           The top-left x-coordinate of the GUI.
     * @param y           The top-left y-coordinate of the GUI.
     * @param partialTick Partial tick time.
     * @param mouseX      Mouse x position.
     * @param mouseY      Mouse y position.
     */
    protected abstract void renderProgressWidgets(GuiGraphics guiGraphics, int x, int y, float partialTick, int mouseX, int mouseY);


    @Override
    protected void init() {
        super.init();
        // Hide default labels
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getGuiTexture());
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(getGuiTexture(), x, y, 0, 0, imageWidth, imageHeight);
        renderProgressWidgets(guiGraphics, x, y, partialTick, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // Standard render loop
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
