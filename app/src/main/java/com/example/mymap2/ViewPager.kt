package com.example.mymap2


import android.accounts.AccountManager

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mymap2.App.Companion.context
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.view_pager.view.*
import kotlin.math.abs
import android.view.inputmethod.InputMethodManager as InputMethodManager_
import com.google.android.material.tabs.TabLayout as TabLayout_


class ViewPagerAdapter
    ( val mMapAct: MapsActivity )
    : RecyclerView.Adapter <ViewPagerAdapter.VpvHolder>()  // VpvHolder := ViewPagerViewHolder
{
    class VpvHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VpvHolder {
        val vw = LayoutInflater.from(parent.context).inflate(R.layout.view_pager, parent, false)
        log("onCreateViewHolder")
        vw.bot_nav_bar.setOnNavigationItemSelectedListener { item ->
            mMapAct.mSelectedGS?.run{
                when (item.itemId) {
                    R.id.email      -> composeEmail(mMapAct)
                    R.id.chat       -> sendSms(mMapAct)
                    R.id.subscribe  -> share()
                    R.id.phone      -> makePhonecall(mMapAct)
                    R.id.directions -> getDirections(mMapAct)
                }
            }
            false
        }
        return VpvHolder(vw)
    }

    override fun getItemCount(): Int {
        return App.visibleGSs.size
    }

    override fun onBindViewHolder(holder: VpvHolder, posn: Int) {
        // holder is actually a ViewGroup layout for a Group Sit
        // posn is the position of the holder in the viewPager array
        log("                    onBindViewHolder $posn")
        val tabLt      = holder.itemView.tabLt
        val tabContent = holder.itemView.tabContent
        val gs = App.visibleGSs[posn]

//        fun setContent(mode: Int, clr: Drawable? = null) {
//          //  content?:       throw IllegalStateException("content is null")
//          //  mode?: clr?:    throw IllegalStateException("mode and clr are both null")
//            tabContent.setText(gs.text[mode])
//            tabContent.background = clr ?: tabColour(mode)
//        }
        var first = true
        fun addTab(mode: Int, tabTitle: String) {
            val tab = tabLt.newTab().setText(tabTitle)
            val clr = tabColour(mode)
            val text = gs.text[mode]
            tab.view.background = clr
            tabLt.addTab(tab)
            if (text == "")
                tab.view.visibility = View.GONE
            else if (first) {
                first = false
                setContent(tabContent, text, mode, clr)
            }
        }
        tabLt.tag = gs
        tabLt.removeAllTabs()
        tabContent.setText("")
        tabContent.background = drawable(android.R.color.white)
        holder.itemView.tvTitle.setText(gs.name)
        holder.itemView.general.setText(gs.general)
        addTab (ONEHOUR,"ONE HOUR")
        addTab (LONGER ,"LONGER"  )
        addTab (ONEDAY ,"ONE DAY" )

        val menu = holder.itemView.bot_nav_bar.menu
        if (gs.email == "") menu.findItem(R.id.email).setVisible(false)
        if (gs.phone == "") menu.findItem(R.id.phone).setVisible(false)

        val linLt = tabLt.parent as LinearLayout
        val edit = linLt.findViewById<ImageButton>(R.id.editBtn)
        edit.visibility = visible(authenticated(gs))

        tabLt.addOnTabSelectedListener (object :TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val gs = tab.parent?.tag as GroupSit //tab.parent is TabLayout
                val mode = tab.position
                val text = gs.text[mode]
                mMapAct.mGsEditor?.onTabSelected(mode)
                                 ?:setContent(tabContent, text, mode)
             }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun authenticated (gs :GroupSit) :Boolean {
        // whether there is an email account registered on this device which matches the GroupSit email
        //todo this only finds gmail accounts - how to get the others?
        val acMgr = AccountManager.get(mMapAct)
       // val accounts = acMgr.getAccountsByType("com.google")

//        log("accounts  = ${accounts.size}")
//        val acts = acMgr.accounts
//        for (ac in acts)
//            log("account1: ${ac.name}")
//        val acts2 = mMapAct.mPermissionMgr.mAccounts
//        for (ac in acts2)
//            log("mAccount: ${ac.name}")

        for (ac in acMgr.accounts) {
            log("account3: ${ac.name}")
            if (gs.email == ac.name)
                return true
        }
        return false
    }
}

fun setContent (tabContent :EditText, text: String, mode: Int, clr: Drawable? = null) {
    //  content?:       throw IllegalStateException("content is null")
    //  mode?: clr?:    throw IllegalStateException("mode and clr are both null")
    tabContent.setText(text)
    tabContent.background = clr ?: tabColour(mode)
}

fun visible (b: Boolean) = if (b) View.VISIBLE else View.GONE

fun drawable(clrSlctr: Int) = ContextCompat.getDrawable(context, clrSlctr)
fun colour(clrId: Int)      = ContextCompat.getColor(context, clrId)

fun tabColour (mode: Int) = drawable(
    when(mode){
        ONEHOUR-> R.drawable.onehour_tabcolor_selector
        LONGER -> R.drawable.longer_tabcolor_selector
        ONEDAY -> R.drawable.oneday_tabcolor_selector
        else   -> throw IllegalStateException("bad index: $mode")
    })

//fun tabContent (mode: Int, gs: GroupSit) =
//    when(mode){
//        ONEHOUR-> gs.onehour
//        LONGER -> gs.longer
//        ONEDAY -> gs.oneday
//        else   -> throw IllegalStateException("bad index: $mode")
//    }

// Adds margin to the left and right sides of the RecyclerView item.
class HMarginItemDecoration
    (hMarginPx: Int)         // hMarginPx := the required horizontal margin in px.
    : RecyclerView.ItemDecoration()
{   private val mPx = hMarginPx
    override fun getItemOffsets(outRect: Rect, v: View, p: RecyclerView, s: RecyclerView.State) {
        outRect.right = mPx
        outRect.left  = mPx
    }
}

fun initViewPager (mapAct :MapsActivity){
    log("  visibleGS size = ${App.visibleGSs.size}")
    mapAct.vwPager.adapter = ViewPagerAdapter(mapAct)
    mapAct.vwPager.offscreenPageLimit = 1  // render the next and previous items so they can partly visible
    // create PageTransformer that translates the next and previous items horizontally towards the center of the screen, to make them visible
    val itemHMarginPx = mapAct.resources.getDimension(R.dimen.vwpager_item_hmargin)
    val nextVisiblePx = mapAct.resources.getDimension(R.dimen.vwpager_next_visible)
    val pageTranslationX = itemHMarginPx + (nextVisiblePx * 2)
    val pageTransformer = ViewPager2.PageTransformer { page: View, position: Float ->
        page.translationX = -pageTranslationX * position
        page.scaleY = 1 - (0.15f * abs(position)) // reduce height of next and previous items
        //page.alpha = 0.25f + (1 - abs(position))  // for a fading effect
    }
    mapAct.vwPager.setPageTransformer(pageTransformer)
    // ItemDecoration for a horizontal margin on the current (centered) item so that
    // it doesn't occupy the whole screen width. Without it the items overlap
    val itemDecoration = HMarginItemDecoration((itemHMarginPx + nextVisiblePx).toInt())
    mapAct.vwPager.addItemDecoration(itemDecoration)
}


class GroupSitEditor
    ( val mMapAct: MapsActivity
    , val editBtn: View)
{
    private val linLt     = editBtn.parent.parent as LinearLayout
    private val relLt     = linLt.parent.parent.parent as RelativeLayout
    private val tabLt     = linLt.findViewById(R.id.tabLt) as TabLayout_
    private val doneBtn   = relLt.findViewById<ImageButton>(R.id.done)
    private val cancelBtn = relLt.findViewById<ImageButton>(R.id.cancel)
    private val nameTxt   = linLt.findViewById<EditText>(R.id.tvTitle)
    private val contentTxt= linLt.findViewById<EditText>(R.id.tabContent)
    private val generalTxt= linLt.findViewById<EditText>(R.id.general)
    private val mDirtyTxt = arrayOfNulls<String?> (3)
    val gs = tabLt.tag as GroupSit
    private var lastMode = tabLt.selectedTabPosition
    //private var lastText = contentTxt.text.toString()

    init {
        //mMapAct.vwPager.tag = linLt
        nameTxt.requestFocus()
        nameTxt.setSelection(nameTxt.text.length)
        setEditMode (true)
    }

    fun doneOrCancel(btn: View) { //, bDone: Boolean) {
        check (btn == doneBtn || btn == cancelBtn)
        val t = tabLt.selectedTabPosition
        if (btn == doneBtn) {
            gs.name   = nameTxt   .text.toString()
            gs.general= generalTxt.text.toString()
            for (i in 0 until tabLt.tabCount)
                if (i == t)
                    gs.text[i] = contentTxt.text.toString() //save content for selected tab
                else
                    mDirtyTxt[i]?.let { gs.text[i] = it } //save content for other tabs
        } else {
            nameTxt   .setText(gs.name)
            generalTxt.setText(gs.general)
            contentTxt.setText(gs.text[t])
        }
        //todo the others
        setEditMode (false)
        mMapAct.mGsEditor = null
    }

    private fun setEditMode (bEdit:Boolean) {
        mMapAct.vwPager.isUserInputEnabled = !bEdit
        mMapAct.mMap.uiSettings.setAllGesturesEnabled(!bEdit)
        editBtn  .visibility = visible(!bEdit)
        cancelBtn.visibility = visible (bEdit)
        doneBtn  .visibility = visible (bEdit)
        nameTxt   .isEnabled = bEdit
        contentTxt.isEnabled = bEdit
        generalTxt.isEnabled = bEdit
        linLt.background = if (bEdit) drawable(R.drawable.rounded_corner_2)
                           else       drawable(R.drawable.rounded_corner)
        var bSelIsEmpty = false
        var nonemptyTab = -1
        for (i in 0 until tabLt.tabCount) {
            if (gs.text[i] == "") { // content is empty for this tab
                val tab = tabLt.getTabAt(i)
                tab?.view?.visibility = visible(bEdit) // show/hide empty tabs
                if (!bEdit && tabLt.selectedTabPosition == i)
                    bSelIsEmpty = true // selected tab is empty, but now set to GONE
            }
            else if (!bEdit && nonemptyTab == -1)
                nonemptyTab = i  // first nonempty tab
        }
        if (!bEdit && bSelIsEmpty && nonemptyTab >= 0)
            tabLt.selectTab (tabLt.getTabAt (nonemptyTab))
        showKeyboard (mMapAct,  bEdit, nameTxt)
    }

    fun onTabSelected (mode :Int) {
        var lastText = contentTxt.text.toString()
        if (lastText != gs.text[lastMode])
            mDirtyTxt[lastMode] = lastText
        val text = mDirtyTxt[mode] ?: gs.text[mode]
        setContent(contentTxt, text, mode)
        lastMode = mode
        lastText = text
    }
}

fun showKeyboard (act :AppCompatActivity, bShow: Boolean, view: View) {
    val imm = act.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager_
    //imm.toggleSoftInput(InputMethodManager1.SHOW_FORCED, InputMethodManager1.HIDE_IMPLICIT_ONLY)
    if (bShow)
        imm.showSoftInput(view, InputMethodManager_.SHOW_IMPLICIT)
    else
        imm.hideSoftInputFromWindow(view.windowToken, 0)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun toast (msg: String) {
    val ctx = context
    val t = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT)
    t.setGravity(Gravity.BOTTOM or Gravity.END, 50, 350)
    t.view.background.setTint(ContextCompat.getColor(ctx, android.R.color.holo_green_dark))
    val tv: TextView = t.view.findViewById(android.R.id.message)
    val tcolour = ContextCompat.getColor(ctx, android.R.color.background_light)
    tv.setTextColor(tcolour)
    t.show()
}

fun log(s: String) { Log.i("mymap2 xxx", s) }
