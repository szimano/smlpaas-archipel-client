package com.softwaremill.smlpaas
import com.bethecoder.ascii_table.ASCIITable

class ArchipelClientMain {

    static ArchipelClient client = new ArchipelClient()

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
            case "status":
            case "list":
                listVMs()
                break
            case "start":
                startVM(args.drop(1))
                break
            case "stop":
                stopVM(args.drop(1))
                break
            case "destroy":
                destroyVM(args.drop(1))
                break
            default:
                println "Unknown command: ${args[2]}"
                break
        }
    }

    static def setup(String[] args) {
        if (args.length != 3) {
            println "Usage: config SERVER USERNAME PASSWORD"
            return
        }

        client.setup(args[0], args[1], args[2])
    }

    static def startVM(String[] args) {
        if (args.length != 1) {
            println "Usage: start VM_NAME"
            return
        }

        client.startVM(args[0])
    }

    static def stopVM(String[] args) {
        if (args.length != 1) {
            println "Usage: stop VM_NAME"
            return
        }

        client.stopVM(args[0])
    }

    static def destroyVM(String[] args) {
        if (args.length != 1) {
            println "Usage: destroy VM_NAME"
            return
        }

        client.destroyVM(args[0])
    }

    static void cloneVM(String[] args) {
        if (args.length != 1) {
            println "Usage: USERNAME PASSWORD clone NEW_VM_NAME"
            return
        }

        def newVM = args[0]

        client.cloneVM(newVM)
    }

    static void listVMs() {
        def vms = client.listVMs();

        String[] header = ["Name", "ID", "Status"]

        List<List<String>> data = new ArrayList<>()

        vms.each {
            List<String> s = new ArrayList<>()
            s.add(it.name)
            s.add(it.id)
            s.add(it.status)

            data.add(s)
        }

        if (data.empty) {
            println "No VMs found"
        }
        else {
            ASCIITable.getInstance().printTable(header, data as String[][]);
        }
    }
}

