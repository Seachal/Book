package com.zia.page.preview.custom

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.zia.bookdownloader.R
import com.zia.util.FileUtil
import com.zia.util.ToastUtil
import com.zia.util.defaultSharedPreferences
import com.zia.util.editor
import com.zia.widget.reader.utils.ScreenUtils
import kotlinx.android.synthetic.main.activity_custom_theme.*
import java.io.File


class CustomThemeActivity : AppCompatActivity() {

    private val TAG = "CustomThemeActivity"
    private val CHOOSE_PICTURE = 2
    private val CROP_PICTURE = 3

    private val screenParams by lazy {
        val s = ScreenUtils.getScreenSize(this@CustomThemeActivity)
        Log.d(TAG, "width:${s[0]}  height=${s[1]}")
        s
    }

    private val defaultColor by lazy { resources.getColor(R.color.grey) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_theme)

        initView()
        initClickListener()
    }

    private fun initClickListener() {
        custom_choose_text_color.setOnClickListener {
            showColorPicker(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {
                }

                override fun onColorSelected(dialogId: Int, color: Int) {
                    val stringColor = String.format("#%06X", (0xFFFFFFFF and color.toLong()))
                    custom_text_color.text = stringColor
                    custom_text_color.setTextColor(color)
                    custom_text_preview.setTextColor(color)
                    defaultSharedPreferences.editor {
                        putInt(CustomThemeConst.custom_textColor_sp, color)
                    }
                }

            })
        }

        custom_choose_bg_color.setOnClickListener {
            showColorPicker(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {
                }

                override fun onColorSelected(dialogId: Int, color: Int) {
                    Log.d(TAG, "custom_choose_bg_color : $color")
                    val stringColor = getStringColor(color)
                    custom_bg_color.text = stringColor
                    custom_bg_color.setTextColor(color)
                    custom_img_path.text = getString(R.string.no_path)
                    custom_bg_preview.setImageBitmap(null)
                    custom_bg_preview.setBackgroundColor(color)
                    defaultSharedPreferences.editor {
                        putInt(CustomThemeConst.custom_bgColor_sp, color)
                        remove(CustomThemeConst.custom_bgImgPath_sp)
                    }
                }

            })
        }

        custom_choose_img.setOnClickListener {
            custom_bg_preview.post {
                val pickIntent = Intent(Intent.ACTION_PICK)
                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "*/*")
                startActivityForResult(pickIntent, CHOOSE_PICTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) {
            ToastUtil.onNormal("resultCode != Activity.RESULT_OK || data == null")
            return
        }
        when (requestCode) {
            CHOOSE_PICTURE -> {
                Log.d(TAG, "CHOOSE_PICTURE")
                if (data.data != null) {
                    // 调用系统中自带的图片剪裁
                    val intent = Intent("com.android.camera.action.CROP")
                    intent.flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    intent.setDataAndType(data.data, "image/*")
                    // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
                    intent.putExtra("crop", "true")
                    intent.putExtra("outputX", screenParams[0])
                    intent.putExtra("outputY", screenParams[1])
                    // aspectX aspectY 是宽高的比例
                    intent.putExtra("aspectX", screenParams[0])
                    intent.putExtra("aspectY", screenParams[1])
                    intent.putExtra("return-data", false)
                    intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG)
                    intent.putExtra("noFaceDetection", true)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(FileUtil.customBgFile))

                    startActivityForResult(intent, CROP_PICTURE)
                } else {
                    ToastUtil.onInfo("data == null")
                }
            }
            CROP_PICTURE -> {
                Log.d(TAG, "CROP_PICTURE")
                Thread(Runnable {
                    try {
                        defaultSharedPreferences.editor {
                            putString(
                                CustomThemeConst.custom_bgImgPath_sp,
                                FileUtil.customBgPath
                            )
                        }
                        runOnUiThread {
                            custom_bg_preview.setImageURI(
                                FileUtil.getFileUri(this, FileUtil.customBgFile)
                            )
                            custom_img_path.text = FileUtil.customBgPath
                            custom_bg_color.text = ""
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }).start()
            }
        }
    }

    private fun showColorPicker(colorPickerDialogListener: ColorPickerDialogListener) {
        val dialog = ColorPickerDialog.newBuilder()
            .setShowAlphaSlider(false)
            .create()
        dialog.setColorPickerDialogListener(colorPickerDialogListener)
        dialog.show(supportFragmentManager, "选择颜色")
    }

    private fun initView() {
        custom_bg_preview.post {
            val bgParams = custom_bg_preview.layoutParams
            bgParams.width = screenParams[0]
            bgParams.height = screenParams[1]
            custom_bg_preview.layoutParams = bgParams

            val tvParams = custom_text_preview.layoutParams
            tvParams.width = screenParams[0]
            tvParams.height = screenParams[1]
            custom_text_preview.layoutParams = tvParams

            resetTheme()
        }
    }

    private fun resetTheme() {
        Thread(Runnable {
            val tc =
                defaultSharedPreferences.getInt(CustomThemeConst.custom_textColor_sp, defaultColor)
            val bc =
                defaultSharedPreferences.getInt(CustomThemeConst.custom_bgColor_sp, defaultColor)
            val imgPath = defaultSharedPreferences.getString(
                CustomThemeConst.custom_bgImgPath_sp,
                getString(R.string.no_path)
            )
            val fontPath = defaultSharedPreferences.getString("fontPath", "")

            var bitmap: Bitmap? = null
            if (File(imgPath!!).exists()) {
                bitmap = BitmapFactory.decodeFile(imgPath)
            }

            runOnUiThread {
                custom_text_color.setTextColor(tc)
                custom_text_color.text = getStringColor(tc)
                custom_text_preview.setTextColor(tc)

                custom_bg_color.setTextColor(bc)
                custom_bg_color.text = getStringColor(bc)
                custom_bg_preview.setBackgroundColor(bc)

                custom_img_path.text = imgPath

                bitmap?.let {
                    custom_bg_preview.setImageBitmap(it)
                }

                if (fontPath != null && fontPath.isNotEmpty()) {
                    //我认为有可能用户清除缓存后，加载了不存在的文件会报错
                    try {
                        custom_text_preview.typeface = Typeface.createFromFile(fontPath)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        ToastUtil.onError(e.message)
                    }
                }
            }
        }).start()
    }

    private fun getStringColor(color: Int): String {
        return String.format("#%06X", (0xFFFFFF and color))
    }
}
