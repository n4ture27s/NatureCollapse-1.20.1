package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.INaturalizable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = Integer.MAX_VALUE)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    @Final
    private MinecraftServer server;

    /**
     * 帰化（即死）状態のプレイヤーがリスポーンボタンを押した際、他MODの体力改変（HP固定等）をバイパスして
     * 確実に正常リスポーン処理を実行する。
     */
    @Inject(method = "handleClientCommand", at = @At("HEAD"), cancellable = true)
    private void onHandleClientCommand(ServerboundClientCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN) {
            if (this.player != null && ((INaturalizable) this.player).nature_collapse$isNaturalized()) {
                PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.serverLevel());
                this.player.resetLastActionTime();
                if (this.player.wonGame) {
                    this.player.wonGame = false;
                    this.player = this.server.getPlayerList().respawn(this.player, true);
                    CriteriaTriggers.CHANGED_DIMENSION.trigger(this.player, Level.END, Level.OVERWORLD);
                } else {
                    this.player = this.server.getPlayerList().respawn(this.player, false);
                    if (this.server.isHardcore()) {
                        this.player.setGameMode(GameType.SPECTATOR);
                        this.player.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_SPECTATORSGENERATECHUNKS).set(false, this.server);
                    }
                }
                ci.cancel();
            }
        }
    }

    /**
     * 帰化（死亡待機中）のプレイヤーが勝手に移動できないようパケットを無効化
     */
    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void onHandleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (this.player != null && ((INaturalizable) this.player).nature_collapse$isNaturalized()) {
            ci.cancel();
        }
    }

    /**
     * 帰化（死亡待機中）のプレイヤーによる他者・ブロックへのインタラクトを無効化
     */
    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void onHandleInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (this.player != null && ((INaturalizable) this.player).nature_collapse$isNaturalized()) {
            ci.cancel();
        }
    }

    /**
     * 帰化（死亡待機中）のプレイヤーによるアイテム使用を無効化
     */
    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void onHandleUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (this.player != null && ((INaturalizable) this.player).nature_collapse$isNaturalized()) {
            ci.cancel();
        }
    }

    /**
     * 帰化（死亡待機中）のプレイヤーによるブロック破壊・アクションを無効化
     */
    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void onHandlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (this.player != null && ((INaturalizable) this.player).nature_collapse$isNaturalized()) {
            ci.cancel();
        }
    }

    /**
     * 帰化（死亡待機中）のプレイヤーによる腕振り（攻撃等）を無効化
     */
    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void onHandleAnimate(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (this.player != null && ((INaturalizable) this.player).nature_collapse$isNaturalized()) {
            ci.cancel();
        }
    }
}
