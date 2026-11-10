# ReCode — Reconstruindo um Mundo Digital

ReCode é um jogo 2D em Java e libGDX controlado pelos giros do antebraço. ELO reconstrói a rede digital em três fases, enquanto Nó Zero interfere no caminho. Um MPU6050 fixado ao antebraço envia os movimentos ao ESP32; também há um modo teclado para jogar sem o dispositivo conectado.

## O jogo

1. **Circuitos Básicos:** ELO repara os circuitos e recolhe energia.
2. **Rede Instável:** sequências de giros regulares estabilizam os nós da rede.
3. **Sistema Central:** ELO enfrenta Nó Zero, restaura os condutos e protege o núcleo. Ao entrar na fase, os três corações são restaurados. Depois de três giros individuais, ELO troca de rota automaticamente; não é preciso sustentar o braço inclinado.

O menu inicia ou continua a campanha. Cada fase apresenta seu objetivo antes de começar, e a passagem para a próxima área espera a pessoa estar pronta. `Espaço` abre as opções de pausa, inclusive voltar ao menu e suavizar os efeitos de fundo. O jogo mantém a fase e o desempenho enquanto a campanha está em andamento. O escudo do mascote é carregado por giros regulares e protege ELO por 15 segundos de jogo. A interferência de Nó Zero dura até 3 segundos por estação nas fases 2 e 3. O menu e as três fases têm música e efeitos sonoros.

**Entrega final e executáveis Windows:** veja [ENTREGA.md](ENTREGA.md). Para compilar e configurar o projeto, consulte [prototipo_pedal/README.md](prototipo_pedal/README.md). O nome da pasta foi mantido para preservar os caminhos do projeto.

## Projeto

- [Game Design Document](docs/ReCode_GDD.docx)
- [Planejamento e cartões do Kanban](planning/)
- [Jogo, firmware e recursos visuais](prototipo_pedal/)

Os parâmetros de giros e regularidade são regras do jogo. A calibração do controle físico e qualquer uso para reabilitação exigem validação específica; o jogo não define uma prescrição de exercício.

