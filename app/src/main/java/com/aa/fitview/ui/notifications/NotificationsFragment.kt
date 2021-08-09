package com.aa.fitview.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aa.fitview.*
import com.aa.fitview.databinding.FragmentNotificationsBinding
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.EventBus




class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private var isCreated = false
    private lateinit var chartData: Cartesian

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var chart: AnyChartView
    private lateinit var dataType: DataType

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        EventBus.getDefault().register(this)
        isCreated = true

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        chart = binding.chart
        chartData = AnyChart.column()
        chart.setChart(chartData)

        dataType = DataType.STEPS

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioSteps -> dataType = DataType.STEPS
                R.id.radioDistance -> dataType = DataType.DISTANCE
                R.id.radioTime -> dataType = DataType.TIME
            }
            setData(dataType)
        }

        FitData.getFitData(requireContext(), Request.BYDAY)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isCreated = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: Messages.FitResult?) {
        if (!isCreated) return

        Log.i(TAG, "Message received")

        binding.progressBar.visibility = View.GONE

        setData(dataType)

       // binding.chart.background = ColorDrawable(Color.RED)
    //    binding.chart.foreground = ColorDrawable(Color.YELLOW)

    }

    private fun setData(dataType: DataType) {
        var data:  ArrayList<Int?> = FitData.steps
        val entries =  ArrayList<ValueDataEntry>()

        when (dataType) {
            DataType.STEPS -> data = FitData.steps
            DataType.DISTANCE -> data = FitData.distances
            DataType.TIME -> data = FitData.times
        }

        for ((i, point) in data.withIndex())
        {
            entries.add(ValueDataEntry(i.toFloat(), point?.toFloat()))
        }

        chartData.data(entries as List<DataEntry>?)
    }
}