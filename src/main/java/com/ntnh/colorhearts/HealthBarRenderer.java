package com.ntnh.colorhearts;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.HEALTH;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/*
 * TODO: Fix regeneration effect
 */

public class HealthBarRenderer extends Gui {

    private static final boolean isRpghudLoaded = Loader.isModLoaded("rpghud");
    private static final boolean isTukmc_vzLoaded = Loader.isModLoaded("tukmc_Vz");
    private static final boolean isBorderlandsModLoaded = Loader.isModLoaded("borderlands");
    private static final ResourceLocation TINKER_HEARTS = new ResourceLocation(
        "colorhearts",
        "textures/gui/newhearts.png");
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();
    private int updateCounter = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !mc.isGamePaused()) {
            this.updateCounter++;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void renderHealthbar(RenderGameOverlayEvent.Pre event) {
        if (event.type != HEALTH) {
            return;
        }

        if (isRpghudLoaded) {
            return;
        }

        if (isTukmc_vzLoaded && !isBorderlandsModLoaded) {
            return;
        }

        mc.mcProfiler.startSection("health");
        GL11.glEnable(GL11.GL_BLEND);

        final int width = event.resolution.getScaledWidth();
        final int height = event.resolution.getScaledHeight();
        final int xBasePos = width / 2 - 91;
        int yBasePos = height - 39;

        boolean highlight = mc.thePlayer.hurtResistantTime / 3 % 2 == 1;

        if (mc.thePlayer.hurtResistantTime < 10) {
            highlight = false;
        }

        final IAttributeInstance attrMaxHealth = mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.maxHealth);
        final int health = MathHelper.ceiling_float_int(mc.thePlayer.getHealth());
        final int healthLast = MathHelper.ceiling_float_int(mc.thePlayer.prevHealth);
        final float healthMax = Math.min(20F, (float) attrMaxHealth.getAttributeValue());
        float absorb = mc.thePlayer.getAbsorptionAmount();

        final int healthRows = MathHelper.ceiling_float_int((healthMax + absorb) / 2.0F / 10.0F);
        final int rowHeight = Math.max(10 - (healthRows - 2), 3);

        this.rand.setSeed(updateCounter * 312871L);

        int left = width / 2 - 91;
        int top = height - GuiIngameForge.left_height;

        if (!GuiIngameForge.renderExperiance) {
            top += 7;
            yBasePos += 7;
        }

        // Regeneration bounce effect
        float bounceOffset = 0.0F;
        if (mc.thePlayer.isPotionActive(Potion.regeneration)) {
            // Use a sine wave for smooth bouncing, synced with regeneration ticks (e.g., 50 ticks at level 1)
            float regenFrequency = 0.1F; // Adjust for speed (lower = slower)
            bounceOffset = (float) Math.sin(updateCounter * regenFrequency) * 2.0F; // -2 to 2 pixel range
        }

        final int TOP;
        int tinkerTextureY = 0;
        if (mc.theWorld.getWorldInfo()
            .isHardcoreModeEnabled()) {
            TOP = 9 * 5;
            tinkerTextureY += 27;
        } else {
            TOP = 0;
        }
        final int BACKGROUND = (highlight ? 25 : 16);
        int MARGIN = 16;
        if (mc.thePlayer.isPotionActive(Potion.poison)) {
            MARGIN += 36;
            tinkerTextureY = 9;
        } else if (mc.thePlayer.isPotionActive(Potion.wither)) {
            MARGIN += 72;
            tinkerTextureY = 18;
        }

        // Render vanilla hearts
        for (int i = MathHelper.ceiling_float_int((healthMax + absorb) / 2.0F) - 1; i >= 0; --i) {
            final int row = MathHelper.ceiling_float_int((float) (i + 1) / 10.0F) - 1;
            int x = left + i % 10 * 8;
            int y = top - row * rowHeight;

            // Apply shaking when health is low
            if (health <= 4) y += rand.nextInt(3) - 1;

            // Apply regeneration bounce to all hearts
            if (mc.thePlayer.isPotionActive(Potion.regeneration)) {
                y += bounceOffset;
            }

            this.drawTexturedModalRect(x, y, BACKGROUND, TOP, 9, 9);

            if (highlight) {
                if (i * 2 + 1 < healthLast) {
                    this.drawTexturedModalRect(x, y, MARGIN + 54, TOP, 9, 9);
                } else if (i * 2 + 1 == healthLast) {
                    this.drawTexturedModalRect(x, y, MARGIN + 63, TOP, 9, 9);
                }
            }

            if (absorb > 0.0F) {
                if (absorb % 2.0F == 1.0F) {
                    this.drawTexturedModalRect(x, y, MARGIN + 153, TOP, 9, 9);
                } else {
                    this.drawTexturedModalRect(x, y, MARGIN + 144, TOP, 9, 9);
                }
                absorb -= 2.0F;
            } else if (i * 2 + 1 + 20 >= health) {
                if (i * 2 + 1 < health) {
                    this.drawTexturedModalRect(x, y, MARGIN + 36, TOP, 9, 9);
                } else if (i * 2 + 1 == health) {
                    this.drawTexturedModalRect(x, y, MARGIN + 45, TOP, 9, 9);
                }
            }
        }

        if (health > 20) {
            mc.getTextureManager()
                .bindTexture(TINKER_HEARTS);
            for (int i = Math.max(0, health / 20 - 2); i < health / 20; i++) {
                final int heartIndexMax = Math.min(10, (health - 20 * (i + 1)) / 2);

                int loopStartPoint = 0;
                int loopEnd = 10;
                int textureColumn;

                if (i <= loopEnd) {
                    textureColumn = i;
                } else {
                    int span = loopEnd - loopStartPoint + 1;
                    textureColumn = ((i - loopStartPoint) % span) + loopStartPoint;
                }

                int overlayCount = 0;
                if (health > 240 && health <= 260) {
                    overlayCount = health - 240;
                }

                for (int j = 0; j < heartIndexMax; j++) {
                    int y = 0;
                    if (health <= 4) y += rand.nextInt(3) - 1;
                    if (mc.thePlayer.isPotionActive(Potion.regeneration)) {
                        y += bounceOffset;
                    }
                    if ((i + 1) * 20 + j * 2 + 21 >= health) {
                        if (health <= 240) {
                            this.drawTexturedModalRect(
                                xBasePos + 8 * j,
                                yBasePos + y,
                                18 * textureColumn,
                                tinkerTextureY,
                                9,
                                9);
                        } else {
                            this.drawTexturedModalRect(xBasePos + 8 * j, yBasePos + y, 18 * 10, tinkerTextureY, 9, 9);
                            if (health <= 260) {
                                int fullOverlays = overlayCount / 2;
                                if (j < fullOverlays) {
                                    this.drawTexturedModalRect(xBasePos + 8 * j, yBasePos + y, 0, 54, 9, 9);
                                }
                            } else {
                                this.drawTexturedModalRect(
                                    xBasePos + 8 * j,
                                    yBasePos + y,
                                    18 * textureColumn,
                                    54,
                                    9,
                                    9);
                            }
                        }
                    }
                }
                if (health % 2 == 1 && heartIndexMax < 10) {
                    int y = 0;
                    if (health <= 4) y += rand.nextInt(3) - 1;
                    if (mc.thePlayer.isPotionActive(Potion.regeneration)) {
                        y += bounceOffset;
                    }
                    if (health <= 240) {
                        this.drawTexturedModalRect(
                            xBasePos + 8 * heartIndexMax,
                            yBasePos + y,
                            9 + 18 * textureColumn,
                            tinkerTextureY,
                            9,
                            9);
                    } else {
                        this.drawTexturedModalRect(
                            xBasePos + 8 * heartIndexMax,
                            yBasePos + y,
                            9 + 18 * 10,
                            tinkerTextureY,
                            9,
                            9);
                        if (health <= 260) {
                            int fullOverlays = overlayCount / 2;
                            boolean hasHalfOverlay = (overlayCount % 2) == 1;
                            if (heartIndexMax == fullOverlays && hasHalfOverlay) {
                                this.drawTexturedModalRect(xBasePos + 8 * heartIndexMax, yBasePos + y, 9, 54, 9, 9);
                            }
                        } else {
                            this.drawTexturedModalRect(
                                xBasePos + 8 * heartIndexMax,
                                yBasePos + y,
                                9 + 18 * textureColumn,
                                54,
                                9,
                                9);
                        }
                    }
                }
            }
            mc.getTextureManager()
                .bindTexture(Gui.icons);
        }

        GuiIngameForge.left_height += 10;
        if (absorb > 0) GuiIngameForge.left_height += 10;
        GL11.glDisable(GL11.GL_BLEND);
        mc.mcProfiler.endSection();
        event.setCanceled(true);
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(event, HEALTH));
    }
}
