# Entrega final do ReCode

O pacote `ReCode_Entrega_Final.zip` reúne o jogo para Windows, o código fonte, o firmware da ESP32 e os documentos do projeto.

## Iniciar o jogo

1. Extraia o ZIP completo em uma pasta do Windows.
2. Abra `Executaveis/ReCode`.
3. Escolha `ReCode-Teclado.exe` para jogar pelo teclado ou `ReCode-ESP32.exe` para jogar com a placa e o MPU6050.

Os dois executáveis usam as pastas `app` e `runtime` da mesma pasta. Elas devem continuar junto dos arquivos `.exe`; não é necessário instalar Java separadamente.

Para o modo ESP32, conecte a placa por USB, feche o Monitor Serial da Arduino IDE e ligue o sensor com o antebraço apoiado na posição central. Mantenha a posição por cerca de dois segundos durante a calibração. O jogo identifica automaticamente a porta USB da ESP32. Cada gesto sai do centro, gira a palma de forma confortável e volta ao centro antes do próximo. Na fase 3, ELO troca de rota automaticamente após três giros individuais.

No menu, use as setas e `Enter`. `Espaço`, `P` ou `Esc` abre a pausa; nela é possível voltar ao menu. No modo teclado, cada toque em uma seta representa um giro. A tela **Como jogar** apresenta os objetivos e controles. O jogo tem trilhas e efeitos sonoros nas três fases e no menu.

## Conteúdo do pacote

- `Executaveis/ReCode`: dois executáveis Windows, dependências e ambiente Java integrado.
- `Projeto`: código fonte do jogo, recursos, firmware e documentação.
- `Documentos`: atalhos para os arquivos Word principais, incluindo short paper, relatório final, GDD, personagens e montagem IoT.

O código fonte e a documentação também estão no [repositório do ReCode](https://github.com/Pinkpie186/recode-iot-libgdx). O firmware utilizado está em `Projeto/prototipo_pedal/firmware/recode_pedal/recode_pedal.ino`.

## Verificação da entrega

Em 9 de novembro de 2026, o firmware da ESP32 foi identificado na COM3 como USB Serial CH9102. Uma leitura recebeu 201 quadros consecutivos em estado `ok` durante 20 segundos e o contador avançou de 3 para 7. O leitor serial do jogo recebeu 149 quadros em 15 segundos e aceitou quatro giros reais, sem erro de conexão. A verificação automatizada da campanha passou e os dois executáveis abriram no Windows.

As contagens e os ângulos configurados são regras do jogo. Os resultados apresentados não medem recuperação clínica.
