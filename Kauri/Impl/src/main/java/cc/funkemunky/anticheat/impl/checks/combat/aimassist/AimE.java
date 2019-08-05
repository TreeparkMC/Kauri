package cc.funkemunky.anticheat.impl.checks.combat.aimassist;

import cc.funkemunky.anticheat.api.checks.AlertTier;
import cc.funkemunky.anticheat.api.checks.Check;
import cc.funkemunky.anticheat.api.checks.CheckInfo;
import cc.funkemunky.anticheat.api.checks.CheckType;
import cc.funkemunky.anticheat.api.utils.MiscUtils;
import cc.funkemunky.anticheat.api.utils.Packets;
import cc.funkemunky.anticheat.api.utils.Setting;
import cc.funkemunky.anticheat.api.utils.Verbose;
import cc.funkemunky.api.tinyprotocol.api.Packet;
import cc.funkemunky.api.utils.Init;
import lombok.val;
import org.bukkit.event.Event;

@Packets(packets = {Packet.Client.POSITION_LOOK, Packet.Client.LOOK})
@Init
@CheckInfo(name = "Aim (Type E)", description = "Checks for low common denominators in other rotations - FlyCode.", type = CheckType.AIM, maxVL = 50)
public class AimE extends Check {

    private double vl;

    @Override
    public void onPacket(Object packet, String packetType, long timeStamp) {
        val move = getData().getMovementProcessor();

        long threshold = move.getYawDelta() > 10 ? 30000 : 100000;

        float accel = Math.abs(move.getYawDelta() - move.getLastYawDelta());
        if(move.getYawGCD() < threshold && !getData().isCinematicMode() && accel < 4.2) {
            vl++;
        } else vl-= vl > 0 ? 0.5 : 0;

        debug("SD=" + move.getYawGCD() + " PD:" + move.getCinematicPitchDelta() + " mode=" + getData().isCinematicMode());

    }

    @Override
    public void onBukkitEvent(Event event) {

    }
}
