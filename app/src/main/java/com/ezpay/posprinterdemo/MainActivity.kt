package com.ezpay.posprinterdemo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import net.posprinter.posprinterface.IMyBinder
import net.posprinter.posprinterface.ProcessData
import net.posprinter.posprinterface.TaskCallback
import net.posprinter.service.PosprinterService
import net.posprinter.utils.DataForSendToPrinterPos58
import net.posprinter.utils.PosPrinterDev
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var usbList:List<String>? = null
    private var usblist:List<String>? = null
    private var adapter: ArrayAdapter<String>? = null

    lateinit var textViewTitle:TextView
    lateinit var listViewItem:ListView

    var ISCONNECT = false
    var myBinder: IMyBinder? = null
    var usbDev = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //bind service，get imyBinder
        val intent = Intent(this, PosprinterService::class.java)
        bindService(intent, mSerconnection, BIND_AUTO_CREATE)

        btn.setOnClickListener(this)
        connect.setOnClickListener(this)
        disconnect.setOnClickListener(this)
        btnPrintText.setOnClickListener(this)
    }

    var mSerconnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            myBinder = service as IMyBinder
            Log.e("myBinder", "connect")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.e("myBinder", "disconnect")
        }
    }

    /**斷開連接*/
    private fun disConnect() {
        if (ISCONNECT) {
            myBinder?.DisconnectCurrentPort(object : TaskCallback {
                override fun OnSucceed() {
                    ISCONNECT = false
                    Toast.makeText(applicationContext, "disconnect ok", Toast.LENGTH_SHORT).show()
                }

                override fun OnFailed() {
                    ISCONNECT = true
                    Toast.makeText(applicationContext, "disconnect failed", Toast.LENGTH_SHORT)
                            .show()
                }
            })
        }
    }

    /**連接USB*/
    private fun connectUSB() {
        val usbAddress: String = etAddress.text.toString().trim { it <= ' ' }

        if (usbAddress == "") {
            Toast.makeText(applicationContext, getString(R.string.discon), Toast.LENGTH_SHORT).show()
        } else {
            myBinder?.ConnectUsbPort(applicationContext, usbAddress, object : TaskCallback {
                override fun OnSucceed() {
                    ISCONNECT = true
                    Toast.makeText(applicationContext, getString(R.string.connect), Toast.LENGTH_SHORT).show()
                }

                override fun OnFailed() {
                    ISCONNECT = false
                    Toast.makeText(applicationContext, getString(R.string.discon), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    @SuppressLint("SetTextI18n")
    fun setUSB(){
        var dialogView: View? = null
        val inflater = LayoutInflater.from(this)
        dialogView = inflater.inflate(R.layout.usb_link, null)

        textViewTitle = dialogView.findViewById<View>(R.id.textView1) as TextView
        listViewItem = dialogView.findViewById<View>(R.id.listView1) as ListView

        usbList = PosPrinterDev.GetUsbPathNames(this)
        if (usbList == null) {
            usbList = ArrayList<String>()
        }

        usblist = usbList

        textViewTitle.text = getString(R.string.usb_pre_con) + usbList?.size

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, usbList!!)
        listViewItem.adapter = adapter

        val dialog: AlertDialog = AlertDialog.Builder(this)
                .setView(dialogView).create()
        dialog.show()

        setUsbLisener(dialog)
    }

    fun setUsbLisener(dialog: AlertDialog) {
        listViewItem.onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
            usbDev = usbList!![i]
            tvAddress.text = usbDev
            dialog.cancel()

            etAddress.text = usbDev

            Log.e("usbDev: ", usbDev)
        }
    }

    override fun onClick(position: View?) {
        when(position?.id){
            R.id.btn -> {
                setUSB()
            }

            R.id.connect -> {
                connectUSB()
            }

            R.id.disconnect -> {
                disConnect()
            }

            R.id.btnPrintText -> {
                printSample()
            }
        }
    }

    fun strTobytes(str: String): ByteArray? {
        val b: ByteArray? = null
        var data: ByteArray? = null
        try {
            val gbkTransChinese = String(str.toByteArray(charset("GBK")), StandardCharsets.ISO_8859_1) //轉換gbk編碼
            val unicodeTransChinese = String(gbkTransChinese.toByteArray(StandardCharsets.ISO_8859_1), Charset.forName("GBK"))
            data = unicodeTransChinese.toByteArray(charset("GBK"))
        } catch (var4: UnsupportedEncodingException) {
            var4.printStackTrace()
        }

        return data
    }

    /**
     * 打印样张
     */
    private fun printSample() {
        if (ISCONNECT) {
            myBinder?.WriteSendData(object : TaskCallback {
                override fun OnSucceed() {
                    Toast.makeText(applicationContext, getString(R.string.con_success), Toast.LENGTH_SHORT).show()
                }

                override fun OnFailed() {
                    Toast.makeText(applicationContext, getString(R.string.con_failed), Toast.LENGTH_SHORT).show()
                }
            }, ProcessData {
                val list: MutableList<ByteArray> = ArrayList()
                list.add(DataForSendToPrinterPos58.initializePrinter())
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(30, 0)) //设置初始位置
                list.add(DataForSendToPrinterPos58.selectCharacterSize(17)) //字体放大一倍
                list.add(strTobytes("商品")!!)
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(220, 0))
                list.add(strTobytes("項目")!!)
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.initializePrinter())
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(30, 0))
                list.add(strTobytes("黃悶雞")!!)
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(220, 0))
                list.add(strTobytes("5元")!!)
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.initializePrinter())
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(30, 0))
                list.add(strTobytes("炸腿")!!)
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(220, 0))
                list.add(strTobytes("6元")!!)
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.initializePrinter())
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(30, 0))
                list.add(strTobytes("腿排")!!)
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(220, 0))
                list.add(strTobytes("7元")!!)
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.initializePrinter())
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(30, 0))
                list.add(strTobytes("披薩套餐")!!)
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(220, 0))
                list.add(strTobytes("8元")!!)
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.initializePrinter())
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(30, 0))
                list.add(strTobytes("牛肉餅")!!)
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(220, 0))
                list.add(strTobytes("9元")!!)
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.initializePrinter())
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(30, 0))
                list.add(strTobytes("黃悶雞")!!)
                list.add(DataForSendToPrinterPos58.setAbsolutePrintPosition(220, 0))
                list.add(strTobytes("10元")!!)
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.printAndFeedLine())
                list.add(DataForSendToPrinterPos58.printAndFeedLine())

                list
            })
        } else {
            Toast.makeText(applicationContext, getString(R.string.connect_first), Toast.LENGTH_SHORT).show()
        }
    }
}