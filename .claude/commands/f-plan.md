# /f-plan

Entry point do ciclo de desenvolvimento. Transforma qualquer input (conversa, ticket, PRD, ideia vaga) em um plano refinado via grill-me.

## Procedure

1. Usar o Plan agent nativo (subagent_type: Plan) para explorar o codebase e entender o impacto da tarefa.
2. Spawnar o `notion-agent` para:
   - Buscar ADRs em `LedgerPOC/ADRs/` (page_id: `3754b4b4-84d1-81a3-a77d-e8ffd5dba920`) que possam afetar a abordagem.
   - Buscar Plans anteriores em `LedgerPOC/Plans/` (page_id: `3754b4b4-84d1-81a2-b65f-c009a31d5e18`) com escopo sobreposto.
3. Produzir o plano estruturado com as seções:
   - **Understanding** — o que a tarefa pede, em suas próprias palavras
   - **Assumptions** — o que está sendo tratado como verdade sem estar explícito
   - **Scope** — o que vai mudar (arquivos, camadas, comportamentos)
   - **Out of scope** — o que explicitamente não será feito
   - **Approach** — como implementar (concreto: "adicionar método X na classe Y que faz Z")
   - **Files likely to change** — lista de arquivos ou diretórios
   - **Tests needed** — casos de teste e tipo (unit/integration/contract/e2e)
   - **Risks** — o que pode dar errado
   - **Performance criteria** — critérios mensuráveis se houver, ou "none" se não houver
   - **Blocking questions** — perguntas que devem ser respondidas antes de implementar
4. Iniciar o loop de grill-me automaticamente:
   - Para cada pergunta relevante sobre o plano, usar `AskUserQuestion` com uma pergunta por vez.
   - Sempre incluir a resposta recomendada como primeira opção.
   - Se a resposta pode ser encontrada no codebase, explorar o código e responder sem perguntar.
   - Continuar até que não haja mais perguntas abertas relevantes.
5. Ao concluir o grill-me:
   - Sugerir um título curto (kebab-case) para o plano e confirmar com o usuário.
   - Salvar o plano refinado localmente em `.plans/YYYY-MM-DD-<título>.md` com frontmatter:
     ```
     ---
     date: YYYY-MM-DD
     title: <título>
     notion_url: <url após salvar>
     ---
     ```
   - Spawnar o `notion-agent` para salvar o plano em `LedgerPOC/Plans/`.
   - Sugerir: `Plano salvo. Rode /f-implement <título> para continuar.`
