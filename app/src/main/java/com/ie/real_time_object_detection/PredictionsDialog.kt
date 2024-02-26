package com.ie.real_time_object_detection

import android.app.Dialog
import android.content.DialogInterface
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

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_SCORE = "score"

        fun newInstance(name: String, score: Float): PredictionsDialog {
            val args = Bundle().apply {
                putString(ARG_NAME, name)
                putFloat(ARG_SCORE, score)
            }
            return PredictionsDialog().apply {
                arguments = args
            }
        }
    }

    private val viewModel: ScoreViewModel by activityViewModels()
    lateinit var tipsText: TextView
    lateinit var loading: ProgressBar

    private fun toPercentageString(value: Float): String {
        return String.format("%.2f%%", value * 100)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val name = arguments?.getString(ARG_NAME)
        val score = arguments?.getFloat(ARG_SCORE)

        val view = layoutInflater.inflate(R.layout.dialog_predictions, null)

        val predictionTitle = view.findViewById<TextView>(R.id.predictionTitle)
        predictionTitle.text = "$name predicted with ${toPercentageString(score!!)} of score"

        tipsText = view.findViewById(R.id.recycleTip)
        loading = view.findViewById(R.id.loadingTips)
        resetTips()
        viewModel.tips.observe(this) { tips ->
                if(tips.isNullOrBlank()){
                    resetTips()
                }else{
                    tipsText.text = tips
                    loading.isVisible = false
                }
            }
        viewModel.fetchTips(name!!)

        val builder = AlertDialog.Builder(requireActivity())
            .setView(view)

        return builder.create()
    }

    private fun resetTips(){
        loading.isVisible = true
        tipsText.text = ""
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        resetTips()
        viewModel.resetTips()
    }


}