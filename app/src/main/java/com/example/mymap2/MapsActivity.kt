package com.example.mymap2

//import android.R

//import com.google.maps.android.clustering.algo

//import android.R
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import kotlinx.android.synthetic.main.activity_maps.*
import kotlin.math.abs
import kotlin.math.cos
import android.view.inputmethod.InputMethodManager as InputMethodManager_
import com.google.android.material.tabs.TabLayout as TabLayout_


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

class MapsActivity
    : AppCompatActivity()
    , OnMapReadyCallback
    , GoogleMap.OnMapLoadedCallback
    //, OnTabSelectedListener
{
    private lateinit var mMap: GoogleMap

//    override fun onUserInteraction() {
//        log("\nOn User interaction\n")
//        super.onUserInteraction()
//    }
//
//    override fun onUserLeaveHint() {
//        log("\nOn User leave hint\n")
//        super.onUserLeaveHint()
//    }


    private fun initViewPager(){
        log("  visibleGS size = ${App.visibleGSs.size}")
        vwPager.adapter = ViewPagerAdapter(this)
        //   vwPager.clipToPadding = false
        //   vwPager.setPadding(48, 0, 48, 0)
        //   vwPager.setPageMargin(24)
        // MyRecyclerViewAdapter is an standard RecyclerView.Adapter :)
        // vwPager.adapter = MyRecyclerViewAdapter()
        vwPager.offscreenPageLimit = 1  // render the next and previous items so they can partly visible
        // create PageTransformer that translates the next and previous items horizontally towards the center of the screen, to make them visible
        val itemHMarginPx = resources.getDimension(R.dimen.vwpager_item_hmargin)
        val nextVisiblePx = resources.getDimension(R.dimen.vwpager_next_visible)
        val pageTranslationX = itemHMarginPx + (nextVisiblePx * 2)
        val pageTransformer = ViewPager2.PageTransformer { page: View, position: Float ->
            page.translationX = -pageTranslationX * position
            page.scaleY = 1 - (0.15f * abs(position)) // reduce height of next and previous items
            //page.alpha = 0.25f + (1 - abs(position))  // for a fading effect
        }
        vwPager.setPageTransformer(pageTransformer)
        // The ItemDecoration gives the current (centered) item horizontal margin so that
        // it doesn't occupy the whole screen width. Without it the items overlap
        val itemDecoration = HMarginItemDecoration((itemHMarginPx + nextVisiblePx).toInt())
        vwPager.addItemDecoration(itemDecoration)

        vwPager.registerOnPageChangeCallback( // pageViewItem SCROLLED
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(idx: Int) {
                    super.onPageSelected(idx)
                    setSelectedGs(idx)
                    log("select pageVw item")
                }
            }
        )
    }

    lateinit var mDefMarkerIcon: BitmapDescriptor
    lateinit var mSelMarkerIcon: BitmapDescriptor
    lateinit var mClusterManager: ClusterManager <GroupSit>

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val smf = supportFragmentManager.findFragmentById(R.id.mapFrag) as SupportMapFragment
        smf.getMapAsync(this)
        App.init(this)
        mDefMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        mSelMarkerIcon = BitmapDescriptorFactory.defaultMarker()
        initViewPager()
        log("onCreate  end")
    }
    // To find unclustered GSs, we put code in onClusterItemRendered() and onClusterRendered()
    private fun setVisibleGSsWhenDone() {
        mCountGSs--
        if (mCountGSs <= 0)
            setVisibleGSs(true)
    }

    override fun onMapLoaded() {
        log("mapLoaded ")
        mMapLoaded = true
        val gs0 = App.allGroupSittings[8] //todo for testing only  -  use current location
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gs0.locn, 10f))

        var lastZoom :Float? = null
        mMap.setOnCameraIdleListener {
            mCountGSs = App.allGroupSittings.size
        log("..")
            log("XXXXXXXXXXXXXXXXXX    cam idle  n = $mCountGSs")
           // startRunner()

            val zoom = mMap.cameraPosition.zoom
            if (lastZoom != zoom) {
                lastZoom = zoom
                log("zoom  - onCameraIdle() calls cluster()")
                mClusterManager.cluster()
            } else {
                log("no zoom - onCameraIdle() calls setSubList()")
                setVisibleGSs(false)
            }
            log("end CameraIdle ")
        }
    }

    fun onClickEdit (editBtn: View){
        val linLt = editBtn.parent.parent as LinearLayout
        val relLt = linLt.parent.parent.parent as RelativeLayout
        val doneBtn   = relLt.findViewById<ImageButton>(R.id.done)
        val cancelBtn = relLt.findViewById<ImageButton>(R.id.cancel)
        val edTxt = setEditMode (linLt, editBtn, doneBtn, cancelBtn, true)
        vwPager.tag = linLt
        edTxt.requestFocus()
        edTxt.setSelection(edTxt.text.length)
    }

    fun onClickEditDone (doneBtn: View){
        //todo: Save changes
        doneOrCancel (doneBtn, true)
    }

    fun onClickEditCancel (cancelBtn: View) {
        doneOrCancel (cancelBtn, false)
    }

    private fun doneOrCancel(btn: View, bDoneBtn :Boolean) {
        val relLt = btn.parent as RelativeLayout
        val doneBtn   = if ( bDoneBtn) btn else relLt.findViewById<ImageButton>(R.id.done)
        val cancelBtn = if (!bDoneBtn) btn else relLt.findViewById<ImageButton>(R.id.cancel)
        val linLt = vwPager.tag as View
        val editBtn = linLt.findViewById<ImageButton>(R.id.editBtn)
        setEditMode (linLt, editBtn, cancelBtn, doneBtn, false)
    }

    private fun setEditMode (linLt: View, editBtn: View, cancelBtn: View, doneBtn: View, bEdit :Boolean) :EditText {
        vwPager.isUserInputEnabled = !bEdit
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
            val empty = (when (i) {
                            ONEHOUR-> gs.onehour
                            LONGER -> gs.longer
                            ONEDAY -> gs.oneday
                            else   -> throw IllegalStateException("bad index: $i")
                        } == "" )
            if (empty) {
                tab?.view?.visibility = visible(bEdit) // show/hide empty tabs
                // if selected tab is empty, but now set to GONE, needs to select a non empty tag instead
                if (!bEdit && tabLt.selectedTabPosition == i) {
                    val i2 = if (i > 0) i - 1 else tabLt.tabCount - 1
                    tabLt.selectTab(tabLt.getTabAt(i2))
                }
            }
        }
        showKeyboard(bEdit, edTxt)
        return edTxt
    }

    private fun showKeyboard (bShow :Boolean, view: View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager_
        //imm.toggleSoftInput(InputMethodManager1.SHOW_FORCED, InputMethodManager1.HIDE_IMPLICIT_ONLY)
        if (bShow)
            imm.showSoftInput(view, InputMethodManager_.SHOW_IMPLICIT);
        else
            imm.hideSoftInputFromWindow(view.windowToken, 0);
    }

    private fun visible (b :Boolean) = if (b) View.VISIBLE else View.GONE

    var mSelectedGS :GroupSit? = null

    private fun setSelectedGs (idx: Int) {
        if (App.visibleGSs.isNotEmpty()){
            val idx2 = if (idx == -1) 0 else idx
            //assert (idx < App.visGroupSittings.size )
            if (mSelectedGS == null)
                vwPager.visibility = View.VISIBLE

            mSelectedGS?.mkr?.setIcon(mDefMarkerIcon)
            mSelectedGS = App.visibleGSs[idx2]
            mSelectedGS?.mkr?.run {
                setIcon(mSelMarkerIcon)
                //mRenderer.getMarker(it)?.
                showInfoWindow()
            }
            //mSelectedGS() = gs
      //      toast("Back!")
        } else {
            assert (idx == -1)
            if (mSelectedGS != null) {
                //it.mkr = null
                mSelectedGS = null
                vwPager.visibility = View.INVISIBLE
           //     toast("Gone!")
            }
        }
    }

    //todo: adjust(true) needs adjusting - top and bottom bounds are both too far north.
    private fun adjustForVwPager (bounds: LatLngBounds, reduce :Boolean) :LatLngBounds {
        val neLat = bounds.northeast.latitude
        val neLng = bounds.northeast.longitude
        val swLat = bounds.southwest.latitude
        val swLng = bounds.southwest.longitude
        val dLat = neLat - swLat              // gods - GreatCircleDegrees
        val vpHt = vwPager.height
        val mapHt = mapFrag.view!!.height
        val margin = vwPager.marginBottom * resources.displayMetrics.density
        val extraHt =  vwPager.height + margin
        val p = dLat / 8 // extra top margin to accommodate the marker
        if (reduce) {
            val q = (dLat - p) * extraHt / mapHt
            return LatLngBounds ( LatLng(swLat + q, swLng)
                                , bounds.northeast )
        }
        val targetHt = mapHt - vpHt - margin
        val avLat = (abs(neLat) + abs(swLat)) / 2
        val bias = cos(avLat)
        val dLng = abs(neLng - swLng) * bias // gods
        val targetWd = mapFrag.view!!.width //- (padding * 2)
        val dLat2 = dLng * targetHt / targetWd
        if (dLat > dLat2) {
            val r = dLat * extraHt / targetHt + p
            return LatLngBounds( LatLng(swLat - r, swLng)
                               , LatLng(neLat + p, neLng) )
        }
        val s = (dLat2 - dLat) / 2
        val t = (dLat + 2*s) * extraHt / targetHt + s
        return LatLngBounds( LatLng(swLat - t - p, swLng)
                           , LatLng(neLat + s + p, neLng))
    }

    // Manipulates the map once available. If Google Play services is not installed on the device, the user will be prompted to install it
    // inside the SupportMapFragment. This method will then be triggered once the user has installed Google Play services and returned to the app.
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLoadedCallback(this)
  //      googleMap.uiSettings.isZoomControlsEnabled = true
        mClusterManager = ClusterManager<GroupSit>(this, googleMap)
        mClusterManager.addItems(App.allGroupSittings) // 4
        log("onMapReady")
        //todo: under the default algorithm, cluster() calculates the clustering for the entire world, at current zoom level.
        // For a larger data set we will probably need a more focused algorithm
        // eg "NonHierarchicalVIEWBasedAlgorithm" which is similar to the default "NonHierarchicalDISTANCEBasedAlgorithm"
        // but "works only on the visible area"; therefore cluster() should be much faster but would need to be called on a pan as well as on a zoom.
        // Android Studio would need to include the package dependency etc.
        // https://github.com/googlemaps/android-maps-utils/tree/master/library/src/main/java/com/google/maps/android/clustering/algo
        //             mClusterManager.algorithm = NonHierarchicalViewBasedAlgorithm (width, height)
        // Alternatively - other cluster libraries ???

        mMap.setOnMarkerClickListener(mClusterManager)

        mClusterManager.setOnClusterClickListener {
            log("cluster clicked")
            val builder = LatLngBounds.builder()
            for (item in it.items) { // each item in the clicked cluster
                builder.include(item.position)
            }
            val bounds = adjustForVwPager(builder.build(), false)
            val padding = resources.displayMetrics.widthPixels / 6
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            true
        }
        mClusterManager.setOnClusterItemClickListener { // NB this means a free GS was clicked - NOT a cluster!
            log("free item clicked")
            val idx = App.visibleGSs.indexOf(it)

            vwPager.currentItem = idx //it.idx
            val lastMarker = mRenderer.getMarker(mSelectedGS)
            log ("selected marker = $lastMarker")
            setSelectedGs(idx) //(it.idx)
            //mRenderer.getMarker(it)?.showInfoWindow()
            true
        }
        mRenderer = CustomClusterRenderer(this, mMap, mClusterManager)
        mRenderer.setAnimation(true)
        mClusterManager.renderer = mRenderer
//        mClusterManager.renderer = CustomClusterRenderer(this, mMap, mClusterManager)
    }
    lateinit var mRenderer :CustomClusterRenderer
    var mMapLoaded = false
    var mCountGSs =  App.allGroupSittings.size
    val mMinClusterSize = 3
    private var mFreeGSs = listOf<GroupSit>() // Free Groups Sits, sorted by longitude, latitude
    // "Free" is our term for all items "not rendered as clusters" (in the terms of the library)
    // The Clustering algorithm sorts all GSs into what it confusingly calls "CLUSTERS", even though
    // the job is only to put some "close enough" items into clusters. (Not all items are "RENDERED AS clusters")
    // Renderer.shouldRenderAsCluster() decides which of these "Cluster"s actually remain as "free" markers.
    // Also confusingly, all items are called "Cluster Items", whether or not they are (rendered as) clustered


    private fun setVisibleGSs (newClusters :Boolean) { //(clusters: MutableSet<out Cluster<GroupSit>>? = null) {
        val newBounds = adjustForVwPager(mMap.projection.visibleRegion.latLngBounds, true)
        log("bounds = $newBounds")
        if (newClusters) {
            mFreeGSs = App.allGroupSittings  //todo for large dataset, replace this global filter with mFreeGSs.sorted() and put mFreeGSs.add() in onClusterItemRendered
                        .filter { it.mkr != null }
                        .sorted()

//            val mkrGSs = mClusterManager.markerCollection.markers
//                    .map {it.tag as GroupSit}
//                    .sorted()
//            val missing = mFreeGSs - mkrGSs
//            val extra = mkrGSs - mFreeGSs
//            missing.forEach { log(" missing gs = ${it.name}") }
//            extra  .forEach { log(" extra gs = ${it.name}") }
        }
        val newVisibles = mFreeGSs.filter { newBounds.contains(it.locn) }

        log("newSubList = ${newVisibles.size}")
        log("vwPager.adapter?.itemCount = ${vwPager.adapter?.itemCount}")
        log("sel gs =  ${mSelectedGS?.name}")
        newVisibles.forEachIndexed { n, gs -> log("newSubList $n  ${gs.name}") }

        val newSelGSIdx = newVisibles.indexOf(mSelectedGS)
        log("new idx =  $newSelGSIdx")
        updateVwPager(newVisibles)

        if (newSelGSIdx > 0)
            vwPager.currentItem = newSelGSIdx

        setSelectedGs(newSelGSIdx)
    }

    private fun updateVwPager (newVisibles: List<GroupSit>) {
        val oldVisibles = App.visibleGSs
        App.visibleGSs = newVisibles

        val news = newVisibles.iterator()
        val olds = oldVisibles.iterator()
        var newEnd = !news.hasNext()
        var oldEnd = !olds.hasNext()
        if (newEnd || oldEnd) {
            vwPager.adapter?.notifyDataSetChanged()
            log("                                       data set changed")
        } else {
            var new = news.next()
            var old = olds.next()
            var p = 0 // position
            fun nextNew() {
                if (news.hasNext())
                    new = news.next()
                else
                    newEnd = true
            }
            fun nextOld() {
                if (olds.hasNext())
                    old = olds.next()
                else
                    oldEnd = true
            }
            do {if (new == old) {
                    nextOld()
                    nextNew()
                    log("                                       (keep  1 item at posn $p)")
                    p++
                } else {
                    if (!oldEnd) {
                        var r = 0 // number of items to REMOVE
                        while (newEnd || (old < new)) {
                            r++
                            nextOld()
                            if (oldEnd) break
                        }
                        if (r > 0) {
                            log("                                       removed $r items starting from posn $p")
                            vwPager.adapter?.notifyItemRangeRemoved(p, r)
                            continue
                    }   }
                    if (!newEnd) {
                        var a = 0 // number of items to ADD
                        while (oldEnd || (new < old)) {
                            a++
                            nextNew()
                            if (newEnd) break
                        }
                        if (a > 0) {
                            log("                                       inserted $a items starting from posn $p")
                            vwPager.adapter?.notifyItemRangeInserted(p, a)
                            p += a
                }   }   }
            } while (!(newEnd && oldEnd))
        }
    }

    inner class CustomClusterRenderer( context       :MapsActivity
                                     , map           :GoogleMap?
                                     , clusterManager:ClusterManager<GroupSit>
    ) : DefaultClusterRenderer<GroupSit> (context, map, clusterManager) {

        override fun shouldRenderAsCluster(cluster: Cluster<GroupSit>?): Boolean =
            cluster?.run{ size >= mMinClusterSize } ?: false

        override fun onBeforeClusterItemRendered(gs: GroupSit?, mkrOpts: MarkerOptions?) {
            gs?.let{ mkrOpts?.icon (if (it == mSelectedGS)
                                         mSelMarkerIcon
                                    else mDefMarkerIcon
                ) }
            super.onBeforeClusterItemRendered (gs, mkrOpts)
        }
        override fun onClusterItemRendered(gs: GroupSit?, marker: Marker?) {
            //These items are NOT in a cluster
            super.onClusterItemRendered(gs, marker)
            if (gs != null) {
                check (marker != null)
                gs.mkr = marker
                marker.tag = gs
                if (gs == mSelectedGS) {
                    marker.showInfoWindow()
                }
                setVisibleGSsWhenDone()
                //  log("CCR item rendered: ${gs.id}")
//                mAct.mFreeGSs.add(gs)
            }
        }
        override fun onClusterRendered(cluster: Cluster<GroupSit>?, marker: Marker?) {
            //These items ARE in a cluster
            super.onClusterRendered(cluster, marker)
            if (cluster != null) {
                for (gs in cluster.items) {
                //    log("CCR cluster rendered for gs: ${gs.id}")
                    gs.mkr = null
                    setVisibleGSsWhenDone()
                }
            }
        }
    }

//    private fun updateVisibles2 (newVisibles: List<GroupSit>) {
//
//        val oldVisibles = App.visibleGSs
//        App.visibleGSs = newVisibles.toMutableList()
//
//        //remove items in App.visibleGSs that are not in newVisibles
//        val news = newVisibles.iterator()
//        val olds = oldVisibles.iterator()
//        var newEnd = false
//        var oldEnd = false
//        var new = news.next()
//        var old = olds.next()
//        var p = 0 // position
//        //var same = false
//       // var removing = false
//        var r = 0 // number of items to REMOVE
//        var a = 0 // number of items to ADD
//
//        var currState = 'X'
//        var lastState = 'X'
//
//        fun nextNew() {
//            if (news.hasNext())
//                new = news.next()
//            else
//                newEnd = true
//        }
//        fun nextOld() {
//            if (olds.hasNext())
//                old = olds.next()
//            else
//                oldEnd = true
//        }
//
//        do {
//            if (new == old) {
//                currState = 'K'
//                nextOld()
//                nextNew()
//            } else {
//                if (old < new) {
//                    currState = 'R'
//                    r++
//                    nextOld()
//                    if (oldEnd)
//                        nextNew()
//                }
//                else{
//                    check (new < old)
//                    currState = 'A'
//                    a++
//                    nextNew()
//                    if (newEnd)
//                        nextOld()
//                }
//            }
//            val exit = newEnd && oldEnd
//            if (exit || (currState != lastState))
//                if (currState != 'R' && r > 0) {
//                    log("removed $r items starting from posn $p")
//                    vwPager.adapter?.notifyItemRangeRemoved(p, r)
//                    r = 0
//                }
//                if (currState != 'A' && a > 0) {
//                    log("inserted $a items starting from posn $p")
//                    vwPager.adapter?.notifyItemRangeInserted(p, a)
//                    p += a
//                    a = 0
//                }
//            if (currState == 'K')
//                p++
//            lastState = currState
//        } while (! exit)
//    }
//
//    private fun updateSubList(newSubList: List<GroupSit>, bounds: LatLngBounds){
//
//        val toAdd = newSubList - App.visibleGSs
//        val toRem = App.visibleGSs - newSubList
//
//        val oldWest = mLastBounds!!.southwest.longitude
//        val oldEast = mLastBounds!!.northeast.longitude
////            val oldNorth = mLastBounds!!.northeast.latitude
////            val oldSouth = mLastBounds!!.southwest.latitude
//        val newWest = bounds.southwest.longitude
//        val newEast = bounds.northeast.longitude
////            val newNorth = bounds.northeast.latitude
////            val newSouth = bounds.southwest.latitude
//        val lessWest = isEastOf(newWest, oldWest)
//        val lessEast = isWestOf(newEast, oldEast)
////            val goWest = isWestOf(newWest, oldWest)
////            val goEast = isEastOf(newEast, oldEast)
////            val goNorth = newNorth > oldNorth
////            val goSouth = newSouth < oldSouth
//        val oldWestmost = if(App.visibleGSs.isEmpty()) oldWest else App.visibleGSs.first().locn.longitude
//        val oldEastmost = if(App.visibleGSs.isEmpty()) oldEast else App.visibleGSs.last ().locn.longitude
//        val newWestmost = if(newSubList    .isEmpty()) newWest else newSubList    .first().locn.longitude
//        val newEastmost = if(newSubList    .isEmpty()) newWest else newSubList    .last ().locn.longitude
//
//        //midR: middle items to be removed from gsSubList
//        var midR = listOf<GroupSit>()
//        if (lessEast && lessWest) {
//            midR = toRem.filter {
//                !isWestOf(it.locn.longitude, newWestmost)
//                        && !isEastOf(it.locn.longitude, newEastmost)
//            }
//        } else if (lessEast) {
//            assert(!lessWest)
//            midR = toRem.filter { !isEastOf(it.locn.longitude, newEastmost) }
//
//        } else if (lessWest) {
//            assert(!lessEast)
//            midR = toRem.filter { !isWestOf(it.locn.longitude, newWestmost) }
//        }
//        if (midR.isNotEmpty()) { // now remove them one by one
//            var n = 0
//            midR.forEach {
//                val p = it.idx - n++
//                it.idx = -1
//                App.visibleGSs.removeAt(p)
//                vwPager.adapter?.notifyItemRemoved(p)
//            }
//        }
//        App.visibleGSs.forEach { it.idx = -1 }
//
//        //remove East items from end of gsSubList
//        if (lessEast) {
//            val sz1 = App.visibleGSs.size
//            App.visibleGSs.removeAll { isEastOf(it.locn.longitude, newEastmost) }
//            val sz2 = App.visibleGSs.size
//            if (sz1 != sz2)
//                vwPager.adapter?.notifyItemRangeRemoved(sz2, sz1 - sz2)
//            log("east items removed = ${sz1 - sz2}  count = ${vwPager.adapter?.itemCount}")
//        }
//        //remove West items from start of gsSubList
//        if (lessWest) {
//            val sz1 = App.visibleGSs.size
//            App.visibleGSs.removeAll { isWestOf(it.locn.longitude, newWestmost) }
//            val sz2 = App.visibleGSs.size
//            if (sz1 != sz2)
//                vwPager.adapter?.notifyItemRangeRemoved(0, sz1 - sz2)
//            log("west items removed = ${sz1 - sz2}  count = ${vwPager.adapter?.itemCount}")
//        }
//
//        //midA: middle items to be added to gsSubList
//        var midA = listOf<GroupSit>()
//        if (!lessEast && !lessWest) {
//            midA = toAdd.filter {
//                !isWestOf(it.locn.longitude, oldWestmost)
//                        && !isEastOf(it.locn.longitude, oldEastmost)
//            }
//            log("1. mid items to add = ${midA.size}")
//        } else if (lessEast) {
//            assert(!lessWest)
//            midA = toAdd.filter { !isEastOf(it.locn.longitude, oldWestmost) }
//            log("2. mid items to add = ${midA.size}")
//        } else if (lessWest) {
//            assert(!lessEast)
//            midA = toAdd.filter { !isWestOf(it.locn.longitude, oldEastmost) }
//            log("3. mid items to add = ${midA.size}")
//        }
//        if (midA.isNotEmpty()) { // now add them one by one
//            //log("mid items adding = ${midA.size}")
//            var n = 0
//            val sz = App.visibleGSs.size
//            midA.forEach {
//                while (n < sz &&
//                    isEastOf(it.locn.longitude, App.visibleGSs[n].locn.longitude) )
//                    n++
//                App.visibleGSs.add(n, it)
//                vwPager.adapter?.notifyItemInserted(n)
//                n++
//            }
//        }
//        //add West items at end of gsSubList
//        if (!lessWest) {
//            val w = toAdd.filter { isWestOf(it.locn.longitude, oldWestmost) }
//            val wsz = w.size
//            log("west items to add = $wsz")
//            if (wsz > 0) {
//                val p = App.visibleGSs.size - 1
//                App.visibleGSs.addAll(p, w)
//                vwPager.adapter?.notifyItemRangeInserted(p, wsz)
//            }
//        }
//        //add East items at start of gsSubList
//        if (!lessEast) {
//            val e = toAdd.filter { isEastOf(it.locn.longitude, oldEastmost) }
//            val esz = e.size
//            log("east items to add = $esz")
//            if (esz > 0) {
//                App.visibleGSs.addAll(0, e)
//                vwPager.adapter?.notifyItemRangeInserted(0, esz)
//            }
//        }
//    }
//    private fun updateSubList2(newSubList: List<GroupSit>){
//
//        val toAdd = newSubList - App.visibleGSs
//        val toRem = App.visibleGSs - newSubList
//
////        val oldWest = mLastBounds!!.southwest.longitude
////        val oldEast = mLastBounds!!.northeast.longitude
////            val oldNorth = mLastBounds!!.northeast.latitude
////            val oldSouth = mLastBounds!!.southwest.latitude
////        val newWest = bounds.southwest.longitude
////        val newEast = bounds.northeast.longitude
////            val newNorth = bounds.northeast.latitude
////            val newSouth = bounds.southwest.latitude
////        val lessWest = isEastOf(newWest, oldWest)
////        val lessEast = isWestOf(newEast, oldEast)
////            val goWest = isWestOf(newWest, oldWest)
////            val goEast = isEastOf(newEast, oldEast)
////            val goNorth = newNorth > oldNorth
////            val goSouth = newSouth < oldSouth
//        val oldFirst = App.visibleGSs.first().locn.longitude
//        val oldLast  = App.visibleGSs.last ().locn.longitude
//        val newFirst = newSubList   .first().locn.longitude
//        val newLast  = newSubList   .last ().locn.longitude
//
//        //midR: middle items to be removed from gsSubList
//        val lessWest = isWestOf (oldFirst, newFirst)
//        val lessEast = isEastOf (oldFirst, newFirst)
//
////        if (lessEast && lessWest) {
////
////        }
//
//
//        var midR = listOf<GroupSit>()
//        if (lessEast && lessWest) {
//            midR = toRem.filter {
//                !isWestOf(it.locn.longitude, newFirst)
//                        && !isEastOf(it.locn.longitude, newLast)
//            }
//        } else if (lessEast) {
//            assert(!lessWest)
//            midR = toRem.filter { !isEastOf(it.locn.longitude, newLast) }
//
//        } else if (lessWest) {
//            assert(!lessEast)
//            midR = toRem.filter { !isWestOf(it.locn.longitude, newFirst) }
//        }
//        if (midR.isNotEmpty()) { // now remove them one by one
//            var n = 0
//            midR.forEach {
//                val p = it.idx - n++
//                it.idx = -1
//                App.visibleGSs.removeAt(p)
//                vwPager.adapter?.notifyItemRemoved(p)
//            }
//        }
//        App.visibleGSs.forEach { it.idx = -1 }
//
//        //remove East items from end of gsSubList
//        if (lessEast) {
//            val sz1 = App.visibleGSs.size
//            App.visibleGSs.removeAll { isEastOf(it.locn.longitude, newLast) }
//            val sz2 = App.visibleGSs.size
//            if (sz1 != sz2)
//                vwPager.adapter?.notifyItemRangeRemoved(sz2, sz1 - sz2)
//            log("east items removed = ${sz1 - sz2}  count = ${vwPager.adapter?.itemCount}")
//        }
//        //remove West items from start of gsSubList
//        if (lessWest) {
//            val sz1 = App.visibleGSs.size
//            App.visibleGSs.removeAll { isWestOf(it.locn.longitude, newFirst) }
//            val sz2 = App.visibleGSs.size
//            if (sz1 != sz2)
//                vwPager.adapter?.notifyItemRangeRemoved(0, sz1 - sz2)
//            log("west items removed = ${sz1 - sz2}  count = ${vwPager.adapter?.itemCount}")
//        }
//
//        //midA: middle items to be added to gsSubList
//        var midA = listOf<GroupSit>()
//        if (!lessEast && !lessWest) {
//            midA = toAdd.filter {
//                !isWestOf(it.locn.longitude, oldFirst)
//                        && !isEastOf(it.locn.longitude, oldLast)
//            }
//            log("1. mid items to add = ${midA.size}")
//        } else if (lessEast) {
//            assert(!lessWest)
//            midA = toAdd.filter { !isEastOf(it.locn.longitude, oldFirst) }
//            log("2. mid items to add = ${midA.size}")
//        } else if (lessWest) {
//            assert(!lessEast)
//            midA = toAdd.filter { !isWestOf(it.locn.longitude, oldLast) }
//            log("3. mid items to add = ${midA.size}")
//        }
//        if (midA.isNotEmpty()) { // now add them one by one
//            //log("mid items adding = ${midA.size}")
//            var n = 0
//            val sz = App.visibleGSs.size
//            midA.forEach {
//                while (n < sz &&
//                    isEastOf(it.locn.longitude, App.visibleGSs[n].locn.longitude) )
//                    n++
//                App.visibleGSs.add(n, it)
//                vwPager.adapter?.notifyItemInserted(n)
//                n++
//            }
//        }
//        //add West items at end of gsSubList
//        if (!lessWest) {
//            val w = toAdd.filter { isWestOf(it.locn.longitude, oldFirst) }
//            val wsz = w.size
//            log("west items to add = $wsz")
//            if (wsz > 0) {
//                val p = App.visibleGSs.size - 1
//                App.visibleGSs.addAll(p, w)
//                vwPager.adapter?.notifyItemRangeInserted(p, wsz)
//            }
//        }
//        //add East items at start of gsSubList
//        if (!lessEast) {
//            val e = toAdd.filter { isEastOf(it.locn.longitude, oldLast) }
//            val esz = e.size
//            log("east items to add = $esz")
//            if (esz > 0) {
//                App.visibleGSs.addAll(0, e)
//                vwPager.adapter?.notifyItemRangeInserted(0, esz)
//            }
//        }
//    }
}

//We need these, for any view that includes longitude 180°
//NB we dont need to cover isNorthOf and IsSouthOf - just use latA > latB
fun isWestOf(a: Double, b: Double) :Boolean {
    val d = b - a
    return d < 180.0 && d > 0.0
}
fun isEastOf(a: Double, b: Double) :Boolean {
    val d = a - b
    return d < 180.0 && d > 0.0
}
//const val WEST  = 0
//const val NORTH = 1
//const val SOUTH = 2
//const val EAST  = 3
//

    //override fun on

//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    fun toast(msg: String) {
//        val t = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
//        t.setGravity(Gravity.BOTTOM or Gravity.END, 50, 350)
//        t.view.background.setTint(ContextCompat.getColor(applicationContext, android.R.color.holo_green_dark))
//        val tv: TextView = t.view.findViewById(android.R.id.message)
//        val tcolour = ContextCompat.getColor(applicationContext, android.R.color.background_light)
//        tv.setTextColor(tcolour)
//        t.show()


//    private fun createTabContent(tabs :TabLayout, tabContent :TextView) {
//        //val tabs = findViewById<TabLayout>(R.id.tabs)
//        //val tabContent = findViewById<TextView>(R.id.tabContent)
//        tabs?.let {
//            toast("tabs exists")
//            it.getTabAt(0)?.let{
//                toast("tab 0 exisits !!!!!!!")
//                //it.text = "OK tab 0 !!! XXXXXXXXXXXXXXXXXXXX"
//
//            }
//        }
//        tabContent?.text = "OK XXXXXXXXXXXX QQQ"
//        //tabs.addOnTabSelectedListener()
//        tabs?.addOnTabSelectedListener(object : OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) {
//                val n = tab.position
//                when (n) {
//                    0 -> tabContent?.text = "OK XXXXXXXXXXXX 0 xxxxxxxxxx"
//                    1 -> tabContent?.text = "OK XXXXXXXXXXXX 1 xxxxxxxxxx"
//                    2 -> tabContent?.text = "OK XXXXXXXXXXXX 2 xxxxxxxxxx"
//                }
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab) {}
//            override fun onTabReselected(tab: TabLayout.Tab) {}
//        })
//    }
//}

//    private fun setSubList (clusters: MutableSet<out Cluster<GroupSit>>? = null) {
//        val newBounds = mMap.projection.visibleRegion.latLngBounds
//        log("bounds = $newBounds")
//
//        if (clusters != null) {
//            // clusters has changed, so reset mFreeGSs
//            mFreeGSs = clusters.filter { it.size < mMinClusterSize }
//                                .flatMap { it.items }
//                                .sorted()
//        }
//        val newSubList = mFreeGSs.filter { newBounds.contains(it.locn) }
//        log("newSubList =  ${newSubList.size}")
//        log("vwPager.adapter?.itemCount = ${vwPager.adapter?.itemCount}")
//
//
//        if (vwPager.adapter == null) {
//            App.visGroupSittings = newSubList.toMutableList()
//            initViewPager()
//            log("initViewPager()")
//            log("vwPager.adapter?.itemCount = ${vwPager.adapter?.itemCount}")
//        }else {
//            val selGSgon
//
//        //val inter = newSubList intersect App.gsSubList
//        //if (inter.isEmpty()){//todo a version of updateSubList() might be needed for performance
//            App.visGroupSittings.clear()
//            App.visGroupSittings.addAll( newSubList)
//            vwPager.adapter?.notifyDataSetChanged()
//        }//}else{
////            assert (mLastBounds != null)
////            updateSubList(newSubList, newBounds)
////        }
//        initSubListIdx()
//        mLastBounds = newBounds
//
////        val sz = App.gsSubList.size
////        if (sz > 0)
//        if ()
//
//
//            setSelectedGs(0)
////        else
////            mSelectedGS() = null
//    }
/////////////////////////////////////////////////////////////////////////////
//
//        if (goWest) {
//                val (w, r1) = toAdd.partition { isWestOf(it.locn.longitude, oldWestmost) }
//                west = w
//                if (goEast) {                   //both: goWest && goEast
//                    val (e, r2) = r1.partition { isEastOf(it.locn.longitude, oldEastmost) }
//                    east = e //add
//                    mid = r2 //add
//                }else{                          //goWest && !goEast
//                    val sz1 = App.gsSubList.size
//                    App.gsSubList.removeAll { isEastOf(it.locn.longitude, newEastmost) }
//                    val sz2 = App.gsSubList.size
//                    if (sz1 != sz2)
//                        vwPager.adapter?.notifyItemRangeRemoved( sz2, sz1 - sz2)
//                    val midR = toRem.filter{ !isEastOf(it.locn.longitude, newEastmost) }
//                    if (midR.isNotEmpty()) {
//                        midR.forEach { it ->
//                            App.gsSubList.removeAt(it.vwIn)
//                            vwPager.adapter?.notifyItemRemoved(it.vwIn)
//                        }
//                    }
//                }
//            }
//            else {
//                // var (e, r1) = toAdd.partition {  isEastOf(it.locn.longitude, oldEast) }
//                if (goEast) {                   // !goWest && goEast
////                    val (e, r) = toAdd.partition { isEastOf(it.locn.longitude, oldEast) }
////                    eAdd = e
////                    nsAdd = r
//                    val sz1 = App.gsSubList.size
//                    App.gsSubList.removeAll { isWestOf(it.locn.longitude, newWestmost) }
//                    val sz2 = App.gsSubList.size
//                    if (sz1 != sz2)
//                        vwPager.adapter?.notifyItemRangeRemoved( sz2, sz1 - sz2)
//                    val midR = toRem.filter{ !isWestOf(it.locn.longitude, newWestmost) }
//                    if (midR.isNotEmpty()) {
//                        midR.forEach { it ->
//                            App.gsSubList.removeAt(it.vwIn)
//                            vwPager.adapter?.notifyItemRemoved(it.vwIn)
//                        }
//                    }
//                }else{                          //neither:  !goWest && !goEast
//                    val (w, r2) = toRem.partition { isWestOf(it.locn.longitude, newWest) }
//                    val (e, r3) = r2.partition { isEastOf(it.locn.longitude, newEast) }
//                }
//            }
//        }
//
//
//
////            val (west, rest) =
////                if (goWest) toAdd.partition { isWestOf(it.locn.longitude, oldWest) }
////                else  Pair(listOf<GroupSit>(), toAdd)
////
////            val (east, rest2) =
////                if (goWest && goEast) rest.partition { isEastOf(it.locn.longitude, oldEast) }
////                else  if (goWest)    toAdd.partition { isEastOf(it.locn.longitude, oldEast) }
////                else Pair(listOf<GroupSit>(), toAdd)
//
//            var eAdd :List<GroupSit>? = null
//            var nAdd :List<GroupSit>? = null
//            var sAdd :List<GroupSit>? = null
//            var wAdd :List<GroupSit>? = null
//            var eRem :List<GroupSit>? = null
//            var nRem :List<GroupSit>? = null
//            var sRem :List<GroupSit>? = null
//            var wRem :List<GroupSit>? = null
//            var nsAdd:List<GroupSit>? = null
//            var nsRem:List<GroupSit>? = null
//
//            fun part(): Triple<List<GroupSit>, List<GroupSit>, List<GroupSit>> {
//                if (goWest){
//                    val (w, r1) = toAdd.partition { isWestOf(it.locn.longitude, oldWest) }
//                    wAdd = w
//                    if (goEast) { //both: goWest && goEast
//                        val (e, r2) = r1.partition { isEastOf(it.locn.longitude, oldEast) }
//                        eAdd = e
//                        nsAdd = r2
//                    }else{  //goWest && !goEast
//                        val (e, r3) = toRem.partition { isEastOf(it.locn.longitude, newEast) }
//                        eRem = e
//                        nsRem = r3
//                    }
//                }
//                else {
//                   // var (e, r1) = toAdd.partition {  isEastOf(it.locn.longitude, oldEast) }
//                    if (goEast) { // !goWest && goEast
//                        val (e, r) = toAdd.partition { isEastOf(it.locn.longitude, oldEast) }
//                        eAdd = e
//                        nsAdd = r
//                    }else{ //neither:  !goWest && !goEast
//                        val (w, r2) = toRem.partition { isWestOf(it.locn.longitude, newWest) }
//                        val (e, r3) = r2.partition { isEastOf(it.locn.longitude, newEast) }
//                    }
//                }
//            }
//
//
//            if (goEast)
//            {   val a = if (goWest) rest  else toAdd
//                val (inEast, r) = a.partition {  }
//                rest = r
//
//            }
//
//            if (goNorth) {
//                val toAddNorth = rest.filter { it.locn.latitude > oldNorth } }
//                App.gsSubList.forEach {  }
//            val (n, s) = ns.partition { it.locn.latitude > oldNorth }
//            }else{
//
//            }
//
//            if (!goWest){
//                val sz1 = App.gsSubList.size
//                App.gsSubList.removeAll { isWestOf(it.locn.longitude, oldWest) }
//                val sz2 = App.gsSubList.size
//                if (sz1 > sz2)
//                    vwPager.adapter?.notifyItemRangeRemoved( 0, sz1 - sz2)
//            }
//            if (!goEast) {
//                val sz1 = App.gsSubList.size
//                App.gsSubList.removeAll { isEastOf(it.locn.longitude, oldEast) }
//                val sz2 = App.gsSubList.size
//                if (sz1 > sz2)
//                    vwPager.adapter?.notifyItemRangeRemoved( sz2, sz1 - sz2)
//            }
//            if (goWest)
//            {   val n = inWest.size
//                if (n > 0) {
//                    App.gsSubList.addAll(0, inWest)
//                    vwPager.adapter?.notifyItemRangeInserted(0, n)
//                }
//            }
//            if (goEast)
//            {   val n = inEast.size
//                if (n > 0) {
//                    val sz = App.gsSubList.size
//                    App.gsSubList.addAll(0, inEast)
//                    vwPager.adapter?.notifyItemRangeInserted( sz, n)
//                }
//            }
//
//        }
//
//

//        //App.gsSubList = newSubList
//
//        var n = 0
//        App.gsSubList.forEach { it.vwIn = n++
//        log("subList:  ${it.gsId}   ${it.name}")}



//val sz = .size
//
//        App.gsSubList.clear()
//        //vwPager.adapter?.notifyItemRangeRemoved(0, sz)
//
//        for (m in mClusterManager.markerCollection.markers) {
////          for (gs in App.gsList){
//            val gs = m.tag as GroupSit?
//            if (gs != null)
//                if (bounds.contains(gs.locn)) {
//                    log("adding : ${gs.gsId}")
//                    //assert(gs.vwIn == -1)
//                    gs.vwIn = n++
//                    App.gsSubList.add(gs)
//                }
//        }
//        for (gs in mGsSet)
//            log("set has: ${gs.gsId} ")


//    val mGsSet = mutableSetOf<GroupSit>()
