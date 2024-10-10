package com.example.caroilcalculator.ui.home

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.caroilcalculator.CountdownService
import com.example.caroilcalculator.R
import com.example.caroilcalculator.WaterRemindService
import com.example.caroilcalculator.databinding.FragmentHomeBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

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
    private var isWorking :Boolean = false
    private var onCount :Boolean = false
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            remainingTimeMillis = intent.getLongExtra("remaining_time", 0)
            var countdownStatus = intent.getLongExtra("countdown_status", 1)
            if(countdownStatus.toInt() == 0){
                stopService()
            }
            // 在这里处理接收到的广播
            updateRemainingTime()
        }
    }

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
        setupToggleButton()
        waterSwitch()
        //startStatusChecker()
//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // 注册广播接收器
        val filter = IntentFilter("com.example.yourapp.COUNTDOWN_UPDATE")
        requireActivity().registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        // 取消注册广播接收器
        requireActivity().unregisterReceiver(receiver)
    }
    private fun initializeUI() {

        if (isWorking) {
            binding.workStatus.text = "勤務中"
            updateStatus()
            //startStatusChecker()
        } else {
            binding.workStatus.text = "勤務終了"
            binding.textCurrentStatus.text = "お疲れ様です。"
            binding.textCurrentStatus.setTextColor(resources.getColor(R.color.black1))
            binding.textView5.text = ""
            binding.countdown.text = "停止中"
            binding.buttonToggleStatus.setBackgroundColor(resources.getColor(R.color.gray1))
            binding.buttonToggleStatus.text = "勤務終了"
        }
    }
    private fun waterSwitch(){
        binding.waterRemindSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val serviceIntent = Intent(requireContext(), WaterRemindService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(serviceIntent)
                } else {
                    requireContext().startService(serviceIntent)
                }

                val intent = Intent("com.example.caroilcalculator.ALARM_ACTION")
                intent.putExtra("remind_time", waterReminderMill)
                requireContext().sendBroadcast(intent)
            } else {
                val serviceIntent = Intent(requireContext(), WaterRemindService::class.java)
                requireActivity().stopService(serviceIntent)
            }
        }

    }
    private fun setupSwitch() {
        binding.workStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 上班
                isWorking = true
                workStartTime = System.currentTimeMillis()
                binding.workStatus.text = "勤務中"
                binding.workTotaltimeCount.text="-"
                enableButton()
                updateStatus()
                updateRemainingTime()
            } else {
                stopService()
                // 下班
                isWorking = false
                onCount = false
                forbidButton()
                workEndTime = System.currentTimeMillis()
                val workDuration = workEndTime - workStartTime
                val hours = (workDuration / (1000 * 60 * 60)).toInt()
                val minutes = ((workDuration / (1000 * 60)) % 60).toInt()
                binding.workStatus.text = "勤務終了"
                binding.workTotaltimeCount.text= String.format("%02d:%02d", hours,minutes)
                binding.textCurrentStatus.text = "お疲れ様です。"
                binding.textCurrentStatus.setTextColor(resources.getColor(R.color.black1))
                binding.textView5.text = ""
                binding.countdown.text = "停止中"
                binding.buttonToggleStatus.setBackgroundColor(resources.getColor(R.color.gray1))
                binding.buttonToggleStatus.text = "勤務終了"
            }
        }
    }


    private fun setupButton() {

            binding.buttonToggleStatus.setOnClickListener {
                if(isWorking){
                    toggleStatus()
                }

            }


    }

    private fun setupToggleButton() {
        val toggleButton: ToggleButton = binding.countdownStartButton
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if(isWorking){
                if (isChecked) {
                    // Toggle button is enabled
                    launchService()
                    onCount = true
                } else {
                    stopService()
                    updateStatus()
                    updateRemainingTime()
                    // Toggle button is disabled
                    onCount = false
                }
            }else {
                toggleButton.isEnabled = false
                onCount = false
            }
        }

    }

    private fun stopService() {
        val intent = Intent(requireContext(), CountdownService::class.java)
        requireActivity().stopService(intent)
    }

    private fun forbidButton() {
        binding.buttonToggleStatus.isEnabled = false
        binding.countdownStartButton.isEnabled = false
    }

    private fun enableButton() {
        binding.buttonToggleStatus.isEnabled = true
        binding.countdownStartButton.isEnabled = true
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
                if(isWorking){
                    updateRemainingTime()
                }

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
                if(isWorking){
                    updateRemainingTime()
                }
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
                val intent = Intent("com.example.caroilcalculator.ALARM_ACTION")
                intent.putExtra("remind_time", waterReminderMill)
                requireContext().sendBroadcast(intent)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }
    private fun toggleStatus() {
        if(onCount==false){
            currentState = (currentState + 1) % 3
            updateStatus()
        }

    }
    private fun updateStatus() {
        when (currentState) {
            0 -> {
                binding.countdownStartButton.isEnabled = true
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
                binding.countdownStartButton.isEnabled = false
            }
            2 -> {
                binding.countdownStartButton.isEnabled = true
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


    private fun launchService() {

        val intent = Intent(requireContext(), CountdownService::class.java).apply {
            putExtra("countdown_time", remainingTimeMillis)
        }
        startForegroundService(requireContext(), intent)
    }

    private fun updateRemainingTime() {

       // remainingTimeMillis = intent?.getLongExtra("countdown_time", 0) ?: 0
        val minutes = (remainingTimeMillis / 1000) / 60
        val seconds = (remainingTimeMillis / 1000) % 60
        binding.countdown.text = String.format("%02d:%02d", minutes, seconds)
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