// The PORT and SHOULD_PING parameters can be changed for testing purposes.
const PORT = 1337
const SHOULD_PING = true

const VERSION = '1.0'

let net = require('net')
let clients = []
let receivedPong = false
const CMD_CONN = 'CONN'
const CMD_BCST = 'BCST'
const CMD_PONG = 'PONG'
const CMD_QUIT = 'QUIT'
const STAT_CONNECTED = 'CONNECTED'

let server = net.createServer(function(socket) {
    clients.push(socket)
    sendToClient(socket, `INFO Welcome to the server ${VERSION}`)
    socket.on('data', function(data) {
        console.log(data.toString())
        let command = parseCommand(data.toString())
        let client = clients.find(c =>  c === this)
        console.log(`<< [${client.username}] ${command.type} ${command.payload}`)

        if (client.status === undefined) {
            // User needs to provide a valid username first.
            if (command.type !== CMD_CONN) {
                sendToClient(client, '403 Please log in first')
            } else if (!validUsernameFormat(command.payload)) {
                sendToClient(client, '400 Username has an invalid format (only characters, numbers and underscores are allowed)')
            } else if (userExists(command.payload)) {
                sendToClient(client, '401 User already logged in')
            } else {
                // Everything works out.
                client.username = command.payload
                client.status = STAT_CONNECTED
                sendToClient(client, `200 ${command.payload}`)
                stats()
                if (SHOULD_PING) {
                    // Start heartbeat.
                    heartbeat(client)
                }
            }
        } else {
            // User has provided a username.
            if (command.type === CMD_QUIT) {
                sendToClient(client, '200 Goodbye')
                client.destroy()
            } else if (command.type === CMD_BCST) {
                let connected = clients.filter(c => c.status === STAT_CONNECTED)
                for(const other of connected) {
                    if (client !== other) {
                        sendToClient(other, `${CMD_BCST} ${client.username} ${command.payload}`)
                    }
                }
            } else if (command.type === CMD_PONG) {
                receivedPong = true
            } else {
                // Any other command we cannot process.
                sendToClient(client, '400 Unknown command')
            }
        }
    })
    socket.on('close', function() {
        console.log('Connection closed')
        for(let i = 0; i < clients.length; i++) {
            if (clients[i] === this) {
                clients.splice(i, 1)
            }
        }
        stats()
    })
})

function parseCommand(command) {
    // Remove trailing newline (\n)
    let clean = command.substr(0, command.length - 1)
    let parts = clean.split(' ')
    return {
        type: parts[0],
        payload: clean.substr(parts[0].length + 1)
    }
}

function validUsernameFormat(username) {
    let pattern = '[a-zA-Z0-9_]{3,14}'
    return !!username.match(pattern)
}

function userExists(username) {
    let client = clients.find(c => c.username === username)
    return client !== undefined
}

function sendToClient(client, message) {
    if (clients.find(c =>  c === client)) {
        console.log(client.username)
        console.log(message)
        console.log(message.byteLength)
        console.log('[ ' + client.username + '] ' + message)
        client.write(`${message}\n`)
    } else {
        console.log(`Skipped send (${message}): client not active any more`)
    }
}

function heartbeat(client) {
    console.log(`~~ [${client.username}] Heartbeat initiated`)
    setTimeout(function () {
        receivedPong = false
        sendToClient(client, 'PING')
        setTimeout(function () {
            if (receivedPong) {
                console.log(`~~ [${client.username}] Heartbeat expired - SUCCESS`)
                heartbeat(client)
            } else {
                console.log(`~~ [${client.username}] Heartbeat expired - FAILED`)
                sendToClient(client, 'DCSN')
                client.destroy()
            }
        }, 3 * 1000)
    }, 10 * 1000)
}

function stats() {
    console.log(`Total number of clients: ${clients.length}`)
    let connected = clients.filter(c => c.status === STAT_CONNECTED)
    console.log(`Number of connected clients: ${connected.length}`)

}
console.log(`Starting server version ${VERSION}.`)
console.log(`Press 'control-C' to quit the server.`)
server.listen(PORT, '127.0.0.1')
