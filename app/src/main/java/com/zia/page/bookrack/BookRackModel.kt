package com.zia.page.bookrack

import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.zia.App
import com.zia.database.AppDatabase
import com.zia.database.bean.BookCache
import com.zia.database.bean.LocalBook
import com.zia.database.bean.NetBook
import com.zia.easybookmodule.net.NetUtil
import com.zia.page.base.BaseViewModel
import com.zia.util.BookUtil
import com.zia.util.ShortcutsUtil
import com.zia.util.defaultSharedPreferences
import com.zia.util.editor
import com.zia.util.threadPool.DefaultExecutorSupplier
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * Created by zia on 2018/11/21.
 */
class BookRackModel : BaseViewModel() {

    val onNetBooksUpdate = MutableLiveData<List<NetBook>>()
    val onLocalBooksUpdate = MutableLiveData<List<LocalBook>>()

    @Synchronized
    fun updateBooks() {
        DefaultExecutorSupplier.getInstance()
            .forBackgroundTasks()
            .submit {
                val netBookDao = AppDatabase.getAppDatabase().netBookDao()
                val netBooks = netBookDao.netBooks
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                val countDownLatch = CountDownLatch(netBooks.size)
                for (netBook in netBooks) {
                    DefaultExecutorSupplier.getInstance()
                        .forBackgroundTasks()
                        .execute {
                            val book = netBook.rawBook
                            val site = book.site
                            try {
                                val html = NetUtil.getHtml(book.url, site.encodeType)
                                val catalogs = site.parseCatalog(html, book.url)
                                val cacheDao = AppDatabase.getAppDatabase().bookCacheDao()
                                val cacheSize = cacheDao.getBookCacheSize(book.bookName, book.siteName)
                                for (i in cacheSize until catalogs.size) {
                                    cacheDao.insert(
                                        BookCache(
                                            book.siteName,
                                            book.bookName,
                                            i,
                                            catalogs[i].chapterName,
                                            catalogs[i].url,
                                            ArrayList()
                                        )
                                    )
                                }
                                if (netBook.currentCheckCount < catalogs.size) {
                                    netBook.currentCheckCount = catalogs.size
                                    netBook.lastChapterName = catalogs[catalogs.size - 1].chapterName
                                    netBook.lastUpdateTime = format.format(Date())
                                    netBookDao.update(netBook)
                                    toast.postValue(netBook.bookName + "有更新")
                                }
                            } catch (e: java.lang.Exception) {
                                Log.d("BookRackModel", e.message)
                            } finally {
                                countDownLatch.countDown()
                            }
                        }
                }
                countDownLatch.await()
                onNetBooksUpdate.postValue(netBooks)
            }
    }

    fun readNetBook(book: NetBook) {
        DefaultExecutorSupplier.getInstance()
            .forLightWeightBackgroundTasks()
            .execute {
                book.time = Date().time
                AppDatabase.getAppDatabase().netBookDao().update(book)
            }
    }

    fun deleteNetBook(book: NetBook) {
        DefaultExecutorSupplier.getInstance()
            .forLightWeightBackgroundTasks()
            .execute {
                AppDatabase.getAppDatabase().netBookDao().delete(book.bookName, book.siteName)
                toast.postValue("删除成功")
                freshNetBooks()
            }
        DefaultExecutorSupplier.getInstance()
            .forBackgroundTasks()
            .execute {
                ShortcutsUtil.removeBook(App.getContext(), book.rawBook)
            }
    }

    fun resetNetBookProgress(book: NetBook) {
        DefaultExecutorSupplier.getInstance()
            .forLightWeightBackgroundTasks()
            .execute {
                defaultSharedPreferences()
                    .editor {
                        putInt(BookUtil.buildId(book.bookName, book.siteName), 0)
                    }
            }
    }

    fun deleteLocalBook(book: LocalBook) {
        DefaultExecutorSupplier.getInstance()
            .forLightWeightBackgroundTasks()
            .execute {
                AppDatabase.getAppDatabase().localBookDao().delete(book.bookName, book.siteName)
                File(book.filePath).delete()
                toast.postValue("删除成功")
            }
    }

    fun freshNetBooks() {
        DefaultExecutorSupplier.getInstance()
            .forLightWeightBackgroundTasks()
            .execute {
                val netBooks = AppDatabase.getAppDatabase().netBookDao().netBooks
                onNetBooksUpdate.postValue(netBooks)
            }
    }

    fun freshLocalBooks() {
        DefaultExecutorSupplier.getInstance()
            .forLightWeightBackgroundTasks()
            .execute {
                val localBooks = AppDatabase.getAppDatabase().localBookDao().localBooks
                onLocalBooksUpdate.postValue(localBooks)
            }
    }
}