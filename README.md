# ReCode — Reconstruindo um Mundo Digital

Projeto acadêmico de um jogo 2D em libGDX controlado por movimento via IoT. O primeiro protótipo usa um mini pedal com ESP32 e sensor reed: cada volta válida avança ELO pela fase Circuitos Básicos.

## Estado atual

- **Documentação:** [Game Design Document](docs/ReCode_GDD.docx), com decisões do projeto e pontos ainda sujeitos à validação.
- **Planejamento:** 23 cartões do Kanban, dados de prioridade e prazo e scripts usados para registrar/verificar as Issues e campos do GitHub Project em [`planning/`](planning/).
- **Protótipo:** código Java/libGDX, firmware ESP32, sprites e cenários em [`prototipo_pedal/`](prototipo_pedal/). Veja as instruções de montagem, execução e verificação no [README do protótipo](prototipo_pedal/README.md).

O protótipo implementa a primeira fase e oferece prévias visuais das outras duas. A simulação de pedal serve para desenvolvimento; a validação do controle físico e dos objetivos de reabilitação permanece pendente. Os parâmetros de voltas e cadência são provisórios e não representam prescrição de exercício.

## Estrutura

```text
docs/             GDD editável
planning/         cartões, cronograma e scripts do Kanban
prototipo_pedal/  aplicativo desktop, firmware e recursos visuais
```
