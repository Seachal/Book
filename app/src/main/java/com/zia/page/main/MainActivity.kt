package com.zia.page.main

import android.Manifest
import android.app.ProgressDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import com.zia.bookdownloader.R
import com.zia.database.bean.Config
import com.zia.easybookmodule.engine.EasyBook
import com.zia.page.base.BaseActivity
import com.zia.util.FileUtil
import com.zia.util.ToastUtil
import com.zia.util.Version
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener,EasyPermissions.PermissionCallbacks {

    private lateinit var mainPagerAdapter: MainPagerAdapter
    private lateinit var viewModel: MainViewModel

    private val dialog by lazy {
        val dialog = ProgressDialog(this@MainActivity)
        dialog.setCancelable(false)
        dialog.progress = 0
        dialog.setTitle("正在下载")
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.show()
        dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val goFragment = intent.getIntExtra("goFragment", 0)

        main_nav.setOnNavigationItemSelectedListener(this@MainActivity)

        mainPagerAdapter = MainPagerAdapter(supportFragmentManager)
        setViewPager(goFragment)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        initObserver()

        //请求权限
        requestPermission()

        //提示更新解析版本
        viewModel.checkVersion("book", TYPE_APP)
        viewModel.getAllLatestVersion()

        //添加shortcut
//        viewModel.addSearchShortcut(this)
    }

    private var firstCheck = true

    private fun initObserver() {
        viewModel.config.observe(this, Observer {
            if (it == null) return@Observer
            if (it.data.able != "true") {
                //服务器认证失败，无法访问
                showErrorDialog("由于某些原因，软件不再提供使用")
                return@Observer
            }
            Log.e(javaClass.simpleName, it.type)
            if (it.type == TYPE_APP) {
                if (it.data.version > Version.packageCode()) {
                    showUpdateDialog(it.data, "book.apk", TYPE_APP)
                } else {
                    if (firstCheck) {
                        firstCheck = false
                    } else {
                        ToastUtil.onInfo("已经是最新了")
                    }
                }
                return@Observer
            } else if (it.type == TYPE_FIX) {
                if (it.data.version > EasyBook.getVersion()) {
                    showUpdateDialog(it.data, "easybookfix.apk", TYPE_FIX)
                } else {
                    if (firstCheck) {
                        firstCheck = false
                    } else {
                        ToastUtil.onInfo("已经是最新了")
                    }
                }
                return@Observer
            }
        })

        //文件下载完成监听
        viewModel.file.observe(this, Observer {
            dialog.dismiss()
            if (it == null) {
                return@Observer
            }
            if (it.type == TYPE_APP) {
                val intent = Intent(Intent.ACTION_VIEW)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                } else {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                intent.setDataAndType(
                    FileUtil.getFileUri(this@MainActivity, it.data),
                    "application/vnd.android.package-archive"
                )
                this@MainActivity.startActivity(intent)
            } else if (it.type == TYPE_FIX) {
//                DefaultExecutorSupplier.getInstance().forBackgroundTasks().execute {
//                    try {
//                        Log.e("MainActivity", it.data.path)
//                        val applied = Stark.get().applyPatch(App.getContext(), it.data.path)
//                        Log.d(javaClass.simpleName, "applied:$applied")
//                        if (applied) {
//                            Stark.get().load(App.getContext())
//                            runOnUiThread {
//                                ToastUtil.onSuccess("修复完成，重启生效")
//                            }
//                        }
//                    } catch (e: Exception) {
//                        runOnUiThread {
//                            ToastUtil.onSuccess("修复失败")
//                        }
//                    } finally {
//                        runOnUiThread {
//                            waitDialog.dismiss()
//                        }
//                    }
//                }
            }
        })

        viewModel.dialogMessage.observe(this, Observer {
            dialog.setProgressNumberFormat(it)
        })

        viewModel.dialogProgress.observe(this, Observer {
            dialog.progress = it ?: 0
        })

        viewModel.toast.observe(this, Observer {
            ToastUtil.onNormal(it)
        })
    }

    @AfterPermissionGranted(1)
    private fun requestPermission() {
        if (!EasyPermissions.hasPermissions(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            EasyPermissions.requestPermissions(
                this, "需要磁盘读写权限，系统设置写入权限（亮度调节）", 1, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun setViewPager(position: Int = 0) {
        main_vp.adapter = mainPagerAdapter
        main_vp.offscreenPageLimit = 1
        main_vp.currentItem = position
        main_vp.setScrollable(false)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.bookrack -> {
                main_vp.currentItem = 0
                return true
            }
            R.id.search -> {
                main_vp.currentItem = 1
                return true
            }
            R.id.setting -> {
                main_vp.currentItem = 2
                return true
            }
        }
        return false
    }

    private fun showErrorDialog(massage: String) {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage(massage)
            .setPositiveButton("确定") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showUpdateDialog(config: Config, fileName: String, type: String) {
        val title = if (type == TYPE_APP) {
            "有新的版本可以更新~"
        } else {
            "有新的网站解析可以更新~"
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(config.message)
            .setNegativeButton("取消", null)
            .setPositiveButton("更新") { _, _ ->
                dialog.show()
                viewModel.download(config.url, fileName, type)
            }
            .setCancelable(true)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        finish()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
