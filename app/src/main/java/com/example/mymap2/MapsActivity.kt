package com.example.mymap2

//import android.R

//import com.google.maps.android.clustering.algo

//import android.R

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
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
    lateinit var mMap: GoogleMap

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
    lateinit var mClusterMgr: ClusterManager <GroupSit>
    var mPermissionMgr = PermissionMgr (this)
    private val mGroupSitEd = GroupSitEditor(this)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val smf = supportFragmentManager.findFragmentById(R.id.mapFrag) as SupportMapFragment
        smf.getMapAsync(this)
        App.init(this)
        mDefMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        mSelMarkerIcon = BitmapDescriptorFactory.defaultMarker()
        initViewPager()

        mPermissionMgr.getAccountPmn()
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
        mMap.animateCamera( CameraUpdateFactory.newLatLngZoom (gs0.locn, 10f))

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
                mClusterMgr.cluster()
            } else {
                log("no zoom - onCameraIdle() calls setSubList()")
                setVisibleGSs(false)
            }
            log("end CameraIdle ")
        }
    }

    override fun onRequestPermissionsResult (requestCode: Int, permissions: Array<String>, results: IntArray) {
        mPermissionMgr.onRequestPermissionsResult (requestCode, permissions, results)
    }

    override fun onActivityResult (requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPermissionMgr.onActivityResult (requestCode, resultCode, data)
    }



    fun onClickEdit(editBtn: View){
        mGroupSitEd.onClickEdit (editBtn)
    }

    fun onClickEditDone(doneBtn: View){
        //todo: Save changes
        mGroupSitEd.doneOrCancel (doneBtn, true)
    }

    fun onClickEditCancel(cancelBtn: View) {
        mGroupSitEd.doneOrCancel (cancelBtn, false)
    }

    var mSelectedGS :GroupSit? = null

    private fun setSelectedGs(idx: Int) {
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
    private fun adjustForVwPager(bounds: LatLngBounds, reduce: Boolean) :LatLngBounds {
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
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLoadedCallback(this)
  //      googleMap.uiSettings.isZoomControlsEnabled = true
        mClusterMgr = ClusterManager<GroupSit>(this, googleMap)
        mClusterMgr.addItems(App.allGroupSittings) // 4
        log("onMapReady")
        //todo: under the default algorithm, cluster() calculates the clustering for the entire world, at current zoom level.
        // For a larger data set we will probably need a more focused algorithm
        // eg "NonHierarchicalVIEWBasedAlgorithm" which is similar to the default "NonHierarchicalDISTANCEBasedAlgorithm"
        // but "works only on the visible area"; therefore cluster() should be much faster but would need to be called on a pan as well as on a zoom.
        // Android Studio would need to include the package dependency etc.
        // https://github.com/googlemaps/android-maps-utils/tree/master/library/src/main/java/com/google/maps/android/clustering/algo
        //             mClusterManager.algorithm = NonHierarchicalViewBasedAlgorithm (width, height)
        // Alternatively - other cluster libraries ???

        mMap.setOnMarkerClickListener(mClusterMgr)

        mClusterMgr.setOnClusterClickListener {
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
        mClusterMgr.setOnClusterItemClickListener { // NB this means a free GS was clicked - NOT a cluster!
            log("free item clicked")
            val idx = App.visibleGSs.indexOf(it)

            vwPager.currentItem = idx //it.idx
            //val lastMarker = mRenderer.getMarker(mSelectedGS)
            //log ("selected marker = $lastMarker")
            setSelectedGs(idx) //(it.idx)
            //mRenderer.getMarker(it)?.showInfoWindow()
            true
        }

        mClusterMgr.renderer = CustomClusterRenderer(this, mMap, mClusterMgr)
        mClusterMgr.renderer.setAnimation(true)
        mPermissionMgr.update()
    }

    var mMapLoaded = false
    var mCountGSs =  App.allGroupSittings.size
    val mMinClusterSize = 3
    private var mFreeGSs = listOf<GroupSit>() // Free Groups Sits, sorted by longitude, latitude
    // "Free" is our term for all items "not rendered as clusters" (in the terms of the library)
    // The Clustering algorithm sorts ALL GSs into what it confusingly calls "Clusters", even though
    // the job is only to put some of them, those "close enough", into visible (rendered) clusters.
    // (All items are in "Clusters" but NOT all items are "RENDERED AS Clusters")
    // Renderer.shouldRenderAsCluster() decides which of these "Cluster"s actually remain as "free" markers.
    // And so for this library, all items are "Cluster Items", whether or not they are (rendered as) clustered


    private fun setVisibleGSs(newClusters: Boolean) { //(clusters: MutableSet<out Cluster<GroupSit>>? = null) {
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
        log("new idx = $newSelGSIdx")
        updateVwPager(newVisibles)

        if (newSelGSIdx > 0)
            vwPager.currentItem = newSelGSIdx

        setSelectedGs(newSelGSIdx)
    }


    private fun updateVwPager(newVisibles: List<GroupSit>) {
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

    inner class CustomClusterRenderer(
        context: MapsActivity, map: GoogleMap?, clusterManager: ClusterManager<GroupSit>
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
