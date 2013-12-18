package com.softwaremill.smlpaas

import org.jivesoftware.smack.filter.PacketFilter
import org.jivesoftware.smack.packet.Packet
import org.jivesoftware.smack.packet.Presence

class SubscriptionPacketFilter implements PacketFilter {

    @Override
    boolean accept(Packet packet) {
        return packet instanceof Presence
    }
}
