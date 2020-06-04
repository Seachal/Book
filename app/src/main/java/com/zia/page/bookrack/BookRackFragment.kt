package com.zia.page.bookrack


import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zia.bookdownloader.R
import com.zia.database.bean.LocalBook
import com.zia.database.bean.NetBook
import com.zia.page.base.BaseFragment
import com.zia.page.preview.PreviewActivity
import com.zia.util.ReaderUtil
import com.zia.util.ToastUtil
import kotlinx.android.synthetic.main.fragment_book_rack.*
import kotlinx.android.synthetic.main.item_book.view.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File


/**
 * Created by zzzia on 2018/11/2.
 * 书架
 */
class BookRackFragment : BaseFragment(), BookRackAdapter.OnBookRackSelect {

    private var bookRackAdapter: BookRackAdapter? = null
    private lateinit var viewModel: BookRackModel

    /**
     * 拉取所有追更书籍最新章节
     * 偷懒没优化，可能发生内存泄漏
     */
    private fun pullBooks() {
        bookRack_sl.post {
            bookRack_sl.isRefreshing = true
            viewModel.updateBooks()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbar.text = "我的书架"

        viewModel = ViewModelProviders.of(this).get(BookRackModel::class.java)
        initObservers()

        bookRackAdapter = BookRackAdapter(bookRack_rv, this)
        bookRack_rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        bookRack_rv.adapter = bookRackAdapter

        bookRack_sl.setOnRefreshListener { pullBooks() }
        bookRack_sl.setColorSchemeColors(resources.getColor(R.color.colorAccent))
    }

    private fun initObservers() {
        viewModel.onNetBooksUpdate.observe(this, Observer {
            if (it != null) {
                bookRackAdapter?.freshNetBooks(it)
                bookRack_sl.isRefreshing = false
            }
        })

        viewModel.onLocalBooksUpdate.observe(this, Observer {
            if (it != null) {
                bookRackAdapter?.freshLocalBooks(it)
            }
        })

        viewModel.toast.observe(this, Observer { ToastUtil.onNormal(it) })
    }

    /**
     * 点击追更书籍
     */
    override fun onNetBookSelected(
        viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        netBook: NetBook,
        position: Int
    ) {
        //更新检查记录
        if (netBook.lastCheckCount < netBook.currentCheckCount) {
            viewHolder.itemView.item_book_lastUpdateTime.background =
                resources.getDrawable(R.drawable.bg_blank)
            viewHolder.itemView.item_book_lastUpdateTime.text = netBook.lastUpdateTime
            netBook.lastCheckCount = netBook.currentCheckCount
        }
        viewModel.readNetBook(netBook)
//        val intent = Intent(context, BookActivity::class.java)
//        intent.putExtra("book", netBook.rawBook)
//        intent.putExtra("canAddFav", false)
//        //动画
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            val p = arrayListOf<Pair<View, String>>(
////                Pair.create(viewHolder.itemView.item_book_layout, "book"),
////                Pair.create(viewHolder.itemView.item_book_name, "book_name"),
////                Pair.create(viewHolder.itemView.item_book_author, "book_author"),
////                Pair.create(viewHolder.itemView.item_book_lastUpdateChapter, "book_lastUpdateChapter"),
////                Pair.create(viewHolder.itemView.item_book_lastUpdateTime, "book_lastUpdateTime"),
////                Pair.create(viewHolder.itemView.item_book_site, "book_site"),
//                Pair.create(viewHolder.itemView.item_book_image, "book_image")
//            )
//            val options =
//                ActivityOptions.makeSceneTransitionAnimation(context as Activity, *Java2Kotlin.getPairs(p))
//            startActivity(intent, options.toBundle())
//        } else {
//            startActivity(intent)
//        }

        val intent = Intent(context, PreviewActivity::class.java)

        intent.putExtra("bookName", netBook.bookName)
        intent.putExtra("siteName", netBook.siteName)

        startActivity(intent)
    }

    /**
     * 调用第三方阅读器打开书籍
     */
    override fun onLocalBookSelected(
        viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        localBook: LocalBook,
        position: Int
    ) {
        val index = localBook.filePath.indexOf("book")
        ToastUtil.onInfo("位置：${localBook.filePath.substring(index)}")
        val file = File(localBook.filePath)
        val intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(Uri.parse(file.path), ReaderUtil.getMIMEType(file))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtil.onError("没有找到阅读器，推荐使用多看/书旗等软件打开")
        }
    }

    /**
     * 长按追更书籍
     */
    override fun onNetBookPressed(
        viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        netBook: NetBook,
        position: Int
    ) {
        if (context == null) return
        AlertDialog.Builder(context!!)
            .setTitle("是否删除${netBook.bookName}")
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                viewModel.deleteNetBook(netBook)
                viewModel.resetNetBookProgress(netBook)
                freshBookRack()
            }
            .setCancelable(true)
            .show()
    }

    /**
     * 长按本地书籍
     */
    override fun onLocalBookPressed(
        viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        localBook: LocalBook,
        position: Int
    ) {
        if (context == null) return
        AlertDialog.Builder(context!!)
            .setTitle("是否删除${localBook.bookName}")
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                viewModel.deleteLocalBook(localBook)
                freshBookRack()
            }
            .setCancelable(true)
            .show()
    }

    private fun freshBookRack() {
        viewModel.freshNetBooks()
        viewModel.freshLocalBooks()
    }

    //第一次加载时刷新
    companion object {
        @Volatile
        private var needRefresh = true
    }

    override fun onResume() {
        super.onResume()
        freshBookRack()
        if (needRefresh) {
            bookRack_rv.post {
                pullBooks()
                needRefresh = false
            }
        } else {
            viewModel.freshNetBooks()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_book_rack, container, false)
    }
}
