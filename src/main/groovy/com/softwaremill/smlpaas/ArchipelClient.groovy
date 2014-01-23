package com.softwaremill.smlpaas

import com.softwaremill.smlpaas.packets.ArchipelPacket
import org.jivesoftware.smack.Chat
import org.jivesoftware.smack.ChatManagerListener
import org.jivesoftware.smack.Connection
import org.jivesoftware.smack.MessageListener
import org.jivesoftware.smack.Roster
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.Base64

class ArchipelClient extends Thread {

    private shouldRun = true
    public static final String PAAS_GROUP = "smlpaas"

    static File configFile = new File(System.getProperty("user.home") + File.separator + ".smlpaas-archipel")

    @Override
    void run() {
        while (shouldRun) {
            try { Thread.sleep(100) } catch (Exception e) {}
        }
    }

    Connection connect() {
        if (!configFile.exists()) {
            throw new RuntimeException("Config file ${configFile.absolutePath} does not exist. Run setup first")
        }

        Properties p = new Properties()

        configFile.withInputStream {
            p.load(it)
        }

        def username = p.getProperty("username")
        def password = new String(Base64.decode(p.getProperty("password")))
        def server = p.getProperty("server")

        Connection conn = new XMPPConnection(server);
        conn.connect();

        conn.login(username, password)

        return conn
    }

    def setup(String server, String username, String password) {

        Properties p = new Properties()

        p.setProperty("server", server)
        p.setProperty("username", username)
        p.setProperty("password", Base64.encodeBytes(password.getBytes()))

        configFile.withOutputStream { p.store(it, "saved by archipel client") }

    }

    def startVM(String vmName) {
        Connection conn = connect()

        sendMessageTo(conn, "start", vmName, { chat, msg -> println("message from ${chat.participant}: ${msg.body}"); shouldRun = false; conn.disconnect() })
    }

    def stopVM(String vmName) {
        Connection conn = connect()

        sendMessageTo(conn, "stop", vmName, { chat, msg -> println("message from ${chat.participant}: ${msg.body}"); shouldRun = false; conn.disconnect() })
    }

    def destroyVM(String vmName) {
        Connection conn = connect()

        sendMessageTo(conn, "destroy", vmName, { chat, msg -> println("message from ${chat.participant}: ${msg.body}"); shouldRun = false; conn.disconnect() })
    }

    def xml(String vmName) {
        Connection conn = connect()

        String response
        sendMessageTo(conn, "xml", vmName, {chat, msg -> response = msg.body; shouldRun = false; conn.disconnect()})

        while (shouldRun) {sleep(100)}

        return response
    }

    def sendMessageTo(Connection conn, String message, String vmName, Closure onResponse) {
        def userID = findVMByName(conn, vmName)

        def chatManager = conn.getChatManager()
        def chat = chatManager.createChat(userID, new MessageListener() {
            @Override
            void processMessage(Chat chat, Message msg) {
                onResponse(chat, msg)
            }
        })

        chat.sendMessage(message)

        start()
    }

    def findVMByName(Connection conn, String name) {
        def id

        conn.getRoster().entries.each { if (it.name == name) id = it.user }

        if (id == null)
            throw new RuntimeException("Could not find VM of name ${name}")

        return id
    }

    void cloneVM(String newVM) {
        Connection conn = connect()

        String smlpaasID

        conn.getRoster().entries.each { if (it.name == "smlpaas") smlpaasID = it.getUser() }

        conn.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual)

        conn.addPacketListener(new SubscriptionListener(conn, newVM), new SubscriptionPacketFilter())

        conn.getRoster().addRosterListener(new NewVMListener(conn, newVM, { shouldRun = false; conn.disconnect() }))

        conn.chatManager.addChatListener(new ChatManagerListener() {
            @Override
            void chatCreated(Chat chat, boolean createdLocally) {
                println "New chat with $chat.participant"
                chat.addMessageListener(new MessageListener() {
                    @Override
                    void processMessage(Chat chatT, Message message) {
                        println "Got this message $message from $chatT.participant"
                    }
                })
            }
        })

        if (smlpaasID == null) {
            System.err.println("Cannot locate smlpaas")
            System.exit(-2)
        }

        println "Cloning ${smlpaasID} to ${newVM}"

        start()

        def packet = new ArchipelPacket(smlpaasID, newVM)

        conn.sendPacket(packet)
    }

    List<VMStatus> listVMs() {
        def vms = new ArrayList<VMStatus>()

        Connection conn = connect()

        conn.getRoster().getEntries().each {
            shouldRun = true
            if (it.groups.find { it.name == PAAS_GROUP }) {
                def entry = it
                sendMessageTo(conn, "info", it.name, {
                    chat, msg ->
                        vms.add(new VMStatus(name: entry.name, id: entry.user,
                                status: msg.body.substring(0, msg.body.indexOf(","))))
                        shouldRun = false
                })
            } else {
                shouldRun = false
            }

            while (shouldRun) {
                sleep(100)
            }
        }

        conn.disconnect()

        return vms
    }
}

