package com.aa.fitview.ui.dashboard

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aa.fitview.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.ThreadMode

import org.greenrobot.eventbus.Subscribe

class DashboardFragment : Fragment() {

    private var columnCount = 1
    lateinit var savedView: RecyclerView
    private var isCreated = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this)
        isCreated = true

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isCreated = false
        EventBus.getDefault().unregister(this);
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                view.adapter = DashboardRecyclerViewAdapter(ArrayList<Int>())
            }
            savedView = view
        }
        FitData.getFitData(requireContext(), Request.BYDAY)
        return view
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: Messages.FitResult?) {
        if (!isCreated) return
        Log.i(TAG, "Message received")
        savedView.adapter = DashboardRecyclerViewAdapter(FitData.steps)
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            DashboardFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}