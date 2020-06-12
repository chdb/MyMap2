package com.example.mymap2


import android.accounts.AccountManager

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mymap2.App.Companion.context
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.view_pager.*
import kotlinx.android.synthetic.main.view_pager.view.*
import org.intellij.lang.annotations.Pattern
import android.view.inputmethod.InputMethodManager as InputMethodManager_
import com.google.android.material.tabs.TabLayout as TabLayout_


class ViewPagerAdapter
    ( val mMapAct: MapsActivity )
    : RecyclerView.Adapter <ViewPagerAdapter.VpvHolder>()  // VpvHolder := ViewPagerViewHolder
{
   // private lateinit var mBotNavBar: BottomNavigationView
    inner class VpvHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VpvHolder {
        val vw = LayoutInflater.from(parent.context).inflate(R.layout.view_pager, parent, false)
        log("onCreateViewHolder")
        vw.bot_nav_bar.setOnNavigationItemSelectedListener { item ->
            mMapAct.mSelectedGS?.run{
                when (item.itemId) {
                    R.id.email -> composeEmail(mMapAct)
                    R.id.chat -> sendSms(mMapAct)
                    R.id.subscribe -> share()
                    R.id.phone -> makePhonecall(mMapAct)
                    R.id.directions -> getDirections(mMapAct)
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

        fun setContent(mode: Int?, content: String?, clr: Drawable? = null) {
          //  content?:       throw IllegalStateException("content is null")
          //  mode?: clr?:    throw IllegalStateException("mode and clr are both null")
            tabContent.setText(content)
            tabContent.background = clr ?: modeColour(mode!!)
        }
        var first = true
        fun addTab(mode: Int, tabTitle: String, content: String) {
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

        //if (authenticated(gs)) {
        val linLt = tabLt.parent as LinearLayout
        val edit = linLt.findViewById<ImageButton>(R.id.editBtn)
        edit.visibility = visible(authenticated(gs))
       // }

        tabLt.addOnTabSelectedListener (object :TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val gs = tab.parent?.tag as GroupSit //parent is TabLayout
                val mode = tab.position
                setContent(mode, modeContent(mode, gs))
                log("               changed tab for ${gs.name}")
                log("               changed tab for ${gs.name}")
             }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun authenticated (gs :GroupSit) :Boolean {
        // whether there is an email account registered on this device which matches the GroupSit email
        val acMgr = AccountManager.get(mMapAct)
        val accounts = acMgr.getAccountsByType("com.google")
        log("accounts  = ${accounts.size}")
        val acts = acMgr.accounts
        for (ac in acts)
            log("account1: ${ac.name}")
        val acts2 = mMapAct.mPermissionMgr.mAccounts
        for (ac in acts2)
            log("mAccount: ${ac.name}")
        for (ac in accounts) {
            log("account3: ${ac.name}")
            if (gs.email == ac.name)
                return true
        }
        return false
    }
}

fun visible(b: Boolean) = if (b) View.VISIBLE else View.GONE

fun drawable(clrSlctr: Int) = ContextCompat.getDrawable(context, clrSlctr)
fun colour(clrId: Int)      = ContextCompat.getColor(context, clrId)

fun modeColour(mode: Int) = drawable(
    when(mode){
        ONEHOUR-> R.drawable.onehour_tabcolor_selector
        LONGER -> R.drawable.longer_tabcolor_selector
        ONEDAY -> R.drawable.oneday_tabcolor_selector
        else -> throw IllegalStateException("bad index: $mode")
    })

fun modeContent(mode: Int, gs: GroupSit) =
    when(mode){
        ONEHOUR-> gs.onehour
        LONGER -> gs.longer
        ONEDAY -> gs.oneday
        else -> throw IllegalStateException("bad index: $mode")
    }


class GroupSitEditor
    (val mMapAct: MapsActivity)
{

    fun onClickEdit(editBtn: View){
        val linLt = editBtn.parent.parent as LinearLayout
        val relLt = linLt.parent.parent.parent as RelativeLayout
        val doneBtn   = relLt.findViewById<ImageButton>(R.id.done)
        val cancelBtn = relLt.findViewById<ImageButton>(R.id.cancel)
        val edTxt = setEditMode (linLt, editBtn, doneBtn, cancelBtn, true)
        mMapAct.vwPager.tag = linLt
        edTxt.requestFocus()
        edTxt.setSelection(edTxt.text.length)
    }

    fun doneOrCancel(btn: View, bDoneBtn: Boolean) {
        val relLt = btn.parent as RelativeLayout
        val doneBtn   = if ( bDoneBtn) btn else relLt.findViewById<ImageButton>(R.id.done)
        val cancelBtn = if (!bDoneBtn) btn else relLt.findViewById<ImageButton>(R.id.cancel)
        val linLt = mMapAct.vwPager.tag as View
        val editBtn = linLt.findViewById<ImageButton>(R.id.editBtn)
        setEditMode (linLt, editBtn, cancelBtn, doneBtn, false)
    }

    private fun setEditMode(
        linLt: View,
        editBtn: View,
        cancelBtn: View,
        doneBtn: View,
        bEdit: Boolean
    ) :EditText {
        mMapAct.vwPager.isUserInputEnabled = !bEdit
        mMapAct.mMap.uiSettings.setAllGesturesEnabled(!bEdit)
        editBtn  .visibility = visible(!bEdit)
        cancelBtn.visibility = visible (bEdit)
        doneBtn  .visibility = visible (bEdit)
        val edTxt = linLt.findViewById<EditText>(R.id.tvTitle)
        edTxt.isEnabled = bEdit
        linLt.findViewById<EditText>(R.id.tabContent).isEnabled = bEdit
        linLt.findViewById<EditText>(R.id.general)   .isEnabled = bEdit
        linLt.background = if (bEdit) drawable(R.drawable.rounded_corner_2)
        else       drawable(R.drawable.rounded_corner)

        val tabLt = linLt.findViewById(R.id.tabLt) as TabLayout_
        val gs = tabLt.tag as GroupSit
        for (i in tabLt.tabCount-1 downTo 0) {
            val tab = tabLt.getTabAt(i)
            val empty = (modeContent(i, gs) == "" )
            if (empty) {
                tab?.view?.visibility = visible(bEdit) // show/hide empty tabs
                // if selected tab is empty, but now set to GONE, needs to select a non empty tag instead
                if (!bEdit && tabLt.selectedTabPosition == i) {
                    val i2 = if (i > 0) i - 1 else tabLt.tabCount - 1 // i2 = i-1 modulo tabCount
                    tabLt.selectTab(tabLt.getTabAt(i2))
                }
            }
        }
        showKeyboard(bEdit, edTxt)
        return edTxt
    }

    private fun showKeyboard(bShow: Boolean, view: View){
        val imm = mMapAct.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager_
        //imm.toggleSoftInput(InputMethodManager1.SHOW_FORCED, InputMethodManager1.HIDE_IMPLICIT_ONLY)
        if (bShow)
            imm.showSoftInput(view, InputMethodManager_.SHOW_IMPLICIT);
        else
            imm.hideSoftInputFromWindow(view.windowToken, 0);
    }


}

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

fun log(s: String) { Log.i("mymap2 xxx", s) }
