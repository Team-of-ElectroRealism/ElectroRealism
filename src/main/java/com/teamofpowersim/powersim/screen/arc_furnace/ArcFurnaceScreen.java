package com.teamofpowersim.powersim.screen.arc_furnace;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamofpowersim.powersim.PowerSim;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ArcFurnaceScreen extends AbstractContainerScreen<ArcFurnaceMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/arc_furnace/arc_furnace_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.parse("textures/gui/sprites/container/furnace/burn_progress.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/icons/icon_power.png");

    private static final ResourceLocation[] HEAT_TEXTURES = new ResourceLocation[8]; // Assuming 0-7 heat levels

    static {
        for (int i = 0; i < HEAT_TEXTURES.length; i++) { // Use HEAT_TEXTURES.length
            HEAT_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(
                    PowerSim.MODID,
                    "textures/gui/arc_furnace/heat-" + i + ".png"
            );
        }
    }

    public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
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
        renderProgressPower(pGuiGraphics, x, y); // This will use the updated logic
        renderHeatAnimation(pGuiGraphics, x, y);
    }

    private void renderProgressPower(GuiGraphics pGuiGraphics, int x, int y) {
        if (menu.getPowerDisplayStatus() == 1.0f) {
            pGuiGraphics.blit(POWER_TEXTURE, x + 34, y + 35, 0, 0, 14, 14, 14, 14);
        }
    }

    private void renderProgressArrow(GuiGraphics pGuiGraphics, int x, int y) {
        if(menu.isSmelting()) { // isSmelting now also checks if powered from the menu
            int arrowWidth = Mth.ceil(menu.getSmeltingProgress() * 24.0F);
            if (arrowWidth > 0) { // Only blit if there's some width
                pGuiGraphics.blit(ARROW_TEXTURE, x + 79, y + 34, 0, 0, arrowWidth, 16, 24, 16);
            }
        }
    }

    private void renderHeatAnimation(GuiGraphics pGuiGraphics, int x, int y) {
        // menu.getHeatLevel() should return the current heat level (e.g., 0 to 7)
        int heatFrame = menu.getHeatLevel(); // Use the renamed/new getter from ArcFurnaceMenu
        heatFrame = Mth.clamp(heatFrame, 0, HEAT_TEXTURES.length - 1); // Ensure frame is within bounds

        if (heatFrame >= 0 && heatFrame < HEAT_TEXTURES.length) { // Double check bounds
            pGuiGraphics.blit(
                    HEAT_TEXTURES[heatFrame],   // Pass the ResourceLocation directly
                    x + 57, y + 18,          // Screen position
                    0, 0,                // U, V offset in the heat texture (assuming it's the full texture)
                    13, 13,               // Width, Height to draw on screen
                    13, 13                      // Texture Width, Texture Height (of the heatFrame texture itself)
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}