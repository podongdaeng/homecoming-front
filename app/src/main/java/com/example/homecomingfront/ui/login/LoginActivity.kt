package com.example.homecomingfront.ui.login

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import com.example.homecomingfront.databinding.ActivityLoginBinding
import android.text.InputFilter
import android.text.Spanned


import com.example.homecomingfront.R



class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading
        val birthDateEditText: EditText = findViewById(R.id.birthDate)
        birthDateEditText.addTextChangedListener(createBirthDateTextWatcher(birthDateEditText))
        birthDateEditText.filters = arrayOf(DateInputFilter())



        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(username.text.toString(), password.text.toString())
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
fun createBirthDateTextWatcher(editText: EditText): TextWatcher {
    return object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            // 로직이 필요한 경우 여기에 추가
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // 로직이 필요한 경우 여기에 추가
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            s?.let {
                if (it.length == 4 || it.length == 7) {
                    if (it[it.length - 1] != '-') {
                        editText.setText("$it-")
                        editText.setSelection(editText.text.length)
                    }
                }

                if (it.length > 7) {
                    val month = it.substring(5, 7).toIntOrNull()
                    if (month != null) {
                        if (month < 1 || month > 12) {
                            editText.error = "월은 1에서 12 사이의 값이어야 합니다."
                        } else {
                            val maxDay = when (month) {
                                1, 3, 5, 7, 8, 10, 12 -> 31
                                4, 6, 9, 11 -> 30
                                2 -> if (it.substring(0, 4).toInt() % 4 == 0 &&
                                    (it.substring(0, 4).toInt() % 100 != 0 ||
                                            it.substring(0, 4).toInt() % 400 == 0)) 29 else 28
                                else -> 0
                            }

                            if (it.length > 9) {
                                val day = it.substring(8, 10).toIntOrNull()
                                if (day == null || day < 1 || day > maxDay) {
                                    editText.error = "날짜가 유효하지 않습니다."
                                }
                            }
                        }
                    }
                }
                if (it.length > 10 && before == 0) {
                    editText.error = "더 이상 입력할 수 없습니다."
                }
            }
        }
    }
}

class DateInputFilter : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val resultText = dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length)

        // Check against the pattern
        if (!resultText.matches(Regex("^\\d{0,4}(-\\d{0,2}){0,2}$"))) {
            return ""
        }

        val sections = resultText.split("-")

        // Check for the month and day sections length
        for (i in 1..2) {
            if (sections.size > i && sections[i].length > 2) {
                return ""
            }
        }

        // Allow entering day after month has been entered correctly
        if (sections.size > 1 && sections[1].length == 2 && dstart == 7) {
            // Allow entering the first digit of the day
            return null
        }

        // Prevent entering more digits if the date is already complete
        if (resultText.matches(Regex("^\\d{4}-\\d{2}-\\d{3}$"))) {
            // If trying to enter more characters, prevent it
            if (source.isNotEmpty()) {
                return ""
            }
        }

        return null // Allow the change to go through
    }
}
