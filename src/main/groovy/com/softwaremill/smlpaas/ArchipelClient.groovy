package com.softwaremill.smlpaas
import com.softwaremill.smlpaas.packets.ArchipelPacket
import org.jivesoftware.smack.Chat
import org.jivesoftware.smack.Connection
import org.jivesoftware.smack.MessageListener
import org.jivesoftware.smack.Roster
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.Base64

class ArchipelClient extends Thread {

    private static shouldRun = true
    public static final String PAAS_GROUP = "smlpaas"

    static File configFile = new File(System.getProperty("user.home") + File.separator + ".smlpaas-archipel")

    @Override
    void run() {
        while (shouldRun) {
            try { Thread.sleep(100) } catch (Exception e) {}
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: COMMAND")
            System.exit(-1)
        }

        switch (args[0]) {
            case "setup":
            case "config":
                setup(args.drop(1))
                break
            case "clone":
                cloneVM(args.drop(1))
                break
            case "list":
                listVMs()
                break
            case "start":
                startVM(args.drop(1))
                break
            case "stop":
                stopVM(args.drop(1))
                break
            default:
                println "Unknown command: ${args[2]}"
                break
        }
    }

    static Connection connect() {
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

    static def setup(String[] args) {
        if (args.length != 3) {
            println "Usage config SERVER USERNAME PASSWORD"
        }

        Properties p = new Properties()

        p.setProperty("server", args[0])
        p.setProperty("username", args[1])
        p.setProperty("password", Base64.encodeBytes(args[2].getBytes()))

        configFile.withOutputStream {p.store(it, "saved by archipel client")}

    }

    static def startVM(String[] args) {
        Connection conn = connect()

        if (args.length != 1) {
            println "Usage USERNAME PASSWORD start VM_NAME"
        }

        sendMessageTo(conn, "start", args[0])
    }

    static def stopVM(String[] args) {
        Connection conn = connect()

        if (args.length != 1) {
            println "Usage USERNAME PASSWORD stop VM_NAME"
        }

        sendMessageTo(conn, "stop", args[0])
    }

    static def sendMessageTo(Connection conn, String message, String vmName) {
        def userID = findVMByName(conn, vmName)

        def chatManager = conn.getChatManager()
        def chat = chatManager.createChat(userID, new MessageListener() {
            @Override
            void processMessage(Chat chat, Message msg) {
                // we do not really care
                println("message from ${chat.participant}: ${msg.body}")
                shouldRun = false
            }
        })

        chat.sendMessage(message)

        new ArchipelClient().start()
    }

    static def findVMByName(Connection conn, String name) {
        def id

        conn.getRoster().entries.each { if (it.name == name) id = it.user }

        if (id == null)
            throw new RuntimeException("Could not find VM of name ${name}")

        return id
    }

    static void cloneVM(String[] args) {
        if (args.length != 1) {
            println "Usage: USERNAME PASSWORD clone NEW_VM_NAME"
        }

        Connection conn = connect()

        def newVM = args[0]

        String smlpaasID

        conn.getRoster().entries.each { if (it.name == "smlpaas") smlpaasID = it.getUser() }

        conn.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual)

        conn.addPacketListener(new SubscriptionListener(conn, newVM), new SubscriptionPacketFilter())

        conn.getRoster().addRosterListener(new NewVMListener(conn, newVM, { shouldRun = false }))

        if (smlpaasID == null) {
            System.err.println("Cannot locate smlpaas")
            System.exit(-2)
        }

        println "Cloning ${smlpaasID} to ${newVM}"

        new ArchipelClient().start()

        def packet = new ArchipelPacket(smlpaasID, newVM)

        conn.sendPacket(packet)
    }

    static void listVMs() {
        println "Name: ID"
        println "========"

        Connection conn = connect()

        conn.getRoster().getEntries().each {
            if (it.groups.find { it.name == PAAS_GROUP }) {
                println "${it.name}: ${it.user}"
            }
        }
    }
}

