package com.offlinegames.host

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.offlinegames.host.databinding.ActivityMainBinding
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var server: LocalGameServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        server = LocalGameServer(applicationContext)
        setupUi()
        startServer()
    }

    private fun setupUi() {
        binding.toggleServerButton.setOnClickListener {
            if (server.isRunning()) stopServer() else startServer()
        }

        binding.createLobbyButton.setOnClickListener {
            val code = server.createLobby()
            binding.lobbyText.text = "Lobby-Code: $code"
            renderQr("http://${getLocalIpAddress()}:${server.port}/?code=$code")
        }
    }

    private fun startServer() {
        server.start()
        binding.statusText.text = "Status: läuft"
        binding.toggleServerButton.text = "Server stoppen"
        val ip = getLocalIpAddress()
        binding.ipText.text = "Adresse: $ip:${server.port}"
        if (server.currentLobbyCode().isNotBlank()) {
            binding.lobbyText.text = "Lobby-Code: ${server.currentLobbyCode()}"
        }
    }

    private fun stopServer() {
        server.stop()
        binding.statusText.text = "Status: gestoppt"
        binding.toggleServerButton.text = "Server starten"
    }

    private fun getLocalIpAddress(): String {
        NetworkInterface.getNetworkInterfaces().toList().forEach { intf ->
            intf.inetAddresses.toList().forEach { addr ->
                if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                    return addr.hostAddress ?: "0.0.0.0"
                }
            }
        }
        return "0.0.0.0"
    }

    private fun renderQr(content: String) {
        val size = 512
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        binding.qrImage.setImageBitmap(bmp)
    }
}
