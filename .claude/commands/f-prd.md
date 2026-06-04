# /f-prd

Cria um PRD via grill-me conversacional. Use antes do `/f-plan` para features de maior escopo.

## Procedure

1. Usar `AskUserQuestion` para coletar, uma pergunta por vez:
   - "Qual é o nome da feature?"
   - "Qual problema resolve? Quem é afetado?"
   - "Qual o objetivo mensurável?"
   - "Quais são os requisitos funcionais principais?"
   - "O que está explicitamente fora do escopo?"
   - "Quais são os critérios de aceitação?"
   - "Há perguntas em aberto ou bloqueadores?"
2. Rascunhar o PRD e apresentar para revisão antes de salvar.
3. Sugerir um título curto (kebab-case) e confirmar.
4. Salvar localmente em `.plans/YYYY-MM-DD-<título>-prd.md`.
5. Spawnar o `notion-agent` para salvar em `LedgerPOC/PRDs/`.
6. Sugerir: `PRD salvo. Rode /f-plan para transformar em plano de implementação.`
