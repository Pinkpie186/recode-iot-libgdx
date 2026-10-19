# ReCode — Reconstruindo um Mundo Digital

ReCode é um jogo 2D em Java e libGDX controlado por pedaladas. ELO reconstrói a rede digital em três fases, enquanto Nó Zero interfere no caminho. O mini pedal com ESP32 e sensor reed envia as voltas ao jogo; também há um modo teclado para jogar sem o dispositivo conectado.

## O jogo

1. **Circuitos Básicos:** ELO repara os circuitos e recolhe energia.
2. **Rede Instável:** sequências de pedaladas regulares estabilizam os nós da rede.
3. **Sistema Central:** ELO enfrenta Nó Zero, restaura os condutos e protege o núcleo. Quando Nó Zero bloqueia uma linha, ELO para na chave de rota. Uma pedalada individual aciona o desvio e leva o personagem à outra linha.

O menu inicia ou continua a campanha. `Espaço` abre as opções de pausa, inclusive voltar ao menu. O jogo mantém a fase, a integridade e o desempenho enquanto a campanha está em andamento. O escudo do mascote é carregado por pedaladas regulares e protege ELO por 15 segundos de jogo.

**Jogar e configurar:** veja as instruções em [prototipo_pedal/README.md](prototipo_pedal/README.md). O nome da pasta foi mantido para preservar os caminhos do projeto.

## Projeto

- [Game Design Document](docs/ReCode_GDD.docx)
- [Planejamento e cartões do Kanban](planning/)
- [Jogo, firmware e recursos visuais](prototipo_pedal/)

Os parâmetros de voltas e regularidade são regras do jogo. A calibração do controle físico e qualquer uso para reabilitação exigem validação específica; o jogo não define uma prescrição de exercício.
