package com.gothwad.manager

import android.content.Context
import android.os.Environment
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class SimpleFtpServer(
    private val context: Context,
    private val port: Int = 2121
) {

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val rootDir: File by lazy {
        Environment.getExternalStorageDirectory() ?: context.filesDir
    }

    fun start(): Boolean {
        if (isRunning.get()) return true
        try {
            serverSocket = ServerSocket(port)
            isRunning.set(true)
            Thread {
                while (isRunning.get()) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        Thread { handleClient(client) }.start()
                    } catch (e: Exception) {
                        break
                    }
                }
            }.start()
            return true
        } catch (e: Exception) {
            isRunning.set(false)
            return false
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverSocket = null
    }

    fun isServerRunning(): Boolean = isRunning.get()

    private fun handleClient(controlSocket: Socket) {
        var currentDir = rootDir
        var passiveServer: ServerSocket? = null

        try {
            val reader = BufferedReader(InputStreamReader(controlSocket.getInputStream(), Charsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(controlSocket.getOutputStream(), Charsets.UTF_8))

            fun sendResponse(code: Int, text: String) {
                writer.write("$code $text\r\n")
                writer.flush()
            }

            sendResponse(220, "Gothwad Android TV FTP Ready")

            while (isRunning.get()) {
                val line = reader.readLine() ?: break
                val parts = line.trim().split(" ", limit = 2)
                val cmd = parts[0].uppercase(Locale.US)
                val arg = if (parts.size > 1) parts[1] else ""

                when (cmd) {
                    "USER" -> sendResponse(331, "User accepted, password required")
                    "PASS" -> sendResponse(230, "User logged in successfully")
                    "SYST" -> sendResponse(215, "UNIX Type: L8")
                    "FEAT" -> {
                        writer.write("211-Features:\r\n UTF8\r\n211 End\r\n")
                        writer.flush()
                    }
                    "OPTS" -> sendResponse(200, "OK")
                    "TYPE" -> sendResponse(200, "Type set to $arg")
                    "PWD" -> {
                        val rel = currentDir.absolutePath.removePrefix(rootDir.absolutePath).ifEmpty { "/" }
                        sendResponse(257, "\"$rel\" is current directory")
                    }
                    "CWD" -> {
                        val target = resolvePath(currentDir, arg)
                        if (target.exists() && target.isDirectory && target.canRead()) {
                            currentDir = target
                            val rel = currentDir.absolutePath.removePrefix(rootDir.absolutePath).ifEmpty { "/" }
                            sendResponse(250, "Directory successfully changed to \"$rel\"")
                        } else {
                            sendResponse(550, "Failed to change directory")
                        }
                    }
                    "CDUP" -> {
                        if (currentDir.absolutePath != rootDir.absolutePath && currentDir.parentFile != null) {
                            currentDir = currentDir.parentFile!!
                            sendResponse(200, "Directory changed to parent")
                        } else {
                            sendResponse(200, "Already at root")
                        }
                    }
                    "PASV" -> {
                        passiveServer?.close()
                        passiveServer = ServerSocket(0)
                        val localIp = controlSocket.localAddress.hostAddress ?: "127.0.0.1"
                        val ipParts = localIp.split(".").map { it.toIntOrNull() ?: 0 }
                        val pasvPort = passiveServer.localPort
                        val p1 = pasvPort / 256
                        val p2 = pasvPort % 256
                        sendResponse(227, "Entering Passive Mode (${ipParts.joinToString(",")},$p1,$p2)")
                    }
                    "LIST" -> {
                        val dataSocket = passiveServer?.accept()
                        sendResponse(150, "Here comes the directory listing")
                        if (dataSocket != null) {
                            val dataWriter = BufferedWriter(OutputStreamWriter(dataSocket.getOutputStream(), Charsets.UTF_8))
                            val files = currentDir.listFiles() ?: emptyArray()
                            val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.US)
                            for (file in files) {
                                val isD = if (file.isDirectory) "d" else "-"
                                val size = file.length()
                                val dateStr = sdf.format(Date(file.lastModified()))
                                val lineStr = String.format(Locale.US, "%srwxr-xr-x 1 owner group %10d %s %s\r\n", isD, size, dateStr, file.name)
                                dataWriter.write(lineStr)
                            }
                            dataWriter.flush()
                            dataSocket.close()
                            passiveServer?.close()
                            passiveServer = null
                        }
                        sendResponse(226, "Directory send OK")
                    }
                    "RETR" -> {
                        val targetFile = resolvePath(currentDir, arg)
                        if (!targetFile.exists() || targetFile.isDirectory) {
                            sendResponse(550, "File not found")
                        } else {
                            val dataSocket = passiveServer?.accept()
                            sendResponse(150, "Opening binary mode data connection")
                            if (dataSocket != null) {
                                FileInputStream(targetFile).use { input ->
                                    val out = dataSocket.getOutputStream()
                                    val buffer = ByteArray(32 * 1024)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        out.write(buffer, 0, read)
                                    }
                                    out.flush()
                                }
                                dataSocket.close()
                                passiveServer?.close()
                                passiveServer = null
                            }
                            sendResponse(226, "Transfer complete")
                        }
                    }
                    "STOR" -> {
                        val targetFile = resolvePath(currentDir, arg)
                        val dataSocket = passiveServer?.accept()
                        sendResponse(150, "Ok to send data")
                        if (dataSocket != null) {
                            FileOutputStream(targetFile).use { output ->
                                val inStream = dataSocket.getInputStream()
                                val buffer = ByteArray(32 * 1024)
                                var read: Int
                                while (inStream.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                }
                                output.flush()
                            }
                            dataSocket.close()
                            passiveServer?.close()
                            passiveServer = null
                        }
                        sendResponse(226, "File stored successfully")
                    }
                    "DELE" -> {
                        val target = resolvePath(currentDir, arg)
                        if (target.exists() && !target.isDirectory && target.delete()) {
                            sendResponse(250, "File deleted")
                        } else {
                            sendResponse(550, "Could not delete file")
                        }
                    }
                    "MKD" -> {
                        val newDir = resolvePath(currentDir, arg)
                        if (!newDir.exists() && newDir.mkdirs()) {
                            sendResponse(257, "\"${newDir.name}\" created")
                        } else {
                            sendResponse(550, "Create directory failed")
                        }
                    }
                    "RMD" -> {
                        val target = resolvePath(currentDir, arg)
                        if (target.exists() && target.isDirectory && FileUtils.deleteRecursively(target)) {
                            sendResponse(250, "Directory removed")
                        } else {
                            sendResponse(550, "Remove directory failed")
                        }
                    }
                    "QUIT" -> {
                        sendResponse(221, "Goodbye")
                        break
                    }
                    else -> sendResponse(502, "Command not implemented")
                }
            }
        } catch (ignored: Exception) {
        } finally {
            try { passiveServer?.close() } catch (ignored: Exception) {}
            try { controlSocket.close() } catch (ignored: Exception) {}
        }
    }

    private fun resolvePath(base: File, input: String): File {
        var clean = input.trim()
        if (clean.startsWith("/")) {
            clean = clean.substring(1)
            return File(rootDir, clean)
        }
        return File(base, clean)
    }
}
