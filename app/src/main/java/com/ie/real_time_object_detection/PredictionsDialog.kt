package com.ie.real_time_object_detection

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider

class PredictionsDialog: DialogFragment() {

    private val viewModel: ScoreViewModel by activityViewModels()

    private fun toPercentageString(value: Float): String {
        return String.format("%.2f%%", value * 100)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = layoutInflater.inflate(R.layout.dialog_predictions, null)

        val predictionTitle = view.findViewById<TextView>(R.id.predictionTitle)
        predictionTitle.text = "${viewModel.name} predicted with ${toPercentageString(viewModel.score!!)} of score"

        val tipsText = view.findViewById<TextView>(R.id.recycleTip)
        val loading = view.findViewById<ProgressBar>(R.id.loadingTips)
        loading.isVisible = true
        viewModel.tips.observe(this) { tips ->
                tipsText.text = tips
                loading.isVisible = false
            }
        viewModel.fetchTips()

        val builder = AlertDialog.Builder(requireActivity())
            .setView(view)

        return builder.create()
    }


}