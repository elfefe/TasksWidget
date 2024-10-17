import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Base64
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class SimpleMailSender(
    private val smtpServer: String,
    private val port: Int = 465, // SSL port for SMTP
    private val username: String,
    private val password: String
) {

    suspend fun sendMail(
        from: String,
        to: String,
        subject: String,
        body: String
    ) {
        val maxRetries = 5
        var attempt = 0
        var success = false
        var delayTime = 5000L // Start with 5 seconds delay

        while (!success && attempt < maxRetries) {
            try {
                attempt++
                withContext(Dispatchers.IO) {
                    // Create SSL socket for secure connection
                    val socket = SSLSocketFactory.getDefault().createSocket(smtpServer, port) as SSLSocket
                    socket.use {
                        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
                        val reader = BufferedReader(InputStreamReader(socket.inputStream))

                        // SMTP handshake and commands
                        readResponse(reader, "Initial server response")
                        writer.apply {
                            // Introduce yourself to the server
                            write("EHLO $smtpServer\r\n")
                            flush()
                            readResponse(reader, "EHLO command response")

                            // Authenticate with LOGIN method
                            write("AUTH LOGIN\r\n")
                            flush()
                            readResponse(reader, "AUTH LOGIN response")

                            // Send encoded username and password
                            write("${username.toBase64()}\r\n")
                            flush()
                            readResponse(reader, "Username response")

                            write("${password.toBase64()}\r\n")
                            flush()
                            readResponse(reader, "Password response")

                            // Specify the sender
                            write("MAIL FROM:<$from>\r\n")
                            flush()
                            readResponse(reader, "MAIL FROM response")

                            // Specify the recipient
                            write("RCPT TO:<$to>\r\n")
                            flush()
                            readResponse(reader, "RCPT TO response")

                            // Send the email data
                            write("DATA\r\n")
                            flush()
                            readResponse(reader, "DATA response")

                            // Write email headers and body
                            write("Subject: $subject\r\n")
                            write("To: $to\r\n")
                            write("From: $from\r\n")
                            write("\r\n") // Empty line to separate headers from body
                            write("$body\r\n")
                            write(".\r\n") // End of data with a single dot on a line
                            flush()
                            readResponse(reader, "End of data response")

                            // Quit the SMTP session
                            write("QUIT\r\n")
                            flush()
                        }
                    }
                }
                success = true
            } catch (e: IllegalStateException) {
                if (attempt >= maxRetries || e.message?.contains("421")?.let { !it == true } == true) {
                    throw e // Rethrow if max retries are reached or not a "Too many connections" error
                } else {
                    println("Attempt $attempt failed: ${e.message}. Retrying in ${delayTime / 1000} seconds...")
                    delay(delayTime)
                    delayTime += 5000 // Incremental backoff of 5 seconds
                }
            }
        }
    }

    private fun readResponse(reader: BufferedReader, context: String) {
        val response = reader.readLine()
        if (response == null) {
            throw IllegalStateException("SMTP error: response was null in context: $context")
        } else if (!response.startsWith("2") && !response.startsWith("3")) {
            throw IllegalStateException("SMTP error: $response in context: $context")
        }
    }

    private fun String.toBase64(): String {
        return Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
    }
}
