# Design System — Appfinanceiro

Documentação da fundação visual "Modern Tech & Sofisticado". Serve como referência para as Fases 2 e 3 da refatoração.

- **Última atualização:** 2026-10-24.
- **Fonte de verdade:** `res/values/colors.xml`, `res/values-night/colors.xml`, `res/values/themes.xml`, `res/values-night/themes.xml` e `res/values/styles.xml`.

## Princípios

1. Componentes usam **atributos do tema** (`?attr/...`), nunca cores fixas.
2. O modo escuro é automático, seguindo a configuração do sistema Android.
3. Superfícies são sólidas por padrão; translucidez só quando houver justificativa visual.
4. CTA principal usa o token esmeralda (`colorTertiary`).
5. Aliases legados foram removidos após a migração da Fase 2.

## Tokens de cor

### Marca e CTA

| Token | Claro | Escuro | Uso |
| :--- | :--- | :--- | :--- |
| `brand_primary` | `#4F46E5` | `#818CF8` | Ação primária, ícones e destaques de marca. |
| `brand_primary_dark` | `#4338CA` | `#6366F1` | Variação de pressão/ênfase da marca. |
| `brand_secondary` | `#0EA5E9` | `#38BDF8` | Acentos secundários e dados informativos. |
| `on_brand` | `#FFFFFF` | `#FFFFFF` | Texto/ícone sobre cor de marca. |
| `cta_primary` | `#047857` | `#34D399` | Botão de ação principal (esmeralda). |
| `on_cta` | `#FFFFFF` | `#052E1B` | Texto/ícone sobre o CTA. |

### Fundo, superfície e texto

| Token | Claro | Escuro | Uso |
| :--- | :--- | :--- | :--- |
| `background` | `#F6F7FB` | `#0B1020` | Fundo raiz das telas. |
| `surface` | `#FFFFFF` | `#172033` | Cards, diálogos, toolbars e bottom navigation. |
| `surface_variant` | `#EEF1F8` | `#1F2A3D` | Superfícies secundárias, chips e containers. |
| `surface_elevated` | `#FFFFFF` | `#1A2438` | Superfícies elevadas (menus, sheets). |
| `on_background` | `#0F172A` | `#F1F5F9` | Texto sobre o fundo. |
| `on_surface` | `#0F172A` | `#F1F5F9` | Texto sobre superfícies. |
| `on_surface_variant` | `#64748B` | `#94A3B8` | Texto secundário, hints e legendas. |

### Bordas, erro e finanças

| Token | Claro | Escuro | Uso |
| :--- | :--- | :--- | :--- |
| `outline` | `#E2E8F0` | `#2D3A52` | Bordas, divisores e contorno de cards. |
| `outline_strong` | `#CBD5E1` | `#3E4E6B` | Bordas com maior contraste. |
| `error` | `#DC2626` | `#F87171` | Erros e ações destrutivas. |
| `on_error` | `#FFFFFF` | `#2A0B0B` | Texto sobre erro. |
| `money_positive` | `#047857` | `#34D399` | Valores positivos/receitas. |
| `money_negative` | `#DC2626` | `#F87171` | Valores negativos/despesas. |
| `money_neutral` | `#64748B` | `#94A3B8` | Valores neutros. |
| `scrim` | `#33000000` | `#66000000` | Overlays e fundos de modal. |

### Estados

| Token | Claro | Escuro | Uso |
| :--- | :--- | :--- | :--- |
| `surface_brand` | `#F0ECFF` | `#241F45` | Cards de destaque de marca (ex.: conciliação pendente). |
| `success_container` | `#D1FAE5` | `#0F3D2E` | Fundo de badges de sucesso/conciliado. |
| `on_success_container` | `#047857` | `#6EE7B7` | Texto/ícone sobre sucesso. |
| `neutral_container` | `#F0F2F5` | `#1E293B` | Fundo de badges neutros/pendentes. |
| `on_neutral_container` | `#64748B` | `#CBD5E1` | Texto/ícone sobre neutro. |
| `error_container` | `#FEE2E2` | `#3F1D1D` | Fundo de badges de erro/inativo. |
| `on_error_container` | `#B91C1C` | `#FCA5A5` | Texto/ícone sobre erro. |

> Contraste validado: `money_positive` e `money_negative` atingem pelo menos 4,5:1 sobre `surface` no tema claro; `cta_primary` com `on_cta` atinge 5,48:1 (claro) e 5,86:1 (escuro).

## Tema

- Tema base: `Theme.Appfinanceiro.Base`, herdando `Theme.Material3.DayNight.NoActionBar`.
- `Theme.Appfinanceiro` (claro): ícones de barra escuros (`windowLightStatusBar`/`windowLightNavigationBar = true`).
- `Theme.Appfinanceiro` noturno (`values-night/themes.xml`): ícones de barra claros e superfícies dos tokens noturnos.
- Barras do sistema: status bar transparente; navigation bar usa `colorSurface`.
- `Theme.Appfinanceiro.Dark` foi removido; o DayNight cobre claro/escuro.

## Estilos base

| Estilo | Base | Uso |
| :--- | :--- | :--- |
| `ModernCard` | `Widget.Material3.CardView.Filled` | Card moderno: superfície sólida, raio 20dp, borda e elevação 2dp. |
| `ModernListCard` | `ModernCard` | Variação para itens de lista: raio 16dp e sem elevação. |
| `PrimaryCTAButton` | `Widget.Material3.Button` | Ação principal; fundo `colorTertiary`, altura mínima 52dp, raio 14dp. |
| `BrandButton` | `Widget.Material3.Button` | Ação de marca; fundo `colorPrimary`, altura mínima 52dp. |
| `ModernTextInput` | `Widget.Material3.TextInputLayout.OutlinedBox` | Campo arredondado (14dp) com fundo `surfaceVariant`, contorno do tema e hint em `colorOnSurfaceVariant`. |
| `SettingsCard` | — | Card de configurações: superfície, borda e raio 16dp. |
| `GlassCard` / `GlassButton` / `GlassTextInput` | legado | Rebaseados nos tokens; mantidos durante a transição. |
| `SettingsSectionTitle` / `SettingsLabel` / `SettingsHelper` / `SettingsIcon` | — | Textos e ícones usando `?attr/colorOnSurface*` e `?attr/colorPrimary`. |

## Drawables

| Drawable | Uso |
| :--- | :--- |
| `bg_modern_card.xml` | Fundo de card moderno com `?attr/colorSurface` e `?attr/colorOutline`. |
| `bg_cta_button.xml` | CTA esmeralda com ripple (`?attr/colorTertiary`). |
| `bg_login_logo.xml` | Badge circular com gradiente de marca para o Login. |
| `bg_icon_circle.xml` | Círculo `surfaceVariant` para ícones de itens de lista. |
| `bg_period_selector.xml` | Seletor de período com `surfaceVariant` e contorno do tema. |
| `bg_outlined_rounded.xml` | Contêiner arredondado com superfície e contorno do tema. |
| `bg_dialog.xml` | Fundo de diálogo (raio 20dp) com superfície e contorno. |
| `glass_card_background.xml` / `glass_button_background.xml` | Versões legadas rebaseadas nos tokens. |

## Regras de uso

### Cores

- Em layouts XML, usar `?attr/colorPrimary`, `?attr/colorSurface`, `?attr/colorOnSurface`, `?attr/colorOutline`, etc.
- Em código Java/Kotlin, obter cores via `MaterialColors.getColor(view, attr)` ou `ContextCompat.getColor(context, R.color.<token>)`.
- Não usar `@android:color/white`, `@android:color/black`, `#FFFFFF` ou `#000000` em layouts.
- Valores financeiros usam `R.color.money_positive` / `money_negative` / `money_neutral`.
- A máscara de privacidade é apenas visual; exportações CSV/PDF mantêm valores reais (D-05).

### Componentes

- Preferir `ModernCard`, `PrimaryCTAButton`, `BrandButton` e `ModernTextInput` em telas novas ou redesenhadas.
- Botões em `MaterialButton` devem usar `backgroundTint` com atributos do tema; evitar `android:background` em botões Material.
- Campos de formulário usam `ModernTextInput`; o hint deve usar `?attr/colorOnSurfaceVariant`.
- Ícones interativos devem ter `contentDescription`.

### Migração

1. Substituir cor fixa pelo token/atributo correspondente.
2. Validar a tela nos temas claro e escuro.
3. Remover imports de cores legadas quando o layout deixar de usá-las.
4. Aliases legados foram removidos de `colors.xml` após a migração da Fase 2; não reintroduzir cores fixas.

## Referências

- Plano completo: `docs/plano-refatoracao-visual.md`.
- Tokens: `app/src/main/res/values/colors.xml` e `app/src/main/res/values-night/colors.xml`.
- Temas: `app/src/main/res/values/themes.xml` e `app/src/main/res/values-night/themes.xml`.
- Estilos: `app/src/main/res/values/styles.xml`.
