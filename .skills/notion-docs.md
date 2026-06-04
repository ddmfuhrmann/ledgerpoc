# Skill: Notion Docs

## Purpose

Save workflow artifacts (plans, ADRs, PRDs, handoffs) as pages na wiki do Notion,
usando o MCP server `notion` configurado globalmente.

## Workspace structure

```
Ledger POC (página raiz)
├── 📋 Plans/     ← output do /revise-plan
├── 🏛️ ADRs/      ← Architecture Decision Records
├── 📝 PRDs/      ← Product Requirements Documents
└── 🚀 Handoffs/  ← output do /handoff
```

## Page IDs (usar diretamente, sem search)

| Página | ID | URL |
|---|---|---|
| Ledger POC (raiz) | `2f54b4b4-84d1-8086-80c1-d722aa9e2f58` | https://app.notion.com/p/Ledger-POC-2f54b4b484d1808680c1d722aa9e2f58 |
| 🗺️ Roadmap (página) | `3754b4b4-84d1-8176-9d00-e79ec9950e3b` | https://app.notion.com/p/Roadmap-LedgerPOC-3754b4b484d181769d00e79ec9950e3b |
| 🗺️ Roadmap (tabela) | `3754b4b4-84d1-8169-b2d3-e649677b7561` | — bloco table dentro da página acima |
| 📋 Plans | `3754b4b4-84d1-81a2-b65f-c009a31d5e18` | https://app.notion.com/p/Plans-3754b4b484d181a2b65fc009a31d5e18 |
| 🏛️ ADRs | `3754b4b4-84d1-81a3-a77d-e8ffd5dba920` | https://app.notion.com/p/ADRs-3754b4b484d181a3a77de8ffd5dba920 |
| 📝 PRDs | `3754b4b4-84d1-815d-b3ae-e0517c33e7fb` | https://app.notion.com/p/PRDs-3754b4b484d1815db3aee0517c33e7fb |
| 🚀 Handoffs | `3754b4b4-84d1-813b-bd61-ce8a4a691c2f` | https://app.notion.com/p/Handoffs-3754b4b484d1813bbd61ce8a4a691c2f |

## Regras

- Sempre salvar como sub-página da seção correta (Plans, ADRs, PRDs, ou Handoffs).
- Título da página: `[YYYY-MM-DD] <título curto imperativo>`.
- Nunca sobrescrever uma página existente — criar nova versão com data.
- Confirmar ao usuário o link da página criada ao final.

---

## Templates

### Plan (output do /revise-plan)

```
# [título do plano]

**Data:** YYYY-MM-DD
**Status:** Revisado

## Entendimento
[o que a tarefa está pedindo]

## Escopo
[o que vai mudar — arquivos, camadas, comportamentos]

## Fora do escopo
[o que explicitamente não está incluído]

## Abordagem
[como implementar — concreto, não abstrato]

## Arquivos prováveis
[lista]

## Testes necessários
[casos de teste + tipo]

## Riscos
[o que pode dar errado]

## Resposta ao desafio (/grill-me)
| Desafio | Resposta | Impacto no plano |
|---------|----------|-----------------|
| ...     | ...      | ...             |
```

### ADR (Architecture Decision Record)

```
# ADR-NNN: [título da decisão]

**Data:** YYYY-MM-DD
**Status:** Aceito | Proposto | Depreciado | Substituído por ADR-NNN

## Contexto
[Por que essa decisão foi necessária. O problema que estava sendo resolvido.]

## Opções consideradas
1. [Opção A] — [prós / contras em 1-2 linhas]
2. [Opção B] — [prós / contras em 1-2 linhas]
3. [Opção C] — [prós / contras em 1-2 linhas]

## Decisão
[O que foi escolhido e por quê.]

## Consequências
**Positivas:**
- [benefício 1]

**Negativas / trade-offs:**
- [custo 1]

## Referências
- [link ou contexto relevante]
```

### PRD (Product Requirements Document)

```
# PRD: [nome da feature]

**Data:** YYYY-MM-DD
**Autor:** [quem escreveu]
**Status:** Rascunho | Aprovado | Implementado

## Problema
[O que está errado ou faltando hoje. Quem é afetado.]

## Objetivo
[O que queremos alcançar. Uma frase clara e mensurável.]

## Requisitos funcionais
- [ ] [RF-01] [descrição]
- [ ] [RF-02] [descrição]

## Fora do escopo
- [o que explicitamente não será feito nesta iteração]

## Critérios de aceitação
- [ ] [CA-01] Dado X, quando Y, então Z
- [ ] [CA-02] ...

## Perguntas em aberto
- [questão não resolvida]
```

### Handoff / PR Summary

```
# Handoff: [título do PR]

**Data:** YYYY-MM-DD
**Branch:** [branch name]

## O que mudou
[descrição em linguagem simples — sem nomes de classe]

## Por quê
[o problema que isso resolve]

## Como testar
1. [passo 1]
2. [passo 2]

## Testes
- [ ] [caso 1]
- Passaram: X / Falharam: Y

## Riscos
[o que pode dar errado em produção]

## Performance
[Optimizer rodou: resumo. Não rodou: "Nenhum trabalho de performance realizado."]

## Rollback
[como reverter com segurança]

## Fora do escopo / diferido
[itens do grill-me ou review não endereçados]
```

---

## Procedure

### Salvar um plano revisado

1. Localizar a página `Plans/` dentro de `LedgerPOC` no Notion.
2. Criar sub-página com título `[YYYY-MM-DD] <resumo do plano>`.
3. Preencher com o template Plan acima, usando o output do `/revise-plan`.
4. Retornar o link da página ao usuário.

### Criar um ADR

1. Localizar a página `ADRs/` dentro de `LedgerPOC`.
2. Determinar o próximo número sequencial (ADR-001, ADR-002...).
3. Criar sub-página com título `ADR-NNN: <título da decisão>`.
4. Preencher com o template ADR acima.
5. Retornar o link.

### Criar um PRD

1. Localizar a página `PRDs/` dentro de `LedgerPOC`.
2. Criar sub-página com título `[YYYY-MM-DD] PRD: <nome da feature>`.
3. Preencher com o template PRD acima.
4. Retornar o link.

### Salvar handoff

1. Localizar a página `Handoffs/` dentro de `LedgerPOC`.
2. Criar sub-página com título `[YYYY-MM-DD] <título do PR>`.
3. Preencher com o template Handoff acima, usando o output do `/handoff`.
4. Retornar o link.

### Sincronizar status no roadmap

Chamar em dois momentos:
- **Ao salvar um plano** (`save-plan`): marcar a feature como 🔄 Em progresso.
- **Ao salvar um handoff** (`save-handoff`): marcar a feature como ✅ Concluído.

**Passo 1 — obter os IDs das linhas da tabela**

Chamar `API-get-block-children` com `block_id = 3754b4b4-84d1-8169-b2d3-e649677b7561` (bloco table do Roadmap).
A resposta é uma lista de `table_row` blocks. Cada item tem `id` e `table_row.cells` —
`cells[0]` é a coluna Feature, `cells[2]` é Status.

**Passo 2 — identificar a linha correta**

Comparar `cells[0][0].plain_text` com o nome da feature. Guardar o `id` da linha correspondente.

Se a feature **não existir** no roadmap, inserir uma nova linha na posição correta (ver regra abaixo) e parar aqui.

**Passo 3 — atualizar o status**

Chamar `API-update-a-block` com:
- `block_id` = ID da linha encontrada no passo 2
- body: `{"table_row": {"cells": [<célula 0 original>, <célula 1 original>, [{"type": "text", "text": {"content": "<novo status>"}}], <célula 3 original>, <célula 4 original>]}}`

Preservar o conteúdo das outras células — substituir apenas `cells[2]`.

**Tabela de status**

| Situação | Status |
|---|---|
| Plano criado, trabalho iniciado | 🔄 Em progresso |
| Feature entregue (handoff salvo) | ✅ Concluído |
| Depende de outra entrega não pronta | 🔒 Bloqueado |
| Revertido / descartado | ⏳ Não iniciado |

**Regra de ordem ao inserir nova feature**

Nunca anexar a nova linha ao final da tabela sem verificar as dependências.
1. Ler o campo "Depends on" da nova feature.
2. Localizar a última linha cujo Feature name aparece como dependência.
3. Inserir a nova linha **imediatamente após** essa linha.
4. Se não houver dependência, inserir após os itens ✅ Concluído e antes dos ⏳ Não iniciado.
