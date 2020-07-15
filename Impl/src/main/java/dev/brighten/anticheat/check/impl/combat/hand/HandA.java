package dev.brighten.anticheat.check.impl.combat.hand;

import cc.funkemunky.api.tinyprotocol.packet.in.*;
import dev.brighten.anticheat.Kauri;
import dev.brighten.anticheat.check.api.*;
import dev.brighten.api.check.CheckType;

@CheckInfo(name = "Hand (A)", description = "Checks for irregular block place packets.",
        checkType = CheckType.HAND, punishVL = 30, executable = false)
@Cancellable(cancelType = CancelType.INTERACT)
public class HandA extends Check {

    private boolean sentFlying, sentTrans;

    @Packet
    public void use(WrappedInBlockPlacePacket packet) {
         debug("sentFlying=%v sentTrans=%v", sentFlying, sentTrans);
        if(sentFlying && sentTrans) {
            vl+= 2;
            if(vl > 11) {
                flag("fly=%v trans=%v", sentFlying, sentTrans);
            }
        } else if(vl > 0) vl--;
        sentFlying = sentTrans = false;
    }

    @Packet
    public void onTrans(WrappedInKeepAlivePacket packet) {
        if(Kauri.INSTANCE.keepaliveProcessor.keepAlives.containsKey((int)packet.getTime())) {
            sentFlying = false;
            sentTrans = true;
        }
    }

    @Packet
    public void flying(WrappedInFlyingPacket packet) {
        sentFlying = true;
    }
}
