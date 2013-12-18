package com.softwaremill.smlpaas

import org.jivesoftware.smack.Connection
import org.jivesoftware.smack.PacketListener
import org.jivesoftware.smack.packet.Packet
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smackx.packet.VCard

class SubscriptionListener implements PacketListener{

    private static final String FN = "FN"
    private final Connection conn
    private final String newVM

    SubscriptionListener(Connection conn, String newVM) {
        this.newVM = newVM
        this.conn = conn
    }

    @Override
    void processPacket(Packet packet) {
        Presence presence = packet as Presence

        if (presence.type == Presence.Type.subscribe) {
            println presence

            VCard vCard = new VCard();
            vCard.load(conn, presence.from)

            def name = vCard.getField(FN)

            if (name == null) {
                // vcard not yet created, try again in 5 seconds
                try {Thread.sleep(5000)} catch (Exception e){}
                vCard.load(conn, presence.from)
                name = vCard.getField(FN)
            }

            if (newVM == name) {

                def group = conn.getRoster().getGroup(ArchipelClient.PAAS_GROUP)
                if (group == null) {
                    group = conn.getRoster().createGroup(ArchipelClient.PAAS_GROUP)
                }

                conn.getRoster().createEntry(presence.from, name, group.name)
            } else {
                println "Got unexpected subscription request from $name / ${presence.from}"
            }
        }
    }
}
