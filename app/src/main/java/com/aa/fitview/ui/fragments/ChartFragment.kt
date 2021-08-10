package com.aa.fitview.ui.fragments

import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.aa.fitview.*
import com.aa.fitview.databinding.FragmentChartBinding
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.EventBus

open class ChartFragment(private val period: Period) : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private var isCreated = false
    private lateinit var chartData: Cartesian

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var chart: AnyChartView
    private lateinit var dataType: DataType
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        EventBus.getDefault().register(this)
        isCreated = true

        _binding = FragmentChartBinding.inflate(inflater, container, false)
        val root: View = binding.root
        chart = binding.chart
        chartData = AnyChart.column()
        chart.setChart(chartData)

        dataType = DataType.STEPS

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.progressBar.visibility = View.VISIBLE

            when (checkedId) {
                R.id.radioSteps -> dataType = DataType.STEPS
                R.id.radioDistance -> dataType = DataType.DISTANCE
                R.id.radioTime -> dataType = DataType.TIME
            }
            FitData.getFitData(requireContext(), period, dataType)
            setData(dataType)
        }

        progressBar = binding.progressBar

        FitData.getFitData(requireContext(), period, DataType.STEPS)

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

        progressBar.visibility = View.GONE
        Log.i(TAG, "Visibility=" + progressBar.visibility)

        setData(dataType)

       // binding.chart.background = ColorDrawable(Color.RED)
    //    binding.chart.foreground = ColorDrawable(Color.YELLOW)

    }

    private fun setData(dataType: DataType) {
        var periodData: Data? = null
        periodData = when(period) {
            Period.DAY -> FitData.dayData
            Period.WEEK -> FitData.weekData
            Period.MONTH -> FitData.monthData
        }

        var data:  ArrayList<Int?>? = null
        val entries =  ArrayList<ValueDataEntry>()

        data = when (dataType) {
            DataType.STEPS, DataType.DISTANCE -> periodData.steps
//            DataType.DISTANCE -> periodData.distances
            DataType.TIME -> periodData.times
        }

        for ((i, point) in data.withIndex())
        {
            when(dataType) {
                DataType.DISTANCE ->
                    entries.add(ValueDataEntry(i.toFloat(), if (point == null) 0f else point*0.7165f/1609))
                DataType.TIME ->
                    entries.add(ValueDataEntry(i.toFloat(), if (point == null) 0f else point.toFloat()/60))  // Convert to hours
                DataType.STEPS ->
                    entries.add(ValueDataEntry(i.toFloat(), if (point == null) 0f else point/1000))  // Convert to 1000nds
        }
        }

        chartData.data(entries as List<DataEntry>?)
        chartData.xAxis(0).labels().format("{%value}{type:number, decimalsCount:0}");
    }
}