package com.example.homecomingfront

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import android.content.Context
import android.view.View


class GuardianRegisterActivity : AppCompatActivity() {
    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val contactUri: Uri = result.data?.data ?: return@registerForActivityResult
            val projection: Array<String> = arrayOf(Phone.NUMBER)

            contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(Phone.NUMBER)
                    val number = cursor.getString(numberIndex)

                    // 연락처 번호를 SharedPreferences에 저장
                    val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("GuardianPhoneNumber", number)
                        apply()
                    }

                    // 확인을 위한 Toast 메시지
                    Toast.makeText(this, "Selected contact number: $number", Toast.LENGTH_LONG).show()
                }
                else if(result.resultCode == AppCompatActivity.RESULT_CANCELED) {
                    // 사용자가 뒤로가기 버튼을 누르거나 연락처 선택을 취소했을 때
                    Toast.makeText(this, "연락처 선택 취소됨", Toast.LENGTH_SHORT).show()

                }
                finish()
            }

            // 결과 처리 후 SettingsActivity로 이동
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_register) // 올바른 레이아웃을 지정합니다.


        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // 현재 액티비티 종료
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        pickContact() // 액티비티 시작 시 연락처 선택 화면으로 바로 이동
    }

    private fun pickContact() {
        val pickContactIntent = Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"))
        pickContactIntent.type = Phone.CONTENT_TYPE // 연락처만 표시
        pickContactLauncher.launch(pickContactIntent) // ActivityResultLauncher 사용
    }

}
