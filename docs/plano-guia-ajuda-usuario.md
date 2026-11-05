# Plano de evolução — Guia de ajuda do usuário

**Status geral:** concluído e validado visualmente
**Objetivo:** apresentar dicas contextuais na primeira visita às telas, destacando os controles mais importantes, e permitir que o usuário reative os guias em Configurações.

## 1. Experiência proposta

- **Destaque visual:** a tela recebe uma camada escurecida com um recorte iluminado ao redor do elemento explicado.
- **Balão de ajuda:** mostra título, mensagem, progresso (`Passo 2 de 5`), botões **Voltar**, **Pular** e **Próxima →**.
- **Fluxo de lançamentos:** na primeira visita à tela de lançamentos, o botão **+** é destacado com a mensagem para adicionar uma nova despesa. Ao tocar no **+** (ou em **Adicionar agora →**), o modal abre e recebe um guia passo a passo dos campos.
- **Reativação:** em **Configurações > Ajuda e dicas**, o usuário pode ligar/desligar os guias ou tocar em **Reexibir dicas agora**.
- **Persistência por usuário:** o progresso é salvo localmente por identidade do usuário. Ao reativar, os guias concluídos são liberados novamente.

## 2. Arquitetura implementada

| Componente | Responsabilidade | Status |
|---|---|---|
| `HelpPreferences` | Persistir guias concluídos, estado geral e reset por usuário | [x] |
| `HelpStep` | Modelo imutável de uma dica (alvo, título, mensagem, rótulo e clique no alvo) | [x] |
| `FeatureTour` | Exibir overlay, posicionar o balão, controlar passos e callbacks | [x] |
| `TourSpotlightView` | Desenhar o fundo escurecido e o recorte iluminado sobre o elemento | [x] |
| `view_feature_tour.xml` | Layout do balão de ajuda com progresso e navegação | [x] |
| Configurações | Interruptor de ajuda e ação para reexibir todas as dicas | [x] |

## 3. Catálogo de guias

| Tela | Elementos destacados | Status |
|---|---|---|
| Lançamentos — introdução | Botão **+**; abre o modal ao concluir ou tocar no alvo | [x] |
| Lançamentos — formulário | Tipo, descrição, valor/data, categoria, conta/cartão e salvar | [x] |
| Dashboard | Saldo, período, ocultar valores e atalho de novo lançamento | [x] |
| Contas | Resumo de saldos, nova conta, novo cartão e conciliação pendente | [x] |
| Relatórios | Período, tipo, categorias, resumo e exportação | [x] |
| Cartões | Seleção do cartão, visão da fatura, status, novo lançamento e exportação | [x] |
| Categorias | Alternância despesa/receita, nova categoria e categorias padrão | [x] |
| Importação OFX | Seleção do arquivo e revisão da prévia/conciliação | [x] |
| Espaço financeiro | Seleção do espaço, pessoas com acesso, convite e migração | [x] |
| Menu | Relatórios, importação, configurações e espaço financeiro | [x] |
| Configurações | Metas, orçamento, metas por categoria, DND, retenção e reativação da ajuda | [x] |
| Notificações | Alertas financeiros, lembretes personalizados, aparelho e histórico | [x] |

## 4. Regras de exibição

- O guia só aparece quando a ajuda está habilitada e aquele guia ainda não foi concluído/pulado.
- Se o usuário tocar em **Pular**, o guia atual é considerado encerrado para evitar repetição.
- Guias de elementos que estão ocultos (por exemplo, resumo sem contas cadastradas) são omitidos automaticamente.
- Em Cartões, o guia só inicia quando existe pelo menos um cartão cadastrado.
- O progresso é local ao aparelho e separado por usuário.
- A rolagem automática mantém o destaque acima da barra inferior e dos botões flutuantes.
- O contorno é recalculado durante a rolagem manual da página.

## 5. Checklist de validação

- [x] Primeira abertura de Lançamentos exibe a dica no botão **+**.
- [x] Tocar no **+** abre o modal e inicia o guia do formulário.
- [x] **Próxima**, **Voltar**, **Pular** e **Entendi** funcionam nos guias validados.
- [x] O destaque acompanha o elemento após rolagem manual.
- [x] O elemento é rolado automaticamente para uma área segura acima da barra inferior e dos botões flutuantes.
- [x] Os guias não voltam a aparecer depois de concluídos/pulados.
- [x] **Reexibir dicas agora** libera novamente os guias para as próximas telas.
- [x] Desligar o interruptor impede novos guias.
- [x] Validar visualmente o guia de seleção/revisão da importação OFX.
- [x] Validar visualmente o guia do espaço financeiro.
- [x] Validar visualmente o guia do menu principal.
- [x] Validar visualmente o guia de Configurações.
- [x] Validar visualmente o guia de Notificações.
- [x] Projeto compila sem erros (`compileDebugJavaWithJavac`).
- [x] Testes unitários existentes continuam passando (`testDebugUnitTest`).
- [x] Lint executado sem erros (`lintDebug`).
- [x] APK de debug gerado (`assembleDebug`).

> Todos os fluxos foram validados visualmente pelo usuário, incluindo a correção do destaque sobre a barra inferior e os botões flutuantes.

## 7. Arquivos principais

- `ui/help/HelpPreferences.java` — estado por usuário.
- `ui/help/HelpGuide.java` — identificadores dos guias.
- `ui/help/HelpStep.java` — modelo de cada dica.
- `ui/help/FeatureTour.java` — controle do overlay e navegação.
- `ui/help/TourSpotlightView.java` — recorte iluminado sobre o elemento.
- `res/layout/view_feature_tour.xml` — balão com progresso e ações.
- `res/layout/activity_settings.xml` — seção **Ajuda e dicas**.
- `ui/transactions/TransactionsActivity.java` — introdução no **+** e guia do formulário.
- `ui/dashboard/DashboardActivity.java` — guia do dashboard e atalho para lançamentos.
- `ui/accounts/AccountsActivity.java` — guia de saldos, contas e conciliação.
- `ui/reports/ReportsActivity.java` — guia de filtros e exportação.
- `ui/cards/CardsActivity.java` — guia de faturas e ações do cartão.
- `ui/categories/CategoriesActivity.java` — guia de organização de categorias.
- `ui/accounts/StatementImportActivity.java` — guia de seleção e revisão da importação.
- `ui/finance/FinanceSpaceActivity.java` — guia do espaço financeiro.
- `ui/menu/MenuActivity.java` — guia das áreas secundárias do menu.
- `ui/settings/SettingsActivity.java` — guia de metas, DND, retenção e ajuda.
- `ui/notifications/NotificationsActivity.java` — guia dos alertas e lembretes.

## 6. Próximas evoluções sugeridas

- Feedback visual de carregamento padronizado nas listas e gráficos.
- Testes instrumentados cobrindo abertura, navegação e reexibição dos guias.
- Revisão de acessibilidade dos overlays com TalkBack.
- Dicas sazonais ou novidades por versão do aplicativo.
