# /f-ship

Unifica review, ADR check e handoff em um único fluxo conversacional. Disparado manualmente após `/f-implement` concluir.

## Procedure

1. Spawnar o `git-agent` para obter o diff (`git diff main`).
2. Identificar o plano relacionado (do contexto da conversa ou perguntar).
3. Spawnar o `reviewer` com: conteúdo do plano + diff + implementation summary + test summary.
4. Para cada finding relevante do reviewer, usar `AskUserQuestion`:
   - Apresentar o achado e perguntar a ação: "Corrigir agora" / "Deferir para depois" / "Abrir issue" (Recomendado varia por severidade: BLOCKER → corrigir, WARNING → deferir, SUGGESTION → abrir issue).
   - Se "Corrigir agora": spawnar o `feature-implementer` para aplicar a correção.
5. ADR check — analisar o diff em busca de decisões arquiteturais que desviam do padrão ou são difíceis de reverter. Se encontrar candidatas:
   - Apresentar via `AskUserQuestion`: "Registrar todos" / "Escolher quais" / "Nenhuma por agora".
   - Se registrar: spawnar o `notion-agent` para criar ADR(s).
6. Grill-me do handoff — usar `AskUserQuestion` para coletar contexto:
   - "Qual é o próximo passo após essa entrega?" (opções baseadas no contexto)
   - "Há decisões pendentes que devem ser registradas?"
   - "Há riscos conhecidos para produção?"
7. Spawnar o `notion-agent` para salvar: review summary + ADRs (se houver) + handoff doc em `LedgerPOC/Handoffs/`.
8. Spawnar o `git-agent` para criar o PR com o summary gerado.
9. Confirmar com os links criados.
