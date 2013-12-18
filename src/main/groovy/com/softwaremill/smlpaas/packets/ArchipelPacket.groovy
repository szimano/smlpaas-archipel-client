package com.softwaremill.smlpaas.packets

import org.jivesoftware.smack.packet.IQ

/**
 * //TODO fill in komentarz bitch!
 */
class ArchipelPacket extends IQ {

    private final String source
    private final String newName

    ArchipelPacket(String source, String newName) {
        this.newName = newName
        this.source = source
        setType(IQ.Type.SET)
        setTo("sml.cumulushost.eu@xmpp.pacmanvps.com/sml.cumulushost.eu")
        setPacketID("17755")
    }

    @Override
    String getChildElementXML() {
        return "<query xmlns=\"archipel:hypervisor:control\">" +
                "<archipel action=\"clone\" jid=\"${source}\" name=\"${newName}\"/>" +
                "</query>"
    }
}
