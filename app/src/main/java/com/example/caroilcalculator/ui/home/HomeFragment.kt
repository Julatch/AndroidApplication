package com.example.caroilcalculator.ui.home

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.caroilcalculator.R
import com.example.caroilcalculator.databinding.FragmentHomeBinding
import java.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var currentState = 0 // 0: 工作中, 1: 会议中, 2: 休息中
    private var remainingTimeMillis: Long = 3600000 // 1小时
    private var restTimeMill: Long = 0
    private var restReminderMill: Long = 0
    private var waterReminderMill: Long = 0
    private var workStartTime: Long = 0
    private var workEndTime: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusChecker: Runnable
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        initializeUI()
        setupSwitch()
        setupSpinner()
        setupButton()
        setupGenerateButton()
        //startStatusChecker()
//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }
    private fun initializeUI() {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isWorking = sharedPreferences.getBoolean("isWorking", false)
        val endTime = sharedPreferences.getLong("endTime", 0)
        val currentTime = System.currentTimeMillis()

        if (isWorking && endTime > currentTime) {
            remainingTimeMillis = endTime - currentTime
            binding.workStatus.isChecked = true
            binding.workStatus.text = "勤務中"
            startStatusChecker()
        } else {
            binding.workStatus.isChecked = false
            binding.workStatus.text = "勤務終了"
            binding.textCurrentStatus.text = "お疲れ様です。"
            binding.textCurrentStatus.setTextColor(resources.getColor(R.color.black1))
            binding.textView5.text = ""
            binding.countdown.text = "停止中"
            binding.buttonToggleStatus.setBackgroundColor(resources.getColor(R.color.gray1))
            binding.buttonToggleStatus.text = "勤務終了"
        }
    }
    private fun setupSwitch() {
        binding.workStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 上班
                workStartTime = System.currentTimeMillis()
                binding.workStatus.text = "勤務中"
                binding.workTotaltimeCount.text="-"
                enableButton()
                updateStatus()
                startStatusChecker()

            } else {
                // 下班
                forbidButton()
                workEndTime = System.currentTimeMillis()
                val workDuration = workEndTime - workStartTime
                val hours = (workDuration / (1000 * 60 * 60)).toInt()
                val minutes = ((workDuration / (1000 * 60)) % 60).toInt()
                binding.workStatus.text = "勤務終了"
                binding.workTotaltimeCount.text="${hours}時${minutes}分"
                binding.textCurrentStatus.text = "お疲れ様です。"
                binding.textCurrentStatus.setTextColor(resources.getColor(R.color.black1))
                binding.textView5.text = ""
                binding.countdown.text = "停止中"
                binding.buttonToggleStatus.setBackgroundColor(resources.getColor(R.color.gray1))
                binding.buttonToggleStatus.text = "勤務終了"
                stopStatusChecker()
            }
        }
    }


    private fun setupButton() {
        binding.buttonToggleStatus.setOnClickListener {
            toggleStatus()
        }
    }

    private fun forbidButton() {
        binding.buttonToggleStatus.isEnabled = false
    }

    private fun enableButton() {
        binding.buttonToggleStatus.isEnabled = true
    }

    private fun setupSpinner() {
        val restReminderAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.rest_reminder_array,
            android.R.layout.simple_spinner_item
        )
        restReminderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val restTimeAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.rest_time_array,
            android.R.layout.simple_spinner_item
        )
        restTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val waterReminderAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.water_reminder_array,
            android.R.layout.simple_spinner_item
        )
        waterReminderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.restReminderSpinner.adapter = restReminderAdapter
        binding.restTimeSpinner.adapter = restTimeAdapter
        binding.waterReminderSpinner.adapter = waterReminderAdapter

        binding.restReminderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Handle rest reminder selection
                val restReminderIntervals = arrayOf(15 * 60 * 1000, 30 * 60 * 1000, 60 * 60 * 1000, 2 * 60 * 60 * 1000, 3 * 60 * 60 * 1000)
                restReminderMill = restReminderIntervals[position].toLong()
                updateStatus()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        binding.restTimeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Handle rest reminder selection
                val restTimes = arrayOf(5 * 60 * 1000, 10 * 60 * 1000, 15 * 60 * 1000, 20 * 60 * 1000, 30 * 60 * 1000, 60 * 60 * 1000)
                restTimeMill = restTimes[position].toLong()
                updateStatus()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        binding.waterReminderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Handle water reminder selection
                val waterReminderIntervals = arrayOf(15 * 60 * 1000, 30 * 60 * 1000, 60 * 60 * 1000, 2 * 60 * 1000, 3 * 60 * 1000)
                waterReminderMill = waterReminderIntervals[position].toLong()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }
    private fun toggleStatus() {
        currentState = (currentState + 1) % 3
        updateStatus()
    }
    private fun updateStatus() {
        when (currentState) {
            0 -> {
                binding.textCurrentStatus.text = "仕事頑張って!!"
                binding.textCurrentStatus.setTextColor(resources.getColor(R.color.lime))
                binding.buttonToggleStatus.text = "仕事中"
                binding.buttonToggleStatus.setBackgroundColor(resources.getColor(R.color.lime))
                binding.textView5.text = "休憩まで:"
                remainingTimeMillis = restReminderMill
                updateRemainingTime()
            }
            1 -> {
                binding.textCurrentStatus.text = "会議に集中しましょう"
                binding.textCurrentStatus.setTextColor(resources.getColor(R.color.red))
                binding.buttonToggleStatus.setBackgroundColor(resources.getColor(R.color.red))
                binding.buttonToggleStatus.text = "会議中"
                binding.textView5.text = "リマインダー:"
                binding.countdown.text = "停止中"
            }
            2 -> {
                binding.textCurrentStatus.text = "ゆっくり休憩してください～♪"
                binding.textCurrentStatus.setTextColor(resources.getColor(R.color.darkgreen))
                binding.buttonToggleStatus.text = "休憩中"
                binding.textView5.text = "仕事開始まで:"
                binding.buttonToggleStatus.setBackgroundColor(resources.getColor(R.color.darkgreen))
                remainingTimeMillis = restTimeMill
                updateRemainingTime()
            }
        }
    }
    private fun updateRemainingTime() {
        val minutes = (remainingTimeMillis / 1000) / 60
        val seconds = (remainingTimeMillis / 1000) % 60
        binding.countdown.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun startStatusChecker() {

        statusChecker = object : Runnable {
            override fun run() {
                if(currentState!=1){
                    if (remainingTimeMillis > 0) {
                        remainingTimeMillis -= 1000
                        updateRemainingTime()
                        handler.postDelayed(this, 1000)
                    } else {
                        if (currentState == 0) {
                            binding.textCurrentStatus.text = "メリハリ大事、休憩しましょう！"
                            binding.textCurrentStatus.setTextColor(resources.getColor(R.color.black))
                            handler.postDelayed(this, 1000)
                        } else if (currentState == 2) {
                            binding.textCurrentStatus.text = "休憩終了、仕事に戻りましょう！"
                            binding.textCurrentStatus.setTextColor(resources.getColor(R.color.black))
                            handler.postDelayed(this, 1000)
                        }
                    }
                }else{
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(statusChecker)
    }
    private fun stopStatusChecker() {

        handler.removeCallbacks(statusChecker)
//        WorkManager.getInstance(requireContext()).cancelAllWork()
    }

    private fun setAlarm(hour: Int, minute: Int, message: String) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        //alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun setupGenerateButton() {
        binding.buttonGenerateContent.setOnClickListener {
            generateRandomContent()
        }
    }
    private fun generateRandomContent() {
        Log.d("GenerateContent", "开始生成内容")
        val client = OkHttpClient()
        val json = """
        {
            "prompt": "健康アドバイスを生成してください。内容は短時間内でできる運動や、首・目・腰の保護や、体にいい食べ物など",
            "max_tokens": 50
        }
    """.trimIndent()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/engines/davinci-codex/completions")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .addHeader("Authorization", "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")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("NetworkError", "请求失败: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(context, "请求失败，请稍后再试", Toast.LENGTH_SHORT).show()
                }

            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    Log.d("APIResponse", "响应数据: $responseData")
                    try {
                        val jsonObject = JSONObject(responseData)
                        val choicesArray = jsonObject.getJSONArray("choices")
                        val generatedText = choicesArray.getJSONObject(0).getString("text")
                        activity?.runOnUiThread {
                            binding.textView2.text = generatedText
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        activity?.runOnUiThread {
                            Toast.makeText(context, "解析响应数据时出错", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        Log.e("APIError", "请求失败: ${response.code} - ${response.message}")
                        Toast.makeText(context, "+失败，请稍后再试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}