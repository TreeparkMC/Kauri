package dev.brighten.anticheat.check.impl.combat.autoclicker;

import cc.funkemunky.api.tinyprotocol.packet.in.WrappedInArmAnimationPacket;
import dev.brighten.anticheat.check.api.Check;
import dev.brighten.anticheat.check.api.CheckInfo;
import dev.brighten.anticheat.check.api.Packet;

@CheckInfo(name = "Autoclicker (Type A)", description = "A fast click check.", executable = true)
public class AutoclickerA extends Check {

    private int ticks;
    private long lastClick;

    @Packet
    public void onArmAnimation(WrappedInArmAnimationPacket packet) {
        if(System.currentTimeMillis() - lastClick > 1000L) {
            if(ticks > 20) {
                vl++;
                flag("cps=" + ticks + " ping=%p tps=%t");
            }
            if(ticks > 30) {
                punish();
            }
            ticks = 0;
            lastClick = System.currentTimeMillis();
        } else if(data.playerInfo.lastBrokenBlock.hasPassed(5)) ticks++;
    }
}