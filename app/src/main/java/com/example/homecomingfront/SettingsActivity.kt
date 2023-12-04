package com.example.homecomingfront
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import android.content.Context
import android.view.View

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        val threatAlarmSwitch: SwitchCompat = findViewById(R.id.threatAlarmSwitch) // 또는 SwitchMaterial
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        threatAlarmSwitch.isChecked = sharedPref.getBoolean("ThreatAlarmEnabled", false)


        threatAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPref.edit()) {
                putBoolean("ThreatAlarmEnabled", isChecked)
                apply()
            }

            if (isChecked) {
                Toast.makeText(this, "위협 알림 활성화됨", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "위협 알림 비활성화됨", Toast.LENGTH_SHORT).show()
            }
        }



        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 보호자 등록 버튼 설정
        val guardianRegisterButton: Button = findViewById(R.id.guardianRegisterButton)
        guardianRegisterButton.setOnClickListener {
            // GaurdianRegisterActivity를 시작합니다.
            val intent = Intent(this, GuardianRegisterActivity::class.java)
            startActivity(intent)
        }
    }

    fun handleBackClick(view: View) {
        // MainActivity를 시작합니다.
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // (선택 사항) 현재 액티비티를 종료합니다.
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        }
    }



}
