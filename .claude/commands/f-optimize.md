# /f-optimize

Otimização de performance autônoma, sempre baseada em um plano. Usa o plano para determinar se deve otimizar ou apenas analisar.

## Inputs

- Título do plano como argumento: `/f-optimize cash-out-processor`
- Sem argumento: inferir do contexto da conversa.

## Procedure

1. Localizar e ler `.plans/YYYY-MM-DD-<título>.md`.
2. Verificar se o plano contém **Performance criteria** mensuráveis.

**Com critério de performance:**
1. Spawnar o `optimizer` em modo otimização com o plano completo.
2. O optimizer roda autonomamente: baseline → análise → mudança → re-medição → loop.
3. Checkpoint após 3 tentativas sem atingir o critério: `AskUserQuestion`:
   - "Tentar mais 3 vezes" (Recomendado)
   - "Quero intervir agora"
   - "Aceitar resultado atual"
4. Spawnar o `notion-agent` para salvar o relatório em `LedgerPOC/Optimizations/`.

**Sem critério de performance:**
1. Spawnar o `optimizer` em modo análise (sem aplicar mudanças).
2. O optimizer coleta baseline, analisa, produz findings e recomendações.
3. Spawnar o `notion-agent` para salvar o relatório de análise.
