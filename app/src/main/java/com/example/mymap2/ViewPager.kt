package com.example.mymap2


import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mymap2.App.Companion.context
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.view_pager.view.*


class ViewPagerAdapter
    ( val mapAct :MapsActivity
    )
    : RecyclerView.Adapter <ViewPagerAdapter.VpvHolder>()  // VpvHolder := ViewPagerViewHolder
{
   // private lateinit var mBotNavBar: BottomNavigationView
    inner class VpvHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VpvHolder {
        val vw = LayoutInflater.from(parent.context).inflate(R.layout.view_pager, parent, false)
        log("onCreateViewHolder")
        vw.bot_nav_bar.setOnNavigationItemSelectedListener { item ->
            mapAct.mSelectedGS?.run{
                when (item.itemId) {
                    R.id.email      -> composeEmail(mapAct)
                    R.id.chat       -> sendSms(mapAct)
                    R.id.subscribe  -> share()
                    R.id.phone      -> makePhonecall(mapAct)
                    R.id.directions -> getDirections(mapAct)
                }
            }
            false
        }
        return VpvHolder(vw)
    }

    override fun getItemCount(): Int {
  //      log("getItemCount  ")
        return App.visibleGSs.size
    }
 //   var mSelGS : GroupSit? = null

    override fun onBindViewHolder(holder: VpvHolder, posn: Int) {
        // holder is actually a ViewGroup layout for a Group Sit
        // posn is the position of the holder in the viewPager array
        log("                    onBindViewHolder $posn")
        val tabLt = holder.itemView.tabLt
        val tabContent = holder.itemView.tabContent

        fun setContent(mode :Int?, content :String?, clr :Drawable? = null){
          //  content?:       throw IllegalStateException("content is null")
          //  mode?: clr?:    throw IllegalStateException("mode and clr are both null")
            tabContent.setText(content)
            tabContent.background = clr ?: modeColour(mode!!)
        }
        var first = true
        fun addTab(mode :Int, tabTitle :String, content :String) {
            val tab = tabLt.newTab().setText(tabTitle)
            val clr = modeColour(mode)
            tab.view.background = clr
            tabLt.addTab(tab)
            if (content == "")
                tab.view.visibility = View.GONE
            else if (first) {
                first = false
                setContent(null, content, clr)
            }
        }
        val gs = App.visibleGSs[posn]
        tabLt.tag = gs
        tabLt.removeAllTabs()
        tabContent.setText("")
        tabContent.background = drawable(android.R.color.white)
        holder.itemView.tvTitle.setText(gs.name)
        holder.itemView.general.setText(gs.general)
        addTab (ONEHOUR,"ONE HOUR", gs.onehour)
        addTab (LONGER ,"LONGER"  , gs.longer )
        addTab (ONEDAY ,"ONE DAY" , gs.oneday )

        val menu = holder.itemView.bot_nav_bar.menu
        if (gs.email == "") menu.findItem(R.id.email).setVisible(false)
        if (gs.phone == "") menu.findItem(R.id.phone).setVisible(false)

        tabLt.addOnTabSelectedListener (object :TabLayout.OnTabSelectedListener {
            override fun onTabSelected (tab: TabLayout.Tab) {
                val gs = tab.parent?.tag as GroupSit //parent is TabLayout
                when (tab.position){
                    ONEHOUR-> setContent (ONEHOUR, gs.onehour)
                    LONGER -> setContent (LONGER , gs.longer )
                    ONEDAY -> setContent (ONEDAY , gs.oneday )
                }
                log("               changed tab for ${gs.name}")
             }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}


fun drawable (clrSlctr: Int) = ContextCompat.getDrawable(context, clrSlctr)
fun colour (clrId: Int)      = ContextCompat.getColor(context, clrId)

fun modeColour(mode: Int) = drawable(
    when(mode){
        ONEHOUR-> R.drawable.onehour_tabcolor_selector
        LONGER -> R.drawable. longer_tabcolor_selector
        ONEDAY -> R.drawable. oneday_tabcolor_selector
        else -> throw IllegalStateException("bad index: $mode")
    })


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun toast(msg: String) {
    val ctx = App.context
    val t = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT)
    t.setGravity(Gravity.BOTTOM or Gravity.END, 50, 350)
    t.view.background.setTint(ContextCompat.getColor(ctx, android.R.color.holo_green_dark))
    val tv: TextView = t.view.findViewById(android.R.id.message)
    val tcolour = ContextCompat.getColor(ctx, android.R.color.background_light)
    tv.setTextColor(tcolour)
    t.show()
}

fun log (s :String) { Log.i("mymap2 xxx", s) }
