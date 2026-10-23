package br.com.tecpontes.appfinanceiro.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.ui.dashboard.DashboardActivity;
import br.com.tecpontes.appfinanceiro.ui.menu.MenuActivity;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsActivity;
import br.com.tecpontes.appfinanceiro.ui.cards.CardsActivity;
import br.com.tecpontes.appfinanceiro.ui.accounts.AccountsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Liga a barra inferior às telas principais do aplicativo. */
public final class BottomNavigationHelper {

    private BottomNavigationHelper() {}

    /** Mantém o conteúdo sob as barras do sistema e encaixa a barra inferior na navegação do SO. */
    public static void applyEdgeToEdgeInsets(View root) {
        BottomNavigationView bottomNavigation = root.findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) return;

        final int originalBottomPadding = bottomNavigation.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            bottomNavigation.setPadding(
                    bottomNavigation.getPaddingLeft(),
                    bottomNavigation.getPaddingTop(),
                    bottomNavigation.getPaddingRight(),
                    Math.max(originalBottomPadding, systemBars.bottom)
            );
            return windowInsets;
        });
    }

    public static void setup(Activity activity, @IdRes int selectedItemId) {
        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(selectedItemId);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Class<?> destination = rootDestinationFor(itemId);
            if (destination == null) return false;

            // Em telas secundárias (Perfil, Configurações, Notificações, Histórico, Importar
            // extrato...), tocar na aba já selecionada volta para a raiz daquela aba. O
            // CLEAR_TOP reaproveita a instância existente e preserva o estado da tela raiz.
            if (itemId == selectedItemId && destination.isInstance(activity)) return true;

            Intent intent = new Intent(activity, destination);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            return true;
        });
    }

    /** Tela raiz associada a cada item da barra inferior. */
    @Nullable
    private static Class<?> rootDestinationFor(@IdRes int itemId) {
        if (itemId == R.id.navigation_dashboard) return DashboardActivity.class;
        if (itemId == R.id.navigation_transactions) return TransactionsActivity.class;
        if (itemId == R.id.navigation_cards) return CardsActivity.class;
        if (itemId == R.id.navigation_accounts) return AccountsActivity.class;
        if (itemId == R.id.navigation_menu) return MenuActivity.class;
        return null;
    }
}
