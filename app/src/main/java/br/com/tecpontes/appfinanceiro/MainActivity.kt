package br.com.tecpontes.appfinanceiro

import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.tecpontes.appfinanceiro.ui.auth.LoginActivity
import br.com.tecpontes.appfinanceiro.ui.dashboard.DashboardActivity
import br.com.tecpontes.appfinanceiro.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity principal da aplicação - ponto de entrada
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity iniciada")

        // Configura window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navega para tela apropriada baseado no estado de autenticação
        navigateToAppropriateScreen()
    }

    /**
     * Navega para tela apropriada baseado no estado de autenticação
     */
    private fun navigateToAppropriateScreen() {
        Log.d(TAG, "Verificando estado de autenticação")

        if (tokenManager.isLoggedIn()) {
            Log.d(TAG, "Usuário logado - navegando para Dashboard")
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            Log.d(TAG, "Usuário não logado - navegando para Login")
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destruída")
    }
}
