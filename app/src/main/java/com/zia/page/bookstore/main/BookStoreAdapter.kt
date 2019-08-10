package com.zia.page.bookstore.main

import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zia.bookdownloader.R
import com.zia.easybookmodule.bean.rank.HottestRank
import com.zia.easybookmodule.bean.rank.HottestRankClassify
import com.zia.easybookmodule.bean.rank.RankBook
import com.zia.easybookmodule.bean.rank.RankInfo
import com.zia.page.bookstore.detail.BookInfoActivity
import com.zia.util.ColorConstants
import com.zia.util.loadImage
import kotlinx.android.synthetic.main.item_bookstore_classify.view.*
import kotlinx.android.synthetic.main.item_bookstore_first.view.*
import kotlinx.android.synthetic.main.item_bookstore_normal.view.*
import kotlinx.android.synthetic.main.item_bookstore_title.view.*
import java.util.*

/**
 * Created by zia on 2019/4/16.
 */
class BookStoreAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    private val rankList = ArrayList<HottestRankClassify>()
    private val rankInfoList = ArrayList<RankInfo>()
    private val multiList = ArrayList<Any>()
    private var rankSize = 5

    //分类
    private val TYPE_CLASSIFY = 100
    //标题
    private val TYPE_ITEM_TITLE = 101
    //第一个内容
    private val TYPE_ITEM_FIRST = 102
    //普通内容
    private val TYPE_ITEM = 103

    public fun freshRankData(rank: HottestRank?) {
        if (rank == null) return
        freshClassify(rank.rankInfos)
        freshRankList(rank.hottestRankClassifies)
    }

    public fun freshClassify(infoList: List<RankInfo>) {
        if (rankInfoList.size == 0) {
            rankInfoList.addAll(infoList)
            notifyItemChanged(0, getExtraItemCount())
        }
    }

    public fun freshRankList(list: List<HottestRankClassify>) {
        rankList.clear()
        rankList.addAll(list)
        notifyItemRangeChanged(getExtraItemCount(), itemCount)
    }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_CLASSIFY -> {
                if (holder.itemView.item_bookstore_classify_rv.adapter == null) {
                    val classifyAdapter = ClassifyAdapter()
                    holder.itemView.item_bookstore_classify_rv.adapter = classifyAdapter
                    holder.itemView.item_bookstore_classify_rv.layoutManager =
                        androidx.recyclerview.widget.LinearLayoutManager(
                            holder.itemView.context, androidx.recyclerview.widget.RecyclerView.HORIZONTAL, false
                        )
                    classifyAdapter.fresh(rankInfoList)
                }
            }
            TYPE_ITEM_TITLE -> {
                holder.itemView.item_bookstore_title_tv.text = multiList[position] as String
            }
            TYPE_ITEM_FIRST -> {
                val rankBook = multiList[position] as RankBook
                holder.itemView.item_bookstore_first_author.text = rankBook.author
                holder.itemView.item_bookstore_first_bookName.text = rankBook.bookName
                holder.itemView.item_bookstore_first_classify.text = rankBook.classify
                holder.itemView.item_bookstore_first_viewInfo.text = rankBook.viewInfo
                holder.itemView.context.loadImage(rankBook.imgUrl, holder.itemView.item_bookstore_first_cover)
                holder.itemView.setOnClickListener {
                    val intent = Intent(holder.itemView.context, BookInfoActivity::class.java)
                    intent.putExtra("bid", rankBook.data_bid)
                    holder.itemView.context.startActivity(intent)
                }
            }
            else -> {
                val rankBook = multiList[position] as RankBook
                val rankNum = getRankNum(position)
                holder.itemView.item_bookstore_normal_bookName.text = rankBook.bookName
                holder.itemView.item_bookstore_normal_viewInfo.text = rankBook.viewInfo
                holder.itemView.item_bookstore_normal_rankNum.text = rankNum.toString()
                when (rankNum) {
                    2 -> {
                        holder.itemView.item_bookstore_normal_rankNum.setBackgroundColor(ColorConstants.RANK_SECOND)
                    }
                    3 -> {
                        holder.itemView.item_bookstore_normal_rankNum.setBackgroundColor(ColorConstants.RANK_THIRD)
                    }
                    else -> {
                        holder.itemView.item_bookstore_normal_rankNum.setBackgroundColor(ColorConstants.GREY)
                    }
                }
                holder.itemView.setOnClickListener {
                    val intent = Intent(holder.itemView.context, BookInfoActivity::class.java)
                    intent.putExtra("bid", rankBook.data_bid)
                    holder.itemView.context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_CLASSIFY -> {
                val view = LayoutInflater.from(p0.context).inflate(R.layout.item_bookstore_classify, p0, false)
                return ClassifyHolder(view)
            }
            TYPE_ITEM_TITLE -> {
                val view = LayoutInflater.from(p0.context).inflate(R.layout.item_bookstore_title, p0, false)
                return TitleHolder(view)
            }
            TYPE_ITEM_FIRST -> {
                val view = LayoutInflater.from(p0.context).inflate(R.layout.item_bookstore_first, p0, false)
                return FirstHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(p0.context).inflate(R.layout.item_bookstore_normal, p0, false)
                return NormalHolder(view)
            }
        }
    }


    override fun getItemCount(): Int {
        multiList.clear()
        if (getExtraItemCount() != 0) {
            multiList.add("人气榜单")
            multiList.add(rankInfoList)
        }
        rankList.forEachIndexed { _, hottestRankClassify ->
            multiList.add(hottestRankClassify.rankName)
            hottestRankClassify.rankBookList.forEachIndexed { i, rankBook ->
                if (i < rankSize) {
                    multiList.add(rankBook)
                }
            }
        }
        return multiList.size
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_ITEM_TITLE
        }
        if (position == 1) {
            return TYPE_CLASSIFY
        }
        val n = getRankNum(position)
        if (n == 0) {
            return TYPE_ITEM_TITLE
        }
        if (n == 1) {
            return TYPE_ITEM_FIRST
        }
        return TYPE_ITEM
    }

    private fun getRankNum(position: Int): Int {
        return (position - getExtraItemCount()) % (rankSize + 1)
    }

    private fun getExtraItemCount(): Int {
        if (rankInfoList.size == 0) {
            return 0
        }
        return 2
    }

    companion object {
        private class ClassifyHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {}

        private class TitleHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {}

        private class FirstHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {}
        private class NormalHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {}
    }

}