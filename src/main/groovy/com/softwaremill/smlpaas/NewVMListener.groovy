package com.softwaremill.smlpaas
import org.jivesoftware.smack.Connection
import org.jivesoftware.smack.RosterListener
import org.jivesoftware.smack.packet.Presence

class NewVMListener implements RosterListener{

    private final Connection conn
    private final String newVM
    private final Closure stopThread

    NewVMListener(Connection conn, String newVM, Closure stopThread) {
        this.stopThread = stopThread
        this.newVM = newVM
        this.conn = conn
    }

    @Override
    void entriesAdded(Collection<String> addresses) {
        addresses.each {
            if (conn.getRoster().getEntry(it).name == newVM) {
                stopThread()
            }
        }
    }

    @Override
    void entriesUpdated(Collection<String> addresses) {
    }

    @Override
    void entriesDeleted(Collection<String> addresses) {
    }

    @Override
    void presenceChanged(Presence presence) {
    }
}
