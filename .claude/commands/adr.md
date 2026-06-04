# /adr

Captura uma decisão arquitetural e salva como ADR no Notion.

Leia `.skills/notion-docs.md` antes de começar.

---

## Quando usar

Use após qualquer decisão técnica relevante que não está óbvia no código:
escolha de particionamento, estratégia de serialização, padrão de outbox,
decisão de não usar X em favor de Y, etc.

## Inputs

Peça ao usuário (se não fornecido):
1. **Título da decisão** — frase curta e imperativa ("Usar particionamento por tempo no ledger")
2. **Contexto** — por que a decisão foi necessária
3. **Opções consideradas** — pelo menos duas
4. **Decisão tomada** — o que foi escolhido e por quê
5. **Consequências** — positivas e trade-offs

## Procedure

1. Coletar os inputs acima.
2. Determinar o próximo número de ADR (buscar em `ADRs/` no Notion).
3. Criar a página no Notion usando o template ADR em `.skills/notion-docs.md`.
4. Confirmar com o link da página criada.

---

> ADR salvo. Retorne ao workflow ou use `/plan` para a próxima tarefa.
