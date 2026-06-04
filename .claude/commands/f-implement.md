# /f-implement

Orquestra a implementação completa: código + testes, com loop automático de correção.

## Inputs

- Título do plano como argumento: `/f-implement cash-out-processor`
- Sem argumento: inferir o título do contexto da conversa (plano salvo mais recentemente).

## Procedure

1. Localizar o arquivo `.plans/YYYY-MM-DD-<título>.md` e lê-lo.
2. Spawnar o `feature-implementer` com o conteúdo completo do plano.
3. Após o feature-implementer concluir, spawnar o `test-implementer` com o plano + implementation summary.
4. Se os testes passarem: ir para o passo 7.
5. Se os testes falharem (tentativa 1, 2 ou 3):
   - Spawnar o `feature-implementer` novamente com o plano + test failure output para corrigir.
   - Spawnar o `test-implementer` novamente.
   - Repetir até 3 tentativas automáticas.
6. Se os testes falharem na 4ª vez: checkpoint via `AskUserQuestion`:
   - "Tentar mais 3 vezes" (Recomendado)
   - "Quero intervir agora"
   - "Abandonar e ver o estado atual"
   - Se "Tentar mais 3 vezes": reiniciar contador e voltar ao passo 5.
   - Se "Quero intervir agora": parar e apresentar o estado atual (último failure output).
   - Se "Abandonar": apresentar o estado atual e encerrar.
7. Ao concluir com sucesso: usar `AskUserQuestion` para perguntar:
   - "Quer rodar o /f-optimize agora?" (Recomendado: não, ir direto para /f-ship)
   - "Sim, rodar /f-optimize"
   - "Não, ir para /f-ship"
8. Sugerir: `Implementação concluída. Rode /f-ship para revisar e fazer o handoff.`
