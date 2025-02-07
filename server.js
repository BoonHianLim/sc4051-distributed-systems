const dgram = require("dgram");
const server = dgram.createSocket("udp4");

server.on("message", (msg, rinfo) => {
  console.log(`Received raw buffer:`, msg); // Default Buffer output
  console.log(`Hex dump:`, msg.toString("hex")); // Hex representation
  console.log(`Byte array:`, [...msg]); // Convert to array of numbers
  server.send(msg, rinfo.port, rinfo.address, (err) => {
    if (err) {
      console.error(err);
    } else {
      console.log(`Sent response to ${rinfo.address}:${rinfo.port}`);
    }
  });
});

server.on("listening", () => {
  const address = server.address();
  console.log(`Server listening on ${address.address}:${address.port}`);
});

server.bind(12000); // Bind to port 41234
