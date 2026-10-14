# ReCode: primeiro protótipo com mini pedal

Este protótipo liga **uma volta da manivela do mini pedal** a um passo de ELO em
**Circuitos Básicos**. No modo real, o ESP32 envia a contagem por USB serial;
o jogo libGDX avança somente com voltas válidas. ELO percorre uma trilha de
energia e restaura três circuitos ao atingir 5, 10 e 15 voltas. Em 20 voltas,
aparece a conclusão da demonstração. Esses números são parâmetros técnicos
provisórios, não metas de exercício. Os bloqueios ainda têm desenho provisório.
Há também uma simulação identificada para desenvolver a tela.
Ainda não é o jogo completo nem uma ferramenta de avaliação clínica.

## Materiais

- Mini pedal ergométrico.
- ESP32 com cabo USB de dados.
- Ímã pequeno e chave reed normalmente aberta (ou módulo reed com saída digital
  de 3,3 V; nesse caso, conferir a ligação específica do módulo).
- Suporte e fixação para manter o ímã na parte giratória e a chave na parte fixa,
  sem contato entre eles e sem interferir no uso do pedal.

## Ligação proposta da chave reed simples

| Ponto | Ligação |
| --- | --- |
| Uma ponta da chave | GPIO 27 do ESP32 |
| Outra ponta da chave | GND do ESP32 |

O firmware usa `INPUT_PULLUP`: a passagem do ímã fecha a chave e leva o pino a
GND. **Não aplicar 5 V ao GPIO do ESP32.** A fixação deve resistir ao movimento
sem expor fios ou peças soltas. A distância entre ímã e sensor depende do modelo
e deve ser ajustada por teste manual, com o pedal parado.

## Firmware

Abra `firmware/recode_pedal/recode_pedal.ino` na Arduino IDE com a placa ESP32
configurada. Ajuste `SENSOR_PIN` se usar outro GPIO. Grave no ESP32 e abra o
monitor serial em **115200 baud**. Uma volta deve incrementar `cycle` uma vez.
O ESP32 envia uma linha JSON a cada 100 ms, inclusive quando o pedal está parado:

```json
{"v":1,"seq":42,"ms":123456,"mode":"pedal","cycle":7,"lastCycleMs":122900,"state":"ok"}
```

`cycle` é cumulativo desde a inicialização; `lastCycleMs` marca a última volta
para estimar RPM a partir do intervalo entre voltas. `state: ok` confirma a execução do
firmware e a transmissão, **não** detecta cabo solto na chave reed. A comparação
manual de voltas é necessária para verificar a montagem.

## Aplicativo desktop

O aplicativo está em `desktop/` e usa libGDX 1.14.2 e jSerialComm 2.11.4.
As folhas originais de ELO, do mascote e de Nó Zero ficam em
`desktop/src/main/resources/sprites/`. Cada folha tem 16 quadros em uma grade
4 × 4; o recorte ocorre no libGDX, sem alterar os PNGs. Nesta primeira fase,
ELO usa repouso, movimento e restauração; o mascote flutua, acompanha e orienta.
A folha de Nó Zero está pronta para uma fase posterior e não aparece aqui.
Os três cenários em `desktop/src/main/resources/scenarios/` representam as fases
definidas no GDD: a câmara de circuitos em azul/ciano, a rede fragmentada em
azul e magenta e o núcleo central em violeta. O cenário de Circuitos Básicos
aparece na fase jogável. Os outros dois são prévias visuais; suas mecânicas e
condições de conclusão ainda não foram implementadas.
É preciso instalar um JDK 17 e Gradle, pois este pacote não inclui um wrapper
nem binários. No terminal, dentro de `desktop/`:

```text
gradle run -Pport=COM3
```

Substitua `COM3` pela porta exibida para o ESP32 no seu computador. O programa
não escolhe uma porta automaticamente. Na primeira mensagem válida ele estabelece
o ponto de partida; as voltas **seguintes** avançam ELO. Isso evita pontuar
voltas anteriores à abertura do jogo.

### Trabalhar antes de ter as peças

Com JDK 17 e Gradle instalados, abra `desktop/` no terminal e execute:

```text
gradle run -Psimulate=true
```

O modo de simulação gera uma volta por segundo (60 RPM) e alimenta **o mesmo
parser e contador** usados pelo ESP32. A tela mostra claramente “MODO SIMULADO”.
Ele serve para conferir o avanço de ELO, a reparação dos três circuitos, a tela
de conclusão e o tratamento dos dados enquanto as peças não chegam. Observe
os bloqueios mudarem de vermelho para verde nas voltas 5, 10 e 15. A tela de
conclusão aparece na volta 20. A simulação não demonstra controle via IoT e não atende
sozinho ao requisito de avaliação do projeto. Para testar o dispositivo real,
feche a simulação e use `-Pport=COM3` com a porta correta.

### Visualizar os próximos cenários

Sem precisar do pedal, execute um comando de cada vez na pasta `desktop/`:

```text
gradle run -Ppreview=rede
gradle run -Ppreview=central
```

Cada comando abre apenas a arte da fase indicada, com o aviso de prévia visual.
Feche a janela para encerrar. Essas prévias não representam jogabilidade pronta.

## Verificação na bancada

1. Com o mini pedal parado, confirmar que `cycle` permanece constante.
2. Girar a manivela lentamente 10 voltas completas e comparar com o aumento de
   `cycle`. Repetir em ritmo confortável e observar contagens duplicadas.
3. Abrir o jogo, esperar a indicação de conexão e completar 5 voltas; conferir
   que ELO avança 5 unidades de reparo.
4. Desconectar o USB: o jogo deve indicar perda de dados em até 2 s e parar de
   avançar. Reconectar exige reiniciar o aplicativo nesta primeira versão.

O limiar de rejeição de pulsos abaixo de 180 ms e a meta de 20 voltas são
parâmetros **de engenharia para teste**, não metas de exercício. A duração, a
intensidade e a adequação do movimento de pedalada para reabilitação continuam
pendentes de validação profissional.

## Escopo após este protótipo

Adicionar calibração de cadência individual, menu/pausa/reinício controlados por
IoT, as demais fases, métricas persistidas e tratamento de reconexão. O desenho
final de ELO pode substituir a forma provisória sem alterar a leitura do pedal.
