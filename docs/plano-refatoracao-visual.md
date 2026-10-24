# Plano de Refatoração Visual — Modern Tech & Sofisticado

Documento vivo para acompanhar a modernização visual do app, com etapas, critérios de aceite e conclusões.

- **Status geral:** Fase 3 em andamento; Login, Dashboard, Transações, Criar/editar lançamento, Contas, Importação de extrato, Cartões, Relatórios, Configurações e telas secundárias implementados.
- **Última atualização:** 2026-10-24.
- **Próximo passo:** `F3-11` (Barra inferior e toolbars).
- **Responsável:** equipe de desenvolvimento Android.

## Como atualizar este documento

1. Atualizar o campo **Status** do passo antes de iniciar e ao concluir.
2. Preencher a coluna **Conclusão** com evidências objetivas: arquivos alterados, decisões, validações executadas e pendências.
3. Atualizar a tabela **Visão geral** sempre que uma fase mudar de status.
4. Adicionar uma linha no **Registro de execução** ao final de cada etapa relevante.
5. Nunca marcar um passo como concluído sem executar a validação correspondente.
6. Manter o **Próximo passo** apontando para o primeiro item pendente.

## Legenda de status

| Status | Significado |
| :--- | :--- |
| Pendente | Ainda não iniciado. |
| Em andamento | Iniciado, sem conclusão validada. |
| Concluído | Implementado e validado. |
| Bloqueado | Dependência externa ou decisão pendente. |
| Cancelado | Não será executado nesta refatoração. |

## Visão geral

| Fase | Objetivo | Status | Progresso |
| :--- | :--- | :--- | :--- |
| Fase 0 | Diagnóstico técnico e definição do plano | Concluído | 100% |
| Fase 1 | Fundação visual: tokens, temas, estilos e drawables | Concluído | 100% |
| Fase 2 | Migração global de cores fixas e preparação do dark mode | Concluído | 100% |
| Fase 3 | Redesenho estrutural das telas | Em andamento | 91% |
| Fase 4 | Validação final, dark mode e QA | Pendente | 0% |

## Decisões consolidadas

| ID | Decisão | Justificativa |
| :--- | :--- | :--- |
| D-01 | Iniciar pela fundação visual antes de redesenhar telas | Reduz retrabalho e evita inconsistência entre componentes. |
| D-02 | Dark mode automático, seguindo o sistema Android | Decisão do produto; evita configuração manual. |
| D-03 | Manter XML Views como base principal | O app usa XML Views; o tema Compose não altera essas telas. |
| D-04 | Manter aliases temporários de cores antigas | Evita quebrar dezenas de layouts durante a transição. |
| D-05 | Exportações de relatórios não seguem a máscara de privacidade | A máscara é apenas visual; arquivos devem conter valores reais. |
| D-06 | Telas com cores fixas serão migradas na Fase 2 | A fundação sozinha não garante dark mode completo. |

## Fase 0 — Diagnóstico e planejamento

| ID | Passo | Status | Conclusão |
| :--- | :--- | :--- | :--- |
| F0-01 | Mapear estrutura de temas e recursos atuais | Concluído | App usa `values/themes.xml`, sem `values-night`. |
| F0-02 | Identificar estratégia de UI | Concluído | Predominância de XML Views; Compose presente apenas no tema. |
| F0-03 | Levantar bloqueios para dark mode | Concluído | Muitas cores fixas (`@android:color/white`, `@android:color/black`, `glass_*`) nos layouts. |
| F0-04 | Definir fases e critérios de validação | Concluído | Plano documentado neste arquivo. |

**Conclusões da Fase 0**

- O visual atual é baseado em glassmorphism com branco, roxo e azul.
- `Theme.Appfinanceiro.Dark` existe, mas não é aplicado e não há recursos noturnos.
- Componentes compartilhados (`GlassCard`, `GlassButton`, `GlassTextInput`, `bg_outlined_rounded`, `bg_dialog`) concentram parte importante do visual.
- A modernização exige primeiro tokens/temas, depois migração de cores e, por último, redesenho das telas.

## Fase 1 — Fundação visual

Objetivo: criar a base de design system sem redesenhar a estrutura das telas.

| ID | Passo | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F1-01 | Criar tokens de cor do tema claro | `res/values/colors.xml` | Tokens de marca, CTA, fundo, superfície, texto, borda e finanças definidos. | Concluído | Tokens semânticos criados (`brand_*`, `cta_primary`, `background`, `surface*`, `on_*`, `outline*`, `money_*`, `error`) com aliases legados. Cores de marca movidas de `themes.xml`. |
| F1-02 | Criar variantes noturnas dos tokens | `res/values-night/colors.xml` | Todos os tokens usados no tema claro possuem equivalente noturno. | Concluído | `values-night/colors.xml` criado com equivalentes de todos os tokens semânticos; aliases legados acompanham o tema automaticamente. |
| F1-03 | Atualizar tema claro para Material 3 DayNight | `res/values/themes.xml` | `Theme.Appfinanceiro` usa `Theme.Material3.DayNight.NoActionBar` e tokens semânticos. | Concluído | `Theme.Appfinanceiro.Base` herda `Theme.Material3.DayNight.NoActionBar`; `Theme.Appfinanceiro` usa tokens semânticos e variante clara dos ícones das barras. `Theme.Appfinanceiro.Dark` removido (DayNight substitui). |
| F1-04 | Criar tema noturno | `res/values-night/themes.xml` | Barras, ícones e superfícies corretos no modo escuro. | Concluído | `values-night/themes.xml` sobrescreve `Theme.Appfinanceiro` com `windowLightStatusBar=false` e `windowLightNavigationBar=false`; cores e superfícies resolvidas pelos tokens noturnos. |
| F1-05 | Criar estilos base modernos | `res/values/styles.xml` | `ModernCard`, `PrimaryCTAButton`, `BrandButton` e `ModernTextInput` definidos. | Concluído | Estilos criados com atributos do tema: card de superfície sólida, CTA esmeralda (`colorTertiary`), botão de marca e text input arredondado. |
| F1-06 | Rebasear estilos legados | `res/values/styles.xml` | `GlassCard`, `GlassButton`, `GlassTextInput` e `SettingsCard` usam tokens do tema. | Concluído | Estilos legados migrados para `?attr/colorSurface`, `?attr/colorPrimary`, `?attr/colorOutline` e `?attr/colorOnSurface*`; `Settings*` sem preto/cinza fixo. |
| F1-07 | Atualizar drawables compartilhados | `res/drawable/glass_*.xml`, `bg_outlined_rounded.xml`, `bg_dialog.xml` | Formas usam `?attr/colorSurface` e `?attr/colorOutline`; sem branco fixo. | Concluído | Drawables rebaseados em `?attr/colorSurface`, `?attr/colorOutline` e `?attr/colorPrimary`; verificado que não restou branco/preto fixo. |
| F1-08 | Criar drawables modernos | `res/drawable/bg_modern_card.xml`, `res/drawable/bg_cta_button.xml` | Componentes podem usar superfície sólida e CTA esmeralda com ripple. | Concluído | `bg_modern_card.xml` (superfície + contorno) e `bg_cta_button.xml` (ripple com `?attr/colorTertiary`) criados. |
| F1-09 | Atualizar navegação inferior | `layout/bottom_navigation.xml`, `res/color/navigation_item_tint.xml` | Fundo e tint seguem o tema claro/escuro. | Concluído | Fundo migrado para `?attr/colorSurface`; tint usa `?attr/colorPrimary` (selecionado) e `?attr/colorOnSurfaceVariant`. |
| F1-10 | Sincronizar tema Compose | `ui/theme/Color.kt`, `ui/theme/Theme.kt` | Tokens equivalentes, sem erro de import e sem dynamic color. | Concluído | Tokens claro/escuro espelhados em `Color.kt`; `Theme.kt` usa esquemas estáticos e `dynamicColor` removido. |
| F1-11 | Consolidar documentação de design system | `docs/design-system.md` | Tokens, estilos, regras de uso e migração documentados. | Concluído | `docs/design-system.md` criado com tabelas de tokens, tema, estilos, drawables, regras e migração. |
| F1-12 | Validar fundação | Build e revisão de recursos | `./gradlew testDebugUnitTest assembleDebug` e `git diff --check` sem erros. | Concluído | Build `BUILD SUCCESSFUL`; `git diff --check` sem erros; drawables compartilhados sem branco/preto fixo. |

**Critérios de saída da Fase 1**

- Tokens e temas claro/escuro compilando.
- Componentes base usando atributos do tema.
- Nenhum drawable compartilhado com cor branca fixa.
- App atual funcionando no tema claro.
- Dark mode ainda pode estar parcial nas telas com cores fixas.

## Fase 2 — Migração global de cores e dark mode

Objetivo: remover cores fixas dos layouts para que o dark mode funcione em todas as telas, sem ainda redesenhar a estrutura.

| ID | Passo | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F2-01 | Migrar fundos raiz das telas | `res/layout/activity_*.xml` | `@android:color/white` substituído por `?attr/colorBackground`. | Concluído | 14 raízes migradas para `?android:attr/colorBackground`; sem branco fixo nos layouts. |
| F2-02 | Migrar textos e ícones fixos | Layouts e `res/color/` | `@android:color/black` substituído por `?attr/colorOnSurface` ou `?attr/colorOnSurfaceVariant`. | Concluído | `@android:color/black`, `darker_gray`, `primary_purple` e `primary_blue` substituídos por atributos do tema nos layouts e color state lists. |
| F2-03 | Migrar superfícies, cards e toolbars | Layouts e estilos | Cards/toolbars usam `?attr/colorSurface` e `?attr/colorOutline`. | Concluído | Toolbars, cards e itens migrados para `?attr/colorSurface`; bordas/divisores para `?attr/colorOutline` ou `@color/outline`. |
| F2-04 | Migrar diálogos | `res/layout/dialog_*.xml` | Diálogos claros/escuros com superfície, texto e borda corretos. | Concluído | Diálogos usam `bg_dialog`, `?attr/colorSurface`, textos e divisores do tema. |
| F2-05 | Migrar itens de listas e adapters | `res/layout/item_*.xml` e adapters | Itens usam cores do tema; sem branco/preto fixo. | Concluído | Itens e adapters migrados; badges usam `success_container`/`neutral_container`; textos dinâmicos usam tokens. |
| F2-06 | Migrar gráficos | `MonthlyEvolutionChartView`, `CategoryPieChartView` | Grade, rótulos e fundos legíveis em claro/escuro. | Concluído | Gráficos resolvem receitas, despesas, grade, rótulos e estado vazio via tokens do tema. |
| F2-07 | Revisar estados de carregamento/vazio/erro | Layouts e componentes | Estados usam superfícies e textos do tema. | Concluído | `loading_view`, vazios, erros e badges migrados; aliases legados removidos de `colors.xml`. |
| F2-08 | Validar dark mode global | App em claro/escuro | Nenhuma tela com fundo claro e texto claro ou superfícies inconsistentes. | Concluído | Validado visualmente com prints de Dashboard, Transações, Cartões, Contas e Menu em claro e escuro; sem faixas brancas ou textos ilegíveis. |

**Critérios de saída da Fase 2**

- Nenhuma cor fixa crítica restante nos layouts.
- Dark mode automático funcionando em todas as telas e diálogos.
- Contraste mínimo legível em textos e ícones.
- Build e testes passando.

## Fase 3 — Redesenho estrutural das telas

Objetivo: aplicar a identidade Modern Tech & Sofisticado em cada tela, mantendo funcionalidades existentes.

| ID | Tela | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F3-01 | Login | `activity_login.xml`, `LoginActivity.java` | Estrutura moderna, CTA esmeralda, dark mode e estados de erro preservados. | Concluído | Branding em gradiente, `ModernCard`, `ModernTextInput`, CTA esmeralda e dark mode; IDs e estados de erro preservados; build OK. |
| F3-02 | Dashboard | `activity_dashboard.xml`, `DashboardActivity.java` | Cards de saldo/patrimônio/resumo modernos e legíveis nos dois temas. | Concluído | Cards migrados para `ModernCard`, seletor de período estilizado, CTA esmeralda de adicionar transação, botão secundário outlined e itens recentes com `ModernListCard`; build OK. |
| F3-03 | Transações | `activity_transactions.xml`, `TransactionsActivity.java` | Filtros, resumo, lista e FAB alinhados ao novo design. | Concluído | Toolbar e seletores modernizados, resumo em `ModernCard`, filtros tonal, FAB esmeralda e itens com `ModernListCard`; build OK. |
| F3-04 | Criar/editar lançamento | `dialog_create_transaction.xml`, `dialog_edit_transaction.xml`, `CreateTransactionDialog.java`, `TransactionsActivity.java` | Formulário moderno, teclado, máscara de valor e privacidade preservados. | Concluído | Diálogo de criação com `ModernCard`, subtítulo, campos `ModernTextInput`, spinners com `bg_spinner_selector`, labels em sentence case e CTA esmeralda; botão de data alinhado aos campos. A edição de despesas/receitas reutiliza o formulário completo de criação, com tipo, descrição, valor, data, forma de pagamento, conta/cartão, categoria, parcelamento/mensal e observação carregados do lançamento; a edição em série preserva o diálogo de escopo. Clonar abre o mesmo formulário preenchido e cria uma cópia sem vínculo de grupo. Valor e data usam o mesmo `TextInputLayout`; campos com hint têm 56dp para não cortar ascendentes. Transferências e saldo inicial mantêm o editor reduzido porque o endpoint `PUT /transactions/{id}` não expõe os dois lados da transferência. Máscara/regras/IDs mantidos; build, testes e validação visual em claro/escuro OK. |
| F3-05 | Contas | `activity_accounts.xml`, `AccountsActivity.java`, `dialog_account_form.xml`, `dialog_credit_card_form.xml`, `dialog_balance_history.xml` | Cards de conta e cartão modernizados sem perder ações. | Concluído | Tela migrada para `CoordinatorLayout`/`AppBarLayout`, com card de resumo (saldo, limite disponível e faturas pendentes), conciliação em `ModernCard`, controles de “Inativos” e “Recalcular” no cabeçalho, CTAs e estados vazios. Cards com ícones em círculo, badges de status, valores nas cores financeiras, ações em pills e overflow com `contentDescription`. Formulários e histórico em diálogos próprios com `ModernTextInput`, spinners com chevron e CTA esmeralda. Máscara de privacidade e todos os IDs preservados; build, testes, `git diff --check` e validação visual em claro/escuro OK. |
| F3-06 | Importação de extrato | `activity_statement_import.xml`, `StatementImportActivity.java` | Linhas, seleção e resumo legíveis em ambos os temas. | Concluído | Tela migrada para `CoordinatorLayout`/`AppBarLayout` com toolbar e botão de voltar; card da conta, card de importação, resumo em `ModernCard` tonal, CTA esmeralda e estados de vazio/erro. Correspondências e lançamentos novos usam `item_statement_match.xml`/`item_statement_new.xml` com `ModernListCard`, checkbox, valores nas cores financeiras, badge de confiança e spinner de categoria com `bg_spinner_selector`. Insets das barras do sistema aplicados e prévia limpa ao trocar de arquivo. IDs e fluxo de seleção/conciliação preservados; build/testes e `git diff --check` OK. |
| F3-07 | Cartões | `activity_cards.xml`, `CardsActivity.java` | Visão do cartão, fatura e resumo modernizados. | Concluído | Tela migrada para `CoordinatorLayout`/`AppBarLayout` com toolbar, seletor de cartão temático e estado vazio. Visão do cartão em `ModernCard` com limite, progresso e disponibilidade em cores financeiras; fatura em `ModernCard` com seletor de mês em `bg_period_selector`, resumo com título próprio e CTAs esmeralda/outlined; lançamentos e categorias em `ModernCard` com percentuais, marcadores de cor e progresso. Spinners de cartão, período e formulários usam `bg_spinner_selector`/`item_spinner_form`; datas usam `bg_period_selector`. Após validação com prints, o card de categorias ganhou folga inferior e o botão “Novo lançamento” passou a abrir o `CreateTransactionDialog` compartilhado com a forma de pagamento travada no cartão selecionado, permitindo despesa ou crédito de reembolso. IDs e fluxos de fatura, pagamento, edição, estorno e exportação preservados; build/testes e `git diff --check` OK. |
| F3-08 | Relatórios | `activity_reports.xml`, `ReportsActivity.java` | Filtros, cards de resumo e categorias no novo padrão. | Concluído | Tela migrada para `CoordinatorLayout`/`AppBarLayout` com progresso tonal no cabeçalho. Filtros, resumo e ações em `ModernCard`; botões de período/categoria e exportação em estilos outlined/`PrimaryCTAButton`; spinner de tipo com `bg_spinner_selector`. Categorias com marcador de cor, percentual e barra de progresso; itens de despesa e diálogo de detalhamento modernizados. Exportações CSV/PDF e filtros preservados; build/testes e `git diff --check` OK. |
| F3-09 | Configurações | `activity_settings.xml`, `SettingsActivity.java` | Seções, campos e metas alinhados ao design system. | Concluído | Tela migrada para `CoordinatorLayout`/`AppBarLayout` com progresso tonal no cabeçalho. Metas financeiras, DND, retenção e metas por categoria em `ModernCard`, com ícones em círculo e campos `ModernTextInput`; CTAs esmeralda/outlined. Lista de metas usa `bg_period_selector` e diálogo de importação adotou `ModernCard`, spinner temático e campos do design system. Máscaras, validações, DND, importação e salvamento preservados; build/testes e `git diff --check` OK. |
| F3-10 | Menu, categorias, perfil e notificações | Layouts e Activities correspondentes | Telas secundárias consistentes com o restante do app. | Concluído | Menu migrado para `CoordinatorLayout`/`AppBarLayout` com cards `ModernCard` e itens com ícones em círculo; Categorias com seletor Despesas/Receitas em `MaterialButtonToggleGroup` arredondado, FAB reposicionado e diálogo de categoria com `ModernTextInput`/spinners temáticos; Perfil com cards, avatar, campos `ModernTextInput`, ações e zona de perigo; Notificações e Histórico com cards, estados vazios, busca `ModernTextInput` e diálogos de lembrete/configuração modernizados. Fluxos, validações e IDs preservados; build/testes e `git diff --check` OK. |
| F3-11 | Barra inferior e toolbars | `bottom_navigation.xml`, layouts | Navegação e cabeçalhos consistentes em claro/escuro. | Pendente | Aguardando execução. |

**Critérios de saída da Fase 3**

- Todas as telas principais redesenhadas.
- Funcionalidades existentes preservadas.
- Navegação e estados de erro/carregamento consistentes.
- Nenhuma regressão visual evidente em telas pequenas.

## Fase 4 — Validação final e QA

| ID | Passo | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- |
| F4-01 | Executar testes e build | `./gradlew testDebugUnitTest assembleDebug` sem falhas. | Pendente | Aguardando execução. |
| F4-02 | Validar contraste e acessibilidade | Contraste adequado e `contentDescription` nos ícones interativos. | Pendente | Aguardando execução. |
| F4-03 | Validar dark mode automático | Troca do tema do sistema reflete corretamente no app. | Pendente | Aguardando execução. |
| F4-04 | Validar privacidade de valores | Botão de máscara continua funcionando em todas as telas previstas. | Pendente | Aguardando execução. |
| F4-05 | Validar exportações | CSV/PDF continuam com valores reais. | Pendente | Aguardando execução. |
| F4-06 | Atualizar documentação final | Plano e design system refletem o estado entregue. | Pendente | Aguardando execução. |
| F4-07 | Revisar regressões | Fluxos de login, transações, contas, cartões e relatórios funcionando. | Pendente | Aguardando execução. |

## Registro de execução

| Data | ID | Ação realizada | Conclusão / Evidência |
| :--- | :--- | :--- | :--- |
| 2026-10-24 | F0 | Diagnóstico do projeto e elaboração do plano | Estrutura atual mapeada; fases e critérios definidos. Nenhuma alteração de código realizada. |
| 2026-10-24 | F1-01/F1-02 | Tokens de cor claro/escuro e aliases legados criados | `values/colors.xml` e `values-night/colors.xml`; build `./gradlew testDebugUnitTest assembleDebug` OK e `git diff --check` sem erros. |
| 2026-10-24 | F1-03/F1-04 | Tema migrado para Material 3 DayNight e tema noturno criado | `Theme.Appfinanceiro.Base` + variantes claro/escuro; `values-night/themes.xml`; build `./gradlew testDebugUnitTest assembleDebug` OK. |
| 2026-10-24 | F1-05/F1-06 | Estilos modernos criados e legados rebaseados | `ModernCard`, `PrimaryCTAButton`, `BrandButton`, `ModernTextInput` em `styles.xml`; estilos Glass/Settings usando atributos do tema; build OK. |
| 2026-10-24 | F1-07/F1-08 | Drawables compartilhados rebaseados e drawables modernos criados | `glass_*`, `bg_outlined_rounded`, `bg_dialog` sem cor fixa; `bg_modern_card` e `bg_cta_button` criados; build OK. |
| 2026-10-24 | F1-09/F1-10 | Navegação inferior e tema Compose sincronizados | `bottom_navigation` e `navigation_item_tint` com atributos do tema; tokens Compose espelhados e dynamic color removido; build OK. |
| 2026-10-24 | F1-11/F1-12 | Design system documentado e fundação validada | `docs/design-system.md` criado; `./gradlew testDebugUnitTest assembleDebug` OK; `git diff --check` sem erros. Fase 1 concluída. |
| 2026-10-24 | F2-01/F2-02/F2-03 | Fundos, textos, superfícies e toolbars migrados | 14 raízes, 33 layouts e color state lists usando atributos/tokens do tema; aliases legados removidos; build OK. |
| 2026-10-24 | F2-04/F2-05/F2-07 | Diálogos, itens, adapters e estados migrados | Textos dinâmicos, badges, divisores e strokes via tokens; `loading_view` e estados de vazio/erro revisados; build OK. |
| 2026-10-24 | F2-06 | Gráficos tema-aware | `MonthlyEvolutionChartView` e `CategoryPieChartView` resolvem cores do tema; build OK. |
| 2026-10-24 | F2-08 | Validação visual do dark mode concluída | Prints de Dashboard, Transações, Cartões, Contas e Menu em claro/escuro confirmaram legibilidade e consistência; Fase 2 concluída. |
| 2026-10-24 | F3-01 | Login redesenhado e logo atualizado | `activity_login.xml` atualizado com novo vetor de logo `ic_app_logo.xml` (símbolos de cédula, carteira, moedas, gráfico e seta), `ModernCard`, `ModernTextInput`, CTA esmeralda e suporte a dark mode; build OK. |
| 2026-10-24 | F3-02 | Dashboard redesenhado | Cards `ModernCard`, seletor de período com `bg_period_selector` e itens customizados, CTA esmeralda e botão outlined; `ModernListCard` nos itens recentes; build OK. |
| 2026-10-24 | F3-03 | Transações redesenhadas | Toolbar sem `Dark.ActionBar`, seletor de mês em card `surfaceVariant`, contas com botão outlined, filtros tonal, resumo `ModernCard`, FAB e itens em esmeralda/`ModernListCard`; build OK. |
| 2026-10-24 | F3-04 | Diálogo de lançamento redesenhado | Cabeçalho com subtítulo, `ModernCard`, `ModernTextInput`, spinners `bg_period_selector` com `item_spinner_form`, labels em sentence case, erro em `@color/error` e CTA esmeralda; parcelamento com valor à esquerda e nº de parcelas à direita; máscara de valor e IDs preservados; build OK. Pendente validação visual. |
| 2026-10-24 | F3-04 | Ajustes após validação com prints | Diálogo de edição migrado para `dialog_edit_transaction.xml` com o mesmo shell visual; botão de data com fundo/contorno alinhado aos campos; spinners com `bg_spinner_selector` e chevron; subtítulo no cabeçalho do criação. Validação por campo no editar; escopo de série preservado. Build/testes OK. Aguardando novos prints. |
| 2026-10-24 | F3-04 | Edição completa de lançamentos | `CreateTransactionDialog` passou a suportar modo de edição: carrega tipo, descrição, valor, data, forma de pagamento, conta/cartão, categoria, parcelamento/mensal e observação a partir do `TransactionListItemDto`, incluindo opções inativas. O `PUT` recebe o formulário completo e o escopo `current`/`future` é aplicado aos lançamentos em série. Transferências/saldo inicial usam o editor reduzido por limitação da API. Build/testes OK. |
| 2026-10-24 | F3-04 | Ajustes de alinhamento e legibilidade | Valor e data passaram a usar o mesmo `TextInputLayout` (campo de data somente leitura com ícone), alinhando os boxes. Campos com hint subiram para 56dp para evitar corte de ascendentes em maiúsculas; observações receberam `textLocale="pt-BR"` e `textAutoCorrect`, sem filtros de caracteres. Build/testes OK. |
| 2026-10-24 | F3-04 | Clonar com formulário preenchido | O menu “Clonar” passou a abrir o formulário completo de lançamento com tipo, descrição, valor, data, forma de pagamento, conta/cartão, categoria, parcelamento/mensal e observação preenchidos. O botão “Criar” envia um novo lançamento sem `installmentGroupId`/`fixedExpenseGroupId` e sem vínculo de conciliação. Data e demais campos podem ser alterados antes de criar. Build/testes OK. |
| 2026-10-24 | F3-04 | Ajuste do seletor de parcelas | O campo “Número de parcelas” deixou de usar hint flutuante e passou a usar o mesmo visual de 53dp do spinner, com label externo. O seletor de valor foi renomeado para “Por parcela”/“Total” para não quebrar em duas linhas. Build/testes OK. |
| 2026-10-24 | F3-05 | Contas redesenhadas | `activity_accounts.xml` com AppBar, card de resumo, conciliação, filtros e estados vazios; cards programáticos com `ModernListCard`, ícones em círculo, badges e ações acessíveis; formulários de conta/cartão e histórico em diálogos próprios. Máscara de privacidade e IDs preservados; `./gradlew testDebugUnitTest assembleDebug` OK; `git diff --check` sem erros. Pendente validação visual. |
| 2026-10-24 | F3-05 | Controles de inativos e recálculo no cabeçalho | `showInactiveCheckBox` (“Inativos”) e `recalculateButton` (ícone de sincronização) foram movidos para o `Toolbar` de Contas. A linha de filtros do corpo foi removida; o resumo e a conciliação subiram. IDs e listeners Java preservados; `ImageButton` mantém `contentDescription` e `tooltipText`. Build OK. |
| 2026-10-24 | F3-04/F3-05 | Fechamento das pendências de validação | Validação visual em claro/escuro concluída e itens marcados como `Concluído` no plano. |
| 2026-10-24 | F3-06 | Importação de extrato redesenhada | `activity_statement_import.xml` com AppBar, card da conta, card de importação, resumo tonal e CTA esmeralda; `item_statement_match.xml` e `item_statement_new.xml` criados com `ModernListCard`, badge de confiança e spinner de categoria; insets de barras do sistema e limpeza da prévia ao trocar de arquivo; IDs e fluxo de conciliação preservados; `./gradlew testDebugUnitTest assembleDebug` OK e `git diff --check` sem erros. |
| 2026-10-24 | F3-07 | Cartões redesenhados | `activity_cards.xml` migrado para `CoordinatorLayout`/`AppBarLayout`; visão do cartão e fatura em `ModernCard`; seletor de mês em `bg_period_selector`; CTAs esmeralda/outlined; spinners com `bg_spinner_selector`; resumo da fatura com título, categorias com percentual e marcador de cor. IDs e fluxos de pagamento/estorno/exportação preservados; `./gradlew testDebugUnitTest assembleDebug` OK e `git diff --check` sem erros. |
| 2026-10-24 | F3-07 | Ajustes após prints de validação | Card “Despesas por categoria” recebeu margem inferior e padding extra para não encostar na navegação; “Novo lançamento” passou a abrir o `CreateTransactionDialog` compartilhado com forma de pagamento travada no cartão selecionado. O modal mantém as opções de despesa e receita, oculta transferência e permite lançar créditos de reembolso. `CreateTransactionDialog` ganhou `presetCreditCard` e validação de opções por forma de pagamento. Build/testes OK. |
| 2026-10-24 | F3-08 | Relatórios redesenhados | `activity_reports.xml` migrado para `CoordinatorLayout`/`AppBarLayout`; filtros, resumo, categorias e ações em `ModernCard`; período/categoria e exportações com estilos outlined/`PrimaryCTAButton`; categorias com percentual e progresso; `item_report_category`, `item_report_expense` e `dialog_report_expenses` modernizados. Filtros, exportações CSV/PDF e privacidade preservados; `./gradlew testDebugUnitTest assembleDebug` OK e `git diff --check` sem erros. |
| 2026-10-24 | F3-09 | Configurações redesenhadas | `activity_settings.xml` migrado para `CoordinatorLayout`/`AppBarLayout`; metas financeiras, DND, retenção e metas por categoria em `ModernCard`; campos `ModernTextInput`, ícones em círculo e CTAs esmeralda/outlined; `item_category_goal` e `dialog_import_goals` modernizados com spinner temático. Validações, DND, importação e salvamento preservados; `./gradlew testDebugUnitTest assembleDebug` OK e `git diff --check` sem erros. |
| 2026-10-24 | F3-10 | Categorias ajustadas após print | Seletor Despesas/Receitas substituído por `MaterialButtonToggleGroup` arredondado, alinhado aos seletores de tipo de lançamento; FAB movido para 4dp da barra inferior e lista com padding para não cobrir o último item. Build/testes OK. Demais telas do F3-10 pendentes. |
| 2026-10-24 | F3-10 | Telas secundárias redesenhadas | `activity_menu`/`item_menu_option`, `activity_profile`, `activity_notifications`, `activity_notification_history`, `item_notification_config`, `item_notification_log`, `dialog_category`, `dialog_create_reminder` e `dialog_edit_message_config` migrados para `CoordinatorLayout`/`AppBarLayout`, `ModernCard`, ícones em círculo, `ModernTextInput` e spinners temáticos; adapters de Categoria/Notificações atualizados. Fluxos e IDs preservados; `./gradlew testDebugUnitTest assembleDebug` OK e `git diff --check` sem erros. F3-10 concluído. |

## Comandos de validação

```bash
./gradlew testDebugUnitTest assembleDebug
git diff --check
```

## Arquivos-chave da refatoração

- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values-night/colors.xml` *(novo)*
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml` *(novo)*
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/drawable/glass_card_background.xml`
- `app/src/main/res/drawable/glass_button_background.xml`
- `app/src/main/res/drawable/bg_outlined_rounded.xml`
- `app/src/main/res/drawable/bg_dialog.xml`
- `app/src/main/res/drawable/bg_spinner_selector.xml`
- `app/src/main/res/drawable/bg_text_action.xml`
- `app/src/main/res/drawable/bg_status_badge_error.xml`
- `app/src/main/res/drawable/bg_status_badge_success.xml`
- `app/src/main/res/drawable/bg_status_badge_neutral.xml`
- `app/src/main/res/drawable/ic_chevron_down.xml`
- `app/src/main/res/color/navigation_item_tint.xml`
- `app/src/main/res/layout/bottom_navigation.xml`
- `app/src/main/res/layout/activity_statement_import.xml`
- `app/src/main/res/layout/item_statement_match.xml`
- `app/src/main/res/layout/item_statement_new.xml`
- `app/src/main/res/layout/activity_reports.xml`
- `app/src/main/res/layout/item_report_category.xml`
- `app/src/main/res/layout/item_report_expense.xml`
- `app/src/main/res/layout/dialog_report_expenses.xml`
- `app/src/main/res/layout/activity_settings.xml`
- `app/src/main/res/layout/item_category_goal.xml`
- `app/src/main/res/layout/dialog_import_goals.xml`
- `app/src/main/res/layout/activity_menu.xml`
- `app/src/main/res/layout/item_menu_option.xml`
- `app/src/main/res/layout/activity_categories.xml`
- `app/src/main/res/layout/item_category.xml`
- `app/src/main/res/layout/dialog_category.xml`
- `app/src/main/res/layout/activity_profile.xml`
- `app/src/main/res/layout/activity_notifications.xml`
- `app/src/main/res/layout/activity_notification_history.xml`
- `app/src/main/res/layout/item_notification_config.xml`
- `app/src/main/res/layout/item_notification_log.xml`
- `app/src/main/res/layout/dialog_create_reminder.xml`
- `app/src/main/res/layout/dialog_edit_message_config.xml`
- `app/src/main/res/layout/dialog_create_transaction.xml`
- `app/src/main/res/layout/dialog_edit_transaction.xml`
- `app/src/main/res/layout/dialog_account_form.xml`
- `app/src/main/res/layout/dialog_credit_card_form.xml`
- `app/src/main/res/layout/dialog_balance_history.xml`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/ui/theme/Color.kt`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/ui/theme/Theme.kt`
- Layouts `activity_*.xml`, `dialog_*.xml` e `item_*.xml`

## Riscos e pontos de atenção

| Risco | Impacto | Mitigação |
| :--- | :--- | :--- |
| Cores fixas em muitos layouts | Dark mode incompleto | Migração sistemática na Fase 2 com checklist por tela. |
| Alias `glass_white_translucent` usado em 47 locais | Mudança visual ampla | Validar cards, toolbars e loading antes de concluir F1. |
| `EdgeToEdge` altera barras do sistema | Ícones de status bar com contraste errado | Configurar `windowLightStatusBar` e `windowLightNavigationBar` por tema. |
| Tema Compose não afeta XML Views | Expectativa de modernização não atendida | Deixar claro que o Compose é apenas consistência futura. |
| Componentes compartilhados com estilos sobrepostos | Resultado inconsistente | Revisar precedência entre style e atributos diretos. |
| Regressões em gráficos | Baixa legibilidade | Migrar cores de grade/rótulo e validar nos dois temas. |

## Definition of Done da refatoração

- Build e testes passando.
- Tema claro e escuro funcionando automaticamente.
- Nenhuma cor fixa crítica nos componentes compartilhados.
- Todas as telas principais revisadas.
- Estados de carregamento, vazio e erro consistentes.
- Funcionalidades existentes preservadas, incluindo máscara de valores e exportações.
- Documentação atualizada com o estado final.
