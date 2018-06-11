package org.foundation101.karatel.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleExpandableListAdapter
import org.foundation101.karatel.R

class ExpandableListAdapterWithCustomDividers(context: Context,
                                              groupData: List<Map<String, Any>>,
                                              groupLayout: Int,
                                              groupFrom: Array<String>,
                                              groupTo: IntArray,
                                              childData: List<List<Map<String, Any>>>,
                                              childLayout: Int, childFrom: Array<String>,
                                              childTo: IntArray ):
                            SimpleExpandableListAdapter(context,
                                    groupData,
                                    groupLayout,
                                    groupFrom,
                                    groupTo,
                                    childData,
                                    childLayout,
                                    childFrom,
                                    childTo) {
    override fun getChildView(groupPosition: Int,
                              childPosition: Int,
                              isLastChild: Boolean,
                              convertView: View?,
                              parent: ViewGroup?): View {
        val view = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent)
        val dividerVisibility = if (childPosition == 0) View.INVISIBLE else View.VISIBLE
        view.findViewById<View>(R.id.dividerView).visibility = dividerVisibility
        return view
    }
}