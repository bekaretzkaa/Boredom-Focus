package com.example.boredomfocus.feature.sessionsettings.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.boredomfocus.R
import com.example.boredomfocus.core.permission.PermissionViewModel
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.databinding.DialogSessionWarningBinding

class SessionWarningDialogFragment : DialogFragment() {

    private val permissionViewModel: PermissionViewModel by activityViewModels()
    private var _binding: DialogSessionWarningBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "SessionWarningDialogFragment"

        const val ARG_DURATION = "duration"
        const val ARG_DIFFICULTY = "difficulty"
        const val ARG_FOCUS_ONLY = "focusOnly"

        const val REQUEST_KEY = "warning_result"
        const val KEY_CONFIRMED = "confirmed"

        fun newInstance(
            detoxDuration: DetoxDuration,
            difficulty: Difficulty,
            focusOnly: Boolean
        ) = SessionWarningDialogFragment().apply {
            arguments = bundleOf(
                ARG_DURATION to detoxDuration,
                ARG_DIFFICULTY to difficulty,
                ARG_FOCUS_ONLY to focusOnly
            )
        }
    }

    private val difficulty: Difficulty
        get() = requireArguments().getSerializable(ARG_DIFFICULTY) as Difficulty

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.CustomCenterDialogTheme)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogSessionWarningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val params = attributes
            params.dimAmount = 0.78f
            attributes = params

            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStart.text = "Начать сессию"
        binding.btnCancel.text = "Отмена"
        binding.cardInstruction.visibility = View.VISIBLE
        binding.cardSettings.visibility = View.GONE

        binding.btnStart.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(KEY_CONFIRMED to true)
            )
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(KEY_CONFIRMED to false)
            )
            dismiss()
        }



        when(difficulty) {
            Difficulty.BEGINNER -> showBeginnerUi()
            Difficulty.FIGHTER -> showFighterUi()
            Difficulty.HARDCORE -> showHardcoreUi()
        }
    }

    private fun showBeginnerUi() {
        binding.cardDifficulty.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_bg))
        binding.cardDifficulty.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.green_basic))
        binding.tvDifficulty.text = "Уровень: Новичок"
        binding.tvDifficulty.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_basic))

        binding.tvSessionDescription.text = "На этом уровне Do Not Disturb остается выключенным - во время детокса и фокуса"
        binding.tvInstruction.text = "Все звонки и уведомления будут приходить как обычно"
        binding.tvInstruction.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    private fun showFighterUi() {
        binding.cardDifficulty.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange_bg))
        binding.cardDifficulty.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange))
        binding.tvDifficulty.text = "Уровень: Боец"
        binding.tvDifficulty.setTextColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange))

        binding.tvSessionDescription.text = "Включится Do Not Disturb Priority - на детокс и фокус"
        binding.tvInstruction.text = "Избранные звонки и сообщения приходят\n\nОстальные уведомления - заблокированы"
        binding.tvInstruction.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
    }

    private fun showHardcoreUi() {
        binding.cardDifficulty.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.difficulty_red_bg))
        binding.cardDifficulty.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.red_basic))
        binding.tvDifficulty.text = "Уровень: Хардкор"
        binding.tvDifficulty.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_basic))

        binding.tvSessionDescription.text = "Включится Do Not Disturb None - на детокс. Для фокуса переключится на Priority"
        binding.tvInstruction.text = "Все уведомления и звонки - заблокированы\n\nДаже избранные контакты - без исключений"
        binding.tvInstruction.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
    }

    private fun showPermissionDnd() {
        binding.tvStart.text = "Включите Do Not Disturb"
        binding.tvSessionDescription.text = "Для этого уровня нужен режим Do Not Disturb, а он сейчас выключен в настройках телефона."

        binding.cardInstruction.visibility = View.GONE
        binding.cardSettings.visibility = View.VISIBLE

        binding.btnStart.text = "Проверить снова"
        binding.btnCancel.text = "Открыть настройки"
    }

}