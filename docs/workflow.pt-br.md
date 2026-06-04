# Workflow de Desenvolvimento — LedgerPOC

> English version: `docs/workflow.md`

## Visão geral

Este workflow foi redesenhado para ser **spec-driven e plan-first**: nenhum código é escrito sem um plano revisado. O ciclo preserva contexto entre etapas via arquivos locais em `.plans/` e o Notion, e distribui responsabilidades entre subagentes isolados para evitar que contexto de uma etapa contamine outra.

Decisões principais do redesign:

- **Prefixo `f-`** — evita colisão com skills nativas do Claude Code (`/plan`, `/review`, `/test`).
- **Grill-me como loop conversacional** — integrado ao `/f-plan` via `AskUserQuestion`, uma pergunta por vez, sem comando separado.
- **`/f-implement` orquestra implement + test + loop de correção** — sem necessidade de invocar `/test` separadamente.
- **`/f-ship` unifica review + ADR check + handoff** — um único comando para fechar o ciclo.
- **Integrações isoladas em Haiku 4.5** — `notion-agent` e `git-agent` são baratos e focados; domain agents nunca chamam Notion ou git diretamente.
- **`caveman` e `karpathy-guidelines` sempre ativos** — todos os agentes carregam essas skills por padrão.

---

## Fluxo principal

```
[conversa livre / ticket / PRD]
         │
    /f-prd  (opcional — features de maior escopo)
         │
    /f-plan  ──→  [grill-me loop via AskUserQuestion]  ──→  plano salvo em .plans/
         │
    /f-implement <título>
         ├──→  [feature-implementer]  (escreve código)
         ├──→  [test-implementer]     (escreve e roda testes)
         └──→  loop de correção automática (até 3x, depois checkpoint)
         │
    /f-ship
         ├──→  [git-agent]    (obtém diff)
         ├──→  [reviewer]     (analisa diff vs plano)
         ├──→  grill-me das findings via AskUserQuestion
         ├──→  ADR check via AskUserQuestion
         ├──→  grill-me do handoff via AskUserQuestion
         ├──→  [notion-agent] (salva review + ADRs + handoff)
         └──→  [git-agent]    (cria PR)
```

**Fluxo com optimize:**

```
    /f-implement <título>
         │ (ao concluir: AskUserQuestion — rodar /f-optimize?)
         ↓
    /f-optimize <título>
         ├──→  [optimizer]    (baseline → análise → loop)
         └──→  [notion-agent] (salva relatório)
         ↓
    /f-ship
```

**Fluxo com PRD:**

```
    /f-prd
         ├──→  grill-me via AskUserQuestion
         ├──→  salva .plans/YYYY-MM-DD-<título>-prd.md
         └──→  [notion-agent] (salva em LedgerPOC/PRDs/)
         ↓
    /f-plan  (lê o PRD como input)
```

---

## Comandos principais

### /f-plan

Entry point do ciclo. Aceita qualquer input (conversa, ticket, PRD, ideia).

1. Usa o Plan agent nativo para explorar o codebase.
2. Spawna `notion-agent` para buscar ADRs e plans anteriores relacionados.
3. Produz plano estruturado com 10 seções: Understanding, Assumptions, Scope, Out of scope, Approach, Files likely to change, Tests needed, Risks, Performance criteria, Blocking questions.
4. Inicia grill-me automático — uma pergunta por vez via `AskUserQuestion`.
5. Salva plano refinado em `.plans/YYYY-MM-DD-<título>.md` e no Notion.

### /f-implement \<título\>

Orquestra implementação completa. O argumento é o título kebab-case do plano.

1. Lê `.plans/YYYY-MM-DD-<título>.md`.
2. Spawna `feature-implementer` (código de produção).
3. Spawna `test-implementer` (testes).
4. Se testes falham: loop automático de correção (até 3x), depois checkpoint via `AskUserQuestion`.
5. Ao concluir: pergunta se quer rodar `/f-optimize`.

### /f-ship

Fecha o ciclo. Pode ser usado fora do pipeline para revisitar entregas anteriores.

1. Spawna `git-agent` para obter o diff.
2. Spawna `reviewer` com plano + diff + summaries.
3. Grill-me interativo das findings (BLOCKER → corrigir, WARNING → deferir, SUGGESTION → issue).
4. ADR check: detecta decisões arquiteturais candidatas, pergunta se registrar.
5. Grill-me do handoff: próximo passo, decisões pendentes, riscos para produção.
6. Spawna `notion-agent` para salvar artefatos.
7. Spawna `git-agent` para criar PR.

**Integração com SonarQube (opt-in):** se `sonar-project.properties` existir na raiz do projeto, o `reviewer` executa análise estática completa via SonarQube antes de produzir o review summary. Veja [Integrações opcionais — SonarQube](#sonarqube-via-docker) abaixo.

---

## Comandos complementares

### /f-optimize \<título\>

Otimização de performance baseada em plano. Comportamento determinado pelo campo **Performance criteria** do plano.

- **Com critério mensurável** (ex: `p95 < 200ms`): optimizer roda autonomamente — baseline → análise → mudança → re-medição → loop. Checkpoint a cada 3 tentativas.
- **Sem critério**: optimizer coleta baseline e produz findings sem aplicar mudanças.

Em ambos os casos, o relatório é salvo no Notion (`LedgerPOC/Optimizations/`).

### /f-prd

Cria PRD via grill-me conversacional. Use para features de maior escopo antes do `/f-plan`. Uma pergunta por vez: nome, problema, objetivo mensurável, requisitos, fora do escopo, critérios de aceitação, bloqueadores.

### /f-sync-patterns

Escaneia todos os arquivos Java e reescreve `.skills/patterns.md` com snippets canônicos atualizados. Usar após implementar um padrão novo que outros agentes devem seguir.

---

## Referência rápida

| Comando | Quando usar |
|---|---|
| `/f-prd` | Antes do plan, para features de maior escopo |
| `/f-plan` | Sempre — entry point do ciclo |
| `/f-implement <título>` | Após grill-me concluir e plano salvo |
| `/f-ship` | Após implement completar com sucesso |
| `/f-optimize <título>` | Performance — standalone ou pós-implement |
| `/f-sync-patterns` | Após implementar padrão novo relevante |

---

## Estrutura de agentes

| Tipo | Agente | Modelo | Responsabilidade |
|---|---|---|---|
| Orchestrator | `/f-plan`, `/f-implement`, `/f-ship`, `/f-optimize`, `/f-prd` | Sonnet 4.6 (inline) | Coordena fluxo, usa AskUserQuestion, nunca escreve código |
| Domain | `feature-implementer` | Sonnet 4.6 | Escreve código de produção |
| Domain | `test-implementer` | Sonnet 4.6 | Escreve e roda testes |
| Domain | `reviewer` | Opus 4.8 | Analisa diff vs plano |
| Optimizer | `optimizer` | Opus 4.8 | Loop de medição + otimização, evidence-based |
| Integration | `notion-agent` | Haiku 4.5 | Todo CRUD do Notion |
| Integration | `git-agent` | Haiku 4.5 | Branch, commit, PR |

**Princípio de isolamento:** domain agents nunca chamam Notion ou git diretamente — delegam para `notion-agent` e `git-agent` via o orquestrador.

---

## Artefatos locais

| Caminho | Conteúdo |
|---|---|
| `.plans/YYYY-MM-DD-<título>.md` | Plano refinado com frontmatter (`date`, `title`, `notion_url`) |
| `.plans/YYYY-MM-DD-<título>-prd.md` | PRD com frontmatter |
| `docs/architecture.md` | Arquitetura do sistema (leitura obrigatória para agentes que tocam Balance, LedgerEntry ou Payment) |
| `docs/workflow.md` | Este documento (English) |
| `docs/workflow.pt-br.md` | Este documento (Português BR) |
| `.skills/patterns.md` | Snippets canônicos do codebase (atualizado via `/f-sync-patterns`) |

---

## Integrações opcionais

### SonarQube via Docker *(experimental)*

> Ainda não validado em todos os stacks e configurações de CI. Trate os findings como sinal suplementar.

O agente `reviewer` suporta análise estática via SonarQube como integração opt-in. Quando ativa, os findings (code smells, bugs, vulnerabilidades) são incluídos no review summary com os mesmos rótulos de severidade `BLOCKER | WARNING | SUGGESTION`.

**Ativação:** crie `sonar-project.properties` na raiz do projeto. A presença do arquivo é o sinal de opt-in — o reviewer executa a análise automaticamente em todo `/f-ship`.

**`sonar-project.properties` — apenas identidade do projeto (seguro de commitar):**

```properties
sonar.projectKey=ledgerpoc
sonar.sources=src
sonar.exclusions=**/test/**
```

Não adicione `sonar.host.url` ou `sonar.token` neste arquivo. A skill os injeta como overrides de CLI em tempo de execução, mantendo o arquivo compatível com pipelines de CI que usam seu próprio host e token. Um step de SonarQube/SonarCloud no GitHub Actions passa seus próprios flags `-D` e não é afetado.

**Setup: apenas Docker.** Sem instalação de CLI, sem configuração manual de token.

No primeiro uso, a skill sobe o `bsdd-sonarqube`, gera um token automaticamente via API do SonarQube e salva em `.bsdd-sonar-token` (gitignored). Reviews seguintes reutilizam o token salvo. O scanner roda como container efêmero `sonarsource/sonar-scanner-cli` na network compartilhada `bsdd-sonar-net`. Se o SonarQube não ficar saudável em 120s, o review é bloqueado com mensagem de erro explícita.

**Mapeamento de severidade:**

| Severidade Sonar | Tipo Sonar | Label do reviewer |
|---|---|---|
| BLOCKER / CRITICAL | qualquer | `BLOCKER` |
| MAJOR | BUG / VULNERABILITY | `BLOCKER` |
| MAJOR | CODE_SMELL | `WARNING` |
| MINOR / INFO | qualquer | `SUGGESTION` |
| Security hotspot | qualquer | `WARNING` |

Apenas issues em arquivos presentes no diff atual são exibidos. Findings do projeto fora do changeset são ignorados.
