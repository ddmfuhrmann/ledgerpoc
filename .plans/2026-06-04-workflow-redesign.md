---
date: 2026-06-04
title: workflow-redesign
status: implemented
---

# Redesign do Workflow de Desenvolvimento

## Decisões fechadas

### Prefixo de comandos
- Prefixo: `f-` (ex: `/f-plan`, `/f-implement`, `/f-review`)
- Motivo: evitar colisão com skills nativas do Claude Code (`/plan`, `/review`, `/test`)

### `/f-plan`
- Usa o Plan agent nativo para exploração de codebase
- Consulta Notion (ADRs em `LedgerPOC/ADRs/`, Plans em `LedgerPOC/Plans/`)
- Produz plano estruturado
- Inicia o grill-me automaticamente ao final (sem comando separado)

### Grill-me (loop conversacional, não é mais um comando)
- Uma pergunta por vez via `AskUserQuestion` (opções clicáveis)
- Primeira opção sempre é a resposta recomendada
- "Other..." disponível automaticamente para respostas abertas
- Se a resposta está no codebase → explora o código, não pergunta
- Loop continua até consenso
- Ao final: salva o plano refinado + sugere o próximo comando

### Plano salvo ao fim do grill-me
- Local: `.plans/YYYY-MM-DD-<título>.md`
- Também salvo no Notion
- Frontmatter com `date`, `title`, `notion_url`
- Título sugerido pelo Claude, confirmado pelo usuário

### `/f-implement`
- Aceita título como argumento: `/f-implement cash-out-processor`
- Sem argumento: infere o título do contexto da conversa
- Lê `.plans/YYYY-MM-DD-<título>.md` como baseline
- Orquestra subagentes em sequência:
  1. Feature Implementer agent (escreve o código)
  2. Test Implementer agent (escreve e roda os testes)
- Se testes falharem: loop automático de correção (híbrido)
  - Até 3 tentativas automáticas (Feature Implementer corrige → testes rodam de novo)
  - Na 4ª falha: checkpoint via `AskUserQuestion`:
    - "Tentar mais 3 vezes" (Recomendado)
    - "Quero intervir agora"
    - "Abandonar e ver o estado atual"
- Ao concluir com sucesso: sugere `/f-ship`

### `/f-ship`
Substitui `/f-review` e `/f-handoff`. Disparado manualmente após `/f-implement` concluir.
Pode ser usado fora do pipeline (ex: revisitar uma entrega anterior).

Fluxo interno:
1. **Analisa** diff + plano — identifica findings e decisões arquiteturais
2. **Grill-me das findings** via `AskUserQuestion` — cada achado relevante vira uma pergunta com opção de agir agora, deferir ou abrir issue
3. **ADR check** — detecta decisões que desviam do padrão existente ou são difíceis de reverter:
   - Apresenta candidatas a ADR via `AskUserQuestion`
   - Opções: registrar todas / escolher quais / nenhuma por agora
   - Se registrar: cria ADR no Notion + arquivo local
4. **Grill-me do handoff** — perguntas sobre contexto, próximos passos, decisões pendentes
5. **Gera artefatos:** review summary + ADRs (se houver) + handoff doc → Notion + arquivos locais

### Comandos que deixam de existir
- `/grill-me` — vira loop automático do `/f-plan`
- `/revise-plan` — substituído pelo loop conversacional
- `/test` — absorvido pelo `/f-implement`
- `/f-review` — absorvido pelo `/f-ship`
- `/f-handoff` — absorvido pelo `/f-ship`
- `/adr` — absorvido pelo `/f-ship`

---

### `/f-optimize`
Comando independente. Sempre parte de um plano — seja o plano do `/f-implement` ou um plano dedicado criado via `/f-plan`.

O plano define o comportamento do loop:

**Com critério de performance** (ex: p95 < 200ms):
1. Prepara ambiente (local-env-orchestration)
2. Seeda dados realistas (database-seeding)
3. Coleta baseline (benchmark-execution)
4. Loop autônomo:
   - Analisa resultados (explain-analyze, observability, messaging-analysis)
   - Identifica gargalo
   - Propõe e aplica otimização
   - Re-roda harness e compara com baseline
   - Repete até critério atingido ou checkpoint (mesmo modelo do `/f-implement`: 3 tentativas automáticas, depois `AskUserQuestion`)
5. Gera relatório (optimization-reporting) → Notion + arquivo local

**Sem critério de performance:**
1–3. Igual ao acima
4. Análise only — sem aplicar mudanças
5. Gera relatório com findings e recomendações

Pode ser disparado após `/f-implement` (o próprio implement pergunta "quer rodar o optimize agora?") ou standalone.

### `/f-prd`
- Grill-me style via `AskUserQuestion`: uma pergunta por vez sobre problema, objetivo, requisitos, critérios de aceitação, fora do escopo
- Ao final: salva PRD no Notion + `.plans/YYYY-MM-DD-<título>-prd.md`
- Sugere `/f-plan` como próximo passo

---

## Estrutura de agentes

### Orchestrators (comandos inline — sem subagente próprio)
Coordenam o fluxo, tomam decisões, usam `AskUserQuestion`.
- `/f-plan`, `/f-implement`, `/f-ship`, `/f-optimize`, `/f-prd`

### Domain agents (Sonnet 4.6 — trabalho pesado de código)
| Agente | Responsabilidade |
|---|---|
| `feature-implementer` | Escreve código de produção |
| `test-implementer` | Escreve e roda testes |
| `reviewer` | Analisa diff vs plano |

### Optimizer (Opus 4.8 — análise complexa, loop autônomo)
| Agente | Responsabilidade |
|---|---|
| `optimizer` | Loop de medição + otimização, evidence-based |

### Integration agents (Haiku 4.5 — tarefas bem definidas, rápidas e baratas)
| Agente | Responsabilidade |
|---|---|
| `notion-agent` | Todo CRUD do Notion: ADRs, Plans, PRDs, Handoffs. Única fonte de `notion-docs.md` |
| `git-agent` | Branch, commit, PR |

**Princípio:** domain agents nunca chamam Notion ou git diretamente — delegam para os integration agents. O orquestrador coordena a sequência.

---

## Skills — redistribuição por agente

| Skill | Quem usa (novo) |
|---|---|
| `notion-docs.md` | `notion-agent` apenas |
| `grill-me.md` | Orquestrador `/f-plan` |
| `diff-review.md` | `reviewer` |
| `project-architecture.md` | `feature-implementer`, `test-implementer`, `reviewer` |
| `code-style.md` | `feature-implementer`, `reviewer` |
| `error-handling.md` | `feature-implementer`, `reviewer` |
| `plan-first-development.md` | `feature-implementer` |
| `testing-strategy.md`, `testcontainers.md`, `fixtures.md`, `edge-case-generation.md` | `test-implementer` |
| `local-env-orchestration.md`, `observability-setup.md`, `database-seeding.md`, `postgres-explain-analyze.md`, `messaging-analysis.md`, `benchmark-execution.md`, `optimization-reporting.md` | `optimizer` |
| `patterns.md` | `feature-implementer`, `test-implementer`, `reviewer` |
| `caveman.md` | global — todos os agentes e orquestradores |
| `karpathy-guidelines.md` | global — todos os agentes e orquestradores |

---

## Novos artefatos a criar

### `.skills/patterns.md`
Snippets canônicos extraídos do codebase real. Agentes copiam o padrão em vez de inventar.

Padrões a documentar:
- Entity JPA: constructor protegido, constructor público com defaults, seções de domain behavior e getters, `FetchType.LAZY`, `BigDecimal(18,2)`, `Instant`, sem setters
- Domain behavior: state transitions com guard clauses (`IllegalStateException`), `validateAmount()`, `touch()`
- CommandService: `@Service` + constructor injection + `@Transactional` → lookup → new domain obj → save → `OutboxEventType.X.toEvent()` → save outbox
- Processor: `@Component` + `@Transactional`, idempotência via `if (status != REQUESTED) return`, deserializa payload, delega para métodos privados
- Integration test: extends `AbstractIntegrationTest`, `@SpringBootTest` + `@Transactional`, `// given / when / then`, fixture helpers privados, nome `shouldDoXWhenY`

### `/f-sync-patterns` (novo comando)
Escaneia o codebase e atualiza `.skills/patterns.md` quando o código evolui.
Evita que os snippets fiquem stale conforme novas features são implementadas.

### `docs/architecture.md`
Documento para humanos E agentes. Explica o *porquê* do sistema — não regras de como escrever código.

Conteúdo previsto:
- Visão geral dos flows: cash-in, cash-out
- Por que append-only ledger
- Por que optimistic locking no Balance
- Por que outbox pattern (e não chamada direta)
- Por que serialização por payeeId
- Diagrama de sequência simplificado dos dois flows

Referenciado em CLAUDE.md como leitura obrigatória para agentes que tocam balance, ledger ou payment.

---

## Etapa final: documentação do workflow

Criar `docs/workflow.md` — guia de uso do workflow redesenhado. Destinado ao próprio usuário e a qualquer agente que precise entender como o sistema de desenvolvimento funciona.

### Conteúdo

**1. Visão geral**
Por que esse workflow existe: spec-driven, plan-first, subagentes isolados, integrações em Haiku, contexto preservado entre etapas.

Decisões principais registradas:
- Prefixo `f-` para evitar colisão com skills nativas
- Grill-me como loop conversacional com `AskUserQuestion` em vez de comando separado
- `/f-implement` orquestra implement + test + loop de correção automática
- `/f-ship` unifica review + ADR check + handoff
- Integrações (Notion, git) isoladas em subagentes Haiku 4.5
- `caveman` e `karpathy-guidelines` sempre ativos globalmente

**2. Diagrama de fluxo principal**
```
[conversa livre]
      ↓
  /f-prd  (opcional — features de maior escopo)
      ↓
  /f-plan  →  [grill-me loop]  →  plano salvo em .plans/
      ↓
  /f-implement  →  [feature-implementer]  →  [test-implementer]
                →  loop de correção (até 3x automático, depois checkpoint)
      ↓
  /f-ship  →  [reviewer]  →  [grill-me findings]  →  [ADR check]
           →  [grill-me handoff]  →  artefatos no Notion + local
```

**3. Comandos complementares**
- `/f-optimize` — quando e como usar (com e sem critério de performance), relação com o plano
- `/f-sync-patterns` — quando rodar (após implementar novo padrão relevante), o que atualiza

**4. Referência rápida de comandos**

| Comando | Quando usar |
|---|---|
| `/f-prd` | Antes do plan, para features de maior escopo |
| `/f-plan` | Sempre — entry point do ciclo de desenvolvimento |
| `/f-implement <título>` | Após grill-me concluir e plano salvo |
| `/f-ship` | Após implement completar com sucesso |
| `/f-optimize <título>` | Performance — standalone ou pós-implement |
| `/f-sync-patterns` | Após implementar padrão novo relevante |
