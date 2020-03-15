package dev.brighten.anticheat.check.impl.movement.nofall;

import cc.funkemunky.api.tinyprotocol.api.ProtocolVersion;
import cc.funkemunky.api.tinyprotocol.packet.in.WrappedInFlyingPacket;
import cc.funkemunky.api.utils.MathUtils;
import cc.funkemunky.api.utils.world.BlockData;
import cc.funkemunky.api.utils.world.CollisionBox;
import cc.funkemunky.api.utils.world.types.SimpleCollisionBox;
import dev.brighten.anticheat.check.api.Cancellable;
import dev.brighten.anticheat.check.api.Check;
import dev.brighten.anticheat.check.api.CheckInfo;
import dev.brighten.anticheat.check.api.Packet;
import dev.brighten.anticheat.utils.Helper;
import dev.brighten.api.check.CheckType;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

@CheckInfo(name = "NoFall (A)", description = "Checks to make sure the ground packet from the client is legit",
        checkType = CheckType.NOFALL, punishVL = 20, executable = false)
@Cancellable
public class NoFallA extends Check {

    @Packet
    public void onPacket(WrappedInFlyingPacket packet, long timeStamp) {

        boolean flag = data.playerInfo.clientGround
                ? data.playerInfo.deltaY != 0 && !data.playerInfo.serverGround
                && data.playerInfo.lastBlockPlace.hasPassed(10)
                : data.playerInfo.deltaY == 0 && data.playerInfo.lDeltaY == 0;

        if(data.playerInfo.deltaY < 0 && data.playerInfo.clientGround && flag) {
            for (Block block : Helper.blockCollisions(data.blockInfo.handler.getBlocks(),
                    data.box.copy().expand(0.2 + Math.abs(data.playerInfo.deltaX),0,
                            0.2 + Math.abs(data.playerInfo.deltaZ))
                            .expandMin(0, -0.5f + Math.min(0, data.playerInfo.deltaY), 0))) {
                CollisionBox box = BlockData.getData(block.getType())
                        .getBox(block, ProtocolVersion.getGameVersion());

                List<SimpleCollisionBox> sBoxes = new ArrayList<>();
                box.downCast(sBoxes);

                for (SimpleCollisionBox sBox : sBoxes) {
                    double minDelta = sBox.yMax - data.playerInfo.from.y;

                    if(MathUtils.getDelta(data.playerInfo.deltaY, minDelta) < 1E-7) {
                        flag = false;
                        break;
                    }
                }
            }
        }

        if(!data.playerInfo.flightCancel
                && data.playerInfo.lastHalfBlock.hasPassed(4)
                && !data.blockInfo.onSlime
                && !data.blockInfo.blocksAbove
                && data.playerInfo.lastBlockPlace.hasPassed(8)
                && data.playerInfo.lastVelocity.hasPassed(4)
                && (data.playerInfo.deltaY != 0 || data.playerInfo.deltaXZ > 0)
                && data.playerInfo.blockAboveTimer.hasPassed(10)
                && flag) {
            vl+= data.lagInfo.lagging
                    || data.lagInfo.lastPacketDrop.hasNotPassed(1)
                    || data.playerInfo.nearGround
                    || data.blockInfo.blocksNear
                    ? 1 : data.playerInfo.clientGround ? 2 : 3;

            if(vl > 2) {
                flag("ground=" + data.playerInfo.clientGround + " deltaY=" + data.playerInfo.deltaY);
            }
        } else vl-= vl > 0 ? 0.2f : 0;

        debug("ground=" + data.playerInfo.clientGround + " collides=" + data.blockInfo.collidesVertically
                + " deltaY=" + data.playerInfo.deltaY + " vl=" + vl);
    }
}