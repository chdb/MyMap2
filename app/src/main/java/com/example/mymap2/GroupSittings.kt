package com.example.mymap2

//import android.text.Html
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterItem


//enum class GSMode(n :Int) { ONEHOUR (1)
//                          , LONGER  (2)
//                          , ONEDAY  (4)
//}
const val ONEHOUR = 0
const val LONGER  = 1
const val ONEDAY  = 2

const val ONEHOUR_BIT = 1
const val LONGER_BIT  = 2
const val ONEDAY_BIT  = 4

//typealias GSModes = EnumSet<GSMode>

data class GroupSit ( val id      :Int
                    , val name    :String
                    , val locn    :LatLng
                    , val onehour :String = ""
                    , val oneday  :String = ""
                    , val longer  :String = ""
                    , val general :String = ""
                    , val phone   :String = ""
                    , val email   :String = ""
                    , var mkr     :Marker? = null
                    , var idx     :Int = -1
                    , var modeSet :Int = 0
)   : ClusterItem
    , Comparable <GroupSit>
{
    init {
        val hBit = if (onehour != "") ONEHOUR_BIT else 0
        val lBit = if (longer  != "") LONGER_BIT  else 0
        val dBit = if (oneday  != "") ONEDAY_BIT  else 0
        modeSet = hBit or lBit or dBit
  //      require(modeSet != 0)  {"GS must have at least one of the 3 modes (onehour, longer, oneday)"}
       // require(phone!=null || email!=null) {"GS must have at least one contact point (phone, email)"}
    }
    override fun getPosition() = locn
    override fun getTitle() = null
    override fun getSnippet() = name
    override fun equals(other: Any?) = (other is GroupSit) && id == other.id
    override fun compareTo(other: GroupSit): Int {
        val d =  locn.longitude - other.locn.longitude
        if (d != 0.0)
            return if (d > 0.0) 1 else -1
        val e =  locn.latitude - other.locn.latitude
        if (e != 0.0)
            return if (e > 0.0) 1 else -1
        check (this == other) // Two different GroupSit's cannot have the same location.
        return 0
    }

    private fun newActivity (ctx :Context, intent :Intent, msg :String? = null) {
        if (intent.resolveActivity(ctx.packageManager) != null) {
            val intent2 = if (msg != null) Intent.createChooser(intent, msg)
            else intent
            ctx.startActivity(intent2)
        }
        //else alert: "no app is installed to handle this action"
    }
    fun sendSms(ctx :Context) {
        newActivity (ctx
            , Intent().apply {
                action = Intent.ACTION_SENDTO
                //type = "text/plain"
                data = Uri.parse("smsto:$phone")
                //putExtra("sms_body", "hello Hyunsuk!")
                //putExtra(Intent.EXTRA_STREAM, attachment)
            }
            ,"Choose sms app!"
        )}
    fun makePhonecall(ctx :Context){
        newActivity (ctx
            , Intent().apply {
                action = Intent.ACTION_DIAL
                data = Uri.parse("tel:$phone")
            }
            ,"Choose phone app!"
        )}
    fun getDirections(ctx :Context){
        val zoom = 12
        newActivity (ctx
            , Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("geo:0,0?z=$zoom&q=${locn.latitude},${locn.longitude}")
                //"...(${gs.name})" appended to url SHOULD result in a view with LABEL "gs.name" for the destination
                // (see https://developer.android.com/guide/components/intents-common#ViewMap about half way down: "geo:0,0?q=lat,lng(label)"
                // but this is not working currently see: https://issuetracker.google.com/issues/129726279
            }
            , "Choose navigation app!"
        )}
    fun composeEmail(ctx :Context){
        newActivity (ctx
            , Intent().apply {
                action = Intent.ACTION_SENDTO
                type = "*/*"   //"message/rfc822"
                data = Uri.parse("mailto:$email")
                // putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
                putExtra(Intent.EXTRA_SUBJECT, "MapApp enquiry:")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            , "Send an email to the Group Sitting Host"
        )}
}

abstract class App {
    companion object {
        lateinit var context :Context
        fun init (ctx :Context) {
            context = ctx
            allGroupSittings.sorted() // throws if 2 GSs have same locn
            var n = 0
            allGroupSittings.forEach { log("${n++}   ${it.name} ") }
        }

//        var selGroupSitIdx = -1
//        private fun selGroupSit :GroupSit? =
//            if (selGroupSitIdx < 0) null
//            else visGroupSittings [selGroupSitIdx]

        //var selGroupSit :GroupSit? = null
        //fun selGroupSit() = selGroupSit?: throw IllegalStateException("invalid call - selGroupSit is null")
        var visibleGSs = listOf<GroupSit>()

        val allGroupSittings = mutableListOf(
            GroupSit( 70
                ,"London, Dhamma Shed"
                , LatLng(51.544647, -0.065448)
                , "Every Evening: (just turn up)   7 –  8 pm"
                , "One Day Course:(book by email) \n" +
                     "   last Saturday of each month: 9am–6pm"
                , "3 hours: every Sunday:    10 am –  1 pm\n" +
                     "      and every Thursday:     9 am – 12 noon"
                , "We Never Cancel!  Cushions provided."
                , "07960 130 587"
                , "vipassana_hackney@gmail.com"
            )
            , GroupSit( 71
                ,"London, Bloomsbury"
                , LatLng(51.52293, -0.1175)
                , "One hour every Tuesday  7 - 8 pm"
                , "One Day Course on 2nd Sunday of each month 10 am - 4 pm.  " +
                        "Dress modestly. Bring a packed lunch.  No entry 10.15 to 12.40.  Afternoon sitters arrive 12.40 to 1.10pm"
                , general = "Cushions are provided. Please arrive in good time."
            )
            , GroupSit( 72
                ,"London, West Hampstead"
                , LatLng(51.546085, -0.189796)
                , "One hour every Monday  8 - 9 pm\n" +
                        "Bring a cushion, yoga mats are provided but please ask in advance"
            )
            , GroupSit(73
                ,"London, Paddington"
                , LatLng(51.52279, -0.192015)
                , "Every evening at 6 pm\n" +
                        "Cushions and chairs provided.\n" +
                        "Please contact Mohan before first visit."
            )
            , GroupSit( 74
                ,"London, Redbridge"
                , LatLng(51.57852, 0.05285)
                , "One hour Wednesdays and Fridays:\n" +
                        "7 pm - 8 pm"
                , longer = "Half Day every Saturday:\n" +
                        "9 am - 12 noon."
            )
            , GroupSit( 1
                ,"GS 1"
                , LatLng(51.6, 0.05)
                ,"1!"
                ,"1!"
            )
            , GroupSit( 16
                ,"GS 16"
                , LatLng(51.4, -0.05)
                ,"16!"
            )
            , GroupSit( 2
                ,"GS 2"
                , LatLng(51.6, 0.06)
                ,"2!"
                ,"2!"
            )
            , GroupSit( 15
                ,"GS 15"
                , LatLng(51.4, -0.06)
                ,"15!"
                ,"15!"
            )
            , GroupSit( 3
                ,"GS 3"
                , LatLng(51.6, 0.07)
                ,"3!"
            )
            , GroupSit( 14
                ,"GS 14"
                , LatLng(51.41, -0.07)
                ,"14!"
            )
            , GroupSit( 13
                ,"GS 13"
                , LatLng(51.42, -0.08)
                ,"13!"
            )
            , GroupSit( 12
                ,"GS 12"
                , LatLng(51.43, -0.09)
                ,"12!"
            )
            , GroupSit( 11
                ,"GS 11"
                , LatLng(51.44, -0.11)
                ,"11!"
            )
            , GroupSit( 100
                ,"GS Ireland"
                , LatLng(51.44, -10.11)
            )
            , GroupSit( 200
                ,"GS Germany"
                , LatLng(51.44, 10.11)
            )
        )
    }
}

fun share1(){
    val shareBody = "Here is the share content body"
    val sharingIntent = Intent(Intent.ACTION_VIEW)
    sharingIntent.type = "text/plain"
    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
    sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
    App.context.startActivity(sharingIntent)
//        Intent.createChooser(
//            sharingIntent,
//            "share_using ... ok?")
//        )
}
fun share2() {
    val excludedComponents = ArrayList<ComponentName>()
    val shareIntent = Intent()
    shareIntent.action = Intent.ACTION_SEND
    shareIntent.type = "text/plain"
    val resolveInfoList = App.context.packageManager.queryIntentActivities(shareIntent, 0)
    for (resInfo in resolveInfoList) {
        val packageName = resInfo.activityInfo.packageName
        val name = resInfo.activityInfo.name
        if (!  (packageName.contains("com.facebook") ||
                packageName.contains("com.twitter.android") ||
                packageName.contains("com.google.android.gm") ||
                packageName.contains("com.android.mms") ||
                packageName.contains("com.whatsapp"))
        ) {
            excludedComponents.add(ComponentName(packageName, name))
        }
    }
    if (excludedComponents.size==resolveInfoList.size) {
        toast("No apps to share !")
    } else {
        val chooserIntent = Intent.createChooser(shareIntent , "Contact GS Host via")
        chooserIntent.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,  //require v24
            excludedComponents.toTypedArray()
        )
        App.context.startActivity(chooserIntent)
    }
}

fun share(){
    // val resources = App.context.getResources()
    val emailIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        //type = "message/rfc822" //  "*/*"   //
        //data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_TEXT, "share_email_text")
        putExtra(Intent.EXTRA_SUBJECT, "share_email_subject")
    }
    // Native email client doesn't currently support HTML, but it doesn't hurt to try in case they fix it
    //emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("share_email_native"))
   // val chooser = Intent.createChooser(emailIntent, "chooser_text")

    val pm = App.context.packageManager
    val sendIntent = Intent(Intent.ACTION_SEND)
    sendIntent.type = "text/plain"
    val intentList = ArrayList<LabeledIntent>()
    for (ri in pm.queryIntentActivities(sendIntent, 0)) {
        // Extract the label, append it, and repackage it in a LabeledIntent
        //ResolveInfo ri = resInfo.get(i)
        //log("ri = $ri")
        val pkgName = ri.activityInfo.packageName
        log("pkgName =  $pkgName")
        val bTwitter = pkgName.contains("twitter")
        val bFacebook= pkgName.contains("facebook")
        val bMMS     = pkgName.contains("mms")
        val bGmail   = pkgName.contains("android.gm")

        if (pkgName.contains("android.email")) {
            emailIntent.setPackage(pkgName)
        } else if (bTwitter || bFacebook || bMMS || bGmail) {
            val intent = Intent().apply{
                component = ComponentName(pkgName, ri.activityInfo.name)
                action = Intent.ACTION_SEND
                type = "text/plain"
            }

            if (bTwitter) {
                intent.putExtra(Intent.EXTRA_TEXT, "share_twitter")
            } else if(bFacebook) {
                // Warning: Facebook IGNORES our text. They say "These fields are intended for users to express themselves. Pre-filling these fields erodes the authenticity of the user voice."
                // One workaround is to use the Facebook SDK to post, but that doesn't allow the user to choose how they want to share. We can also make a custom landing page, and the link
                // will show the <meta content ="..."> text from that page with our link in Facebook.
                intent.putExtra(Intent.EXTRA_TEXT, "share_facebook")
            } else if(bMMS) {
                intent.putExtra(Intent.EXTRA_TEXT, "share_sms")
            } else if(bGmail) { // If Gmail shows up twice, try removing this else-if clause and the reference to "android.gm" above
                intent.putExtra(Intent.EXTRA_TEXT, "share_email_gmail")
                intent.putExtra(Intent.EXTRA_SUBJECT, "share_email_subject")
                intent.type = "message/rfc822"
            }

            intentList.add( LabeledIntent(intent, pkgName, ri.loadLabel(pm), ri.icon))
        }
    }
    for (i in intentList) log( "$i")

    //val extraIntents = intentList.toArray()
    emailIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray())
    App.context.startActivity(emailIntent)

    if (emailIntent.resolveActivity(App.context.packageManager) != null) {
        App.context.startActivity(emailIntent)
    }
    else toast("there is no app to handle this! ")
}
