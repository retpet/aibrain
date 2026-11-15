package com.aibrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiAIMenu extends GuiScreen {

    private static final int PANEL_W = 260;
    private static final int PANEL_H = 196;

    private int left;
    private int top;

    private static class StyledButton extends GuiButton {
        private final int color;
        private final boolean active;

        StyledButton(int id, int x, int y, int w, int h, String text, int color, boolean active) {
            super(id, x, y, w, h, text);
            this.color = color;
            this.active = active;
        }

        private static int lighten(int c) {
            int r = Math.min(255, ((c >> 16) & 255) + 35);
            int g = Math.min(255, ((c >> 8) & 255) + 35);
            int b = Math.min(255, (c & 255) + 35);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;
            boolean hover = mouseX >= this.x && mouseY >= this.y
                    && mouseX < this.x + this.width && mouseY < this.y + this.height;
            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF000000);
            drawRect(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1,
                    hover ? lighten(color) : color);
            if (active) {
                drawRect(this.x + 1, this.y + this.height - 3, this.x + this.width - 1,
                        this.y + this.height - 1, 0xFFFFD54F);
            }
            this.drawCenteredString(mc.fontRenderer, this.displayString,
                    this.x + this.width / 2, this.y + (this.height - 8) / 2, hover ? 0xFFFFFF : 0xE8E8E8);
        }
    }

    private void add(int id, int x, int y, int w, String text, int color, boolean active) {
        this.buttonList.add(new StyledButton(id, x, y, w, 20, text, color, active));
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        left = (this.width - PANEL_W) / 2;
        top = Math.max(4, (this.height - PANEL_H) / 2);

        boolean surv = BrainManager.getMode() == BrainManager.Mode.SURVIVAL;
        int x1 = left + 10;
        int x2 = left + 130;
        int y = top + 54;

        add(0, x1, y, 120, "Survival", 0xFF2E7D32, surv);
        add(1, x2, y, 120, "Creative", 0xFF1565C0, !surv);
        y += 22;
        add(2, x1, y, 120, "Spawn / respawn", 0xFF455A64, false);
        add(3, x2, y, 120, "Build (my taste)", 0xFF6A1B9A, false);
        y += 22;
        add(4, x1, y, 78, "House", 0xFF7B1FA2, false);
        add(5, x1 + 81, y, 78, "Farm", 0xFF7B1FA2, false);
        add(6, x1 + 162, y, 78, "Redstone", 0xFF7B1FA2, false);
        y += 22;
        add(7, x1, y, 120, "Like last build", 0xFF388E3C, false);
        add(8, x2, y, 120, "Dislike last build", 0xFFC62828, false);
        y += 22;
        add(9, x1, y, 120, "Bot info", 0xFF455A64, false);
        add(10, x2, y, 120, "Progress", 0xFF455A64, false);
        y += 22;
        add(11, x1, y, 120, "Reset brain", 0xFF6D4C41, false);
        add(12, x2, y, 120, "Close", 0xFF37474F, false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        String cmd = null;
        switch (button.id) {
            case 0: cmd = "/aibrain mode survival"; break;
            case 1: cmd = "/aibrain mode creative"; break;
            case 2: cmd = "/aibrain spawn"; break;
            case 3: cmd = "/aibrain build"; break;
            case 4: cmd = "/aibrain build house"; break;
            case 5: cmd = "/aibrain build farm"; break;
            case 6: cmd = "/aibrain build redstone"; break;
            case 7: cmd = "/aibrain like"; break;
            case 8: cmd = "/aibrain dislike"; break;
            case 9: cmd = "/aibrain info"; break;
            case 10: cmd = "/aibrain progress"; break;
            case 11: cmd = "/aibrain reset"; break;
            default: break;
        }
        this.mc.displayGuiScreen(null);
        if (cmd != null && this.mc.player != null) {
            this.mc.player.sendChatMessage(cmd);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // panel with a border and a header strip
        drawRect(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF5C6BC0);
        drawRect(left, top, left + PANEL_W, top + PANEL_H, 0xF0141420);
        drawRect(left, top, left + PANEL_W, top + 24, 0xFF1F2340);
        drawRect(left, top + 24, left + PANEL_W, top + 26, 0xFFFFD54F);

        this.drawCenteredString(this.fontRenderer, "\u00A76\u00A7lAI BRAIN", this.width / 2, top + 8, 0xFFFFFF);

        Brain b = BrainManager.getBrain();
        String modeName = BrainManager.getMode() == BrainManager.Mode.SURVIVAL ? "Survival" : "Creative";
        this.drawCenteredString(this.fontRenderer,
                "\u00A77Mode: \u00A7f" + modeName + "  \u00A77Episodes: \u00A7f" + b.episodes,
                this.width / 2, top + 32, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "\u00A77" + Progress.describe(),
                this.width / 2, top + 42, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
