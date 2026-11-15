package com.aibrain;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiAIMenu extends GuiScreen {

    @Override
    public void initGui() {
        int cx = this.width / 2;
        int y = this.height / 2 - 55;
        this.buttonList.add(new GuiButton(0, cx - 100, y, 200, 20, "Mode: Survival"));
        this.buttonList.add(new GuiButton(1, cx - 100, y + 25, 200, 20, "Mode: Creative"));
        this.buttonList.add(new GuiButton(2, cx - 100, y + 50, 200, 20, "Spawn / respawn bot"));
        this.buttonList.add(new GuiButton(3, cx - 100, y + 75, 200, 20, "Bot info"));
        this.buttonList.add(new GuiButton(4, cx - 100, y + 100, 200, 20, "Reset brain of current mode"));
        this.buttonList.add(new GuiButton(5, cx - 100, y + 125, 200, 20, "Close"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        String cmd = null;
        switch (button.id) {
            case 0: cmd = "/aibrain mode survival"; break;
            case 1: cmd = "/aibrain mode creative"; break;
            case 2: cmd = "/aibrain spawn"; break;
            case 3: cmd = "/aibrain info"; break;
            case 4: cmd = "/aibrain reset"; break;
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
        this.drawCenteredString(this.fontRenderer, "AI Brain", this.width / 2, this.height / 2 - 75, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
