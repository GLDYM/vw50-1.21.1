package com.sqzj.vw50.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ItemIconParticle extends TextureSheetParticle {

    private ItemIconParticle(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed, TextureAtlasSprite sprite) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.setSprite(sprite);
        this.quadSize = 0.25F;
        this.lifetime = 20;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.friction = 1.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final Supplier<? extends Item> item;

        public Provider(Supplier<? extends Item> item) {
            this.item = item;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer()
                    .getItemModelShaper().getItemModel(this.item.get()).getParticleIcon();
            return new ItemIconParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
        }
    }
}
