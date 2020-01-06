const fetch = require('node-fetch')

const server = {
  ip: '127.0.0.1',
  port: '8769'
}

const packetDelayMs = 100

const serverURI = `http://${server.ip}:${server.port}`
const sendPacketURI = `${serverURI}/send`
const receivePacketURI = `${serverURI}/receive`

const pause = ms => new Promise(res => setTimeout(res, ms))

const call = (uri, packet) => fetch(uri, { method: 'POST', body: packet }).then(() => pause(packetDelayMs))
const sendPacket = packet => call(sendPacketURI, packet)
const receivePacket = packet => call(receivePacketURI, packet)


const start = async () => {
  for (let i = 0; i <= 7; i++)
    await sendPacket(`eD${i}`)
}

start()
