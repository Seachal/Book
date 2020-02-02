package com.zia.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.zia.App
import java.io.*


/**
 * Created by zia on 2018/10/13.
 */
object FileUtil {
    fun getFileUri(context: Context, file: File): Uri {

        return if (Build.VERSION.SDK_INT >= 24) {
            val uri = FileProvider.getUriForFile(context, "com.zia.book.FileProvider", file)
            Log.d("FileUtil", uri.toString())
            uri
        } else {
            val uri = Uri.fromFile(file)
            Log.d("FileUtil", uri.toString())
            uri
        }
    }

    fun writeFile(filePath: String, str: String) {
        val file = File(filePath)
        if (!file.exists()) {
            file.createNewFile()
        }
        val out = BufferedWriter(OutputStreamWriter(FileOutputStream(file)))
        out.write(str)
        out.flush()
    }

    fun getString(filePath: String): String {
        val file = File(filePath)
        if (!file.exists()) {
            file.createNewFile()
        }
        val inputStream = BufferedReader(InputStreamReader(FileInputStream(file), "UTF-8"))
        var str = ""
        val sb = StringBuilder()
        while (true) {
            str = inputStream.readLine() ?: break
            sb.append(str)
        }
        inputStream.close()
        return sb.toString()
    }

    fun saveImage(bmp: Bitmap, path: String): String? {
        val file = File(path)
        try {
            val fos = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            return file.path
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    val fileDirPath: String = App.getContext().filesDir.path
    val rulePath: String = fileDirPath + File.separator + "easybookRules.json"
    val fontDirPath: String = fileDirPath + File.separator + "font"
    val customBgFile = File(
        App.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "custom_background.png"
    )
    val customBgPath = customBgFile.path
}
