package br.com.tecpontes.appfinanceiro.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.ui.dashboard.DashboardActivity;
import br.com.tecpontes.appfinanceiro.ui.menu.FeatureDestinationActivity;
import br.com.tecpontes.appfinanceiro.ui.menu.MenuActivity;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsActivity;
import br.com.tecpontes.appfinanceiro.ui.cards.CardsActivity;
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
            if (itemId == selectedItemId) return true;

            Intent intent;
            if (itemId == R.id.navigation_dashboard) {
                intent = new Intent(activity, DashboardActivity.class);
            } else if (itemId == R.id.navigation_transactions) {
                intent = new Intent(activity, TransactionsActivity.class);
            } else if (itemId == R.id.navigation_menu) {
                intent = new Intent(activity, MenuActivity.class);
            } else if (itemId == R.id.navigation_cards) {
                intent = new Intent(activity, CardsActivity.class);
            } else if (itemId == R.id.navigation_accounts) {
                intent = FeatureDestinationActivity.createIntent(
                        activity, FeatureDestinationActivity.ACCOUNTS);
            } else {
                return false;
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            return true;
        });
    }
}
