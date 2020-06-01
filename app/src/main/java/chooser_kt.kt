import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import java.util.*

//class Custom_chooser : Activity() {
//    var adapter: AppAdapter? = null
//    var btn1: Button? = null
//    var email = Intent(Intent.ACTION_SEND)
//    public override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.main)
//        btn1 = findViewById<View>(R.id.button1) as Button
//        btn1!!.setOnClickListener { // TODO Auto-generated method stub
//            show_custom_chooser()
//        }
//    }
//
//    fun show_custom_chooser() {
//        // TODO Auto-generated method stub
//        val dialog = Dialog(this@Custom_chooser)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        val WMLP = dialog.window!!.attributes
//        WMLP.gravity = Gravity.CENTER
//        dialog.window!!.attributes = WMLP
//        dialog.window!!.setBackgroundDrawable(
//            ColorDrawable(Color.TRANSPARENT)
//        )
//        dialog.setCanceledOnTouchOutside(true)
//        dialog.setContentView(R.layout.about_dialog)
//        dialog.setCancelable(true)
//        val lv =
//            dialog.findViewById<View>(R.id.listView1) as ListView
//        val pm = packageManager
//        email.putExtra(Intent.EXTRA_EMAIL, arrayOf("velmurugan@androidtoppers.com"))
//        email.putExtra(Intent.EXTRA_SUBJECT, "Hi")
//        email.putExtra(Intent.EXTRA_TEXT, "Hi,This is Test")
//        email.type = "text/plain"
//        val launchables = pm.queryIntentActivities(email, 0)
//        Collections.sort(
//            launchables,
//            ResolveInfo.DisplayNameComparator(pm)
//        )
//        adapter = AppAdapter(pm, launchables)
//        lv.adapter = adapter
//        lv.onItemClickListener =
//            OnItemClickListener { arg0, arg1, position, arg3 -> // TODO Auto-generated method stub
//                val launchable = adapter!!.getItem(position)
//                val activity = launchable!!.activityInfo
//                val name = ComponentName(
//                    activity.applicationInfo.packageName,
//                    activity.name
//                )
//                email.addCategory(Intent.CATEGORY_LAUNCHER)
//                email.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
//                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
//                email.component = name
//                startActivity(email)
//            }
//        dialog.show()
//    }
//
//    inner class AppAdapter(
//        pm: PackageManager?,
//        apps: List<ResolveInfo>?
//    ) :
//        ArrayAdapter<ResolveInfo?>(this@Custom_chooser, R.layout.row, apps!!) {
//        private val pm: PackageManager? = null
//        override fun getView(
//            position: Int, convertView: View?,
//            parent: ViewGroup
//        ): View {
//            var convertView = convertView
//            if (convertView == null) {
//                convertView = newView(parent)
//            }
//            bindView(position, convertView)
//            return convertView
//        }
//
//        private fun newView(parent: ViewGroup): View {
//            return layoutInflater.inflate(R.layout.row, parent, false)
//        }
//
//        private fun bindView(position: Int, row: View?) {
//            val label = row!!.findViewById<View>(R.id.label) as TextView
//            label.text = getItem(position)!!.loadLabel(pm)
//            val icon =
//                row.findViewById<View>(R.id.icon) as ImageView
//            icon.setImageDrawable(getItem(position)!!.loadIcon(pm))
//        }
//
//        init {
//            this.pm = pm
//        }
//    }
//}