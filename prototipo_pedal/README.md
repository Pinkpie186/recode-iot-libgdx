# ReCode: jogo com giros do antebraço

O ReCode reúne **Circuitos Básicos**, **Rede Instável** e **Sistema Central** em uma campanha libGDX. Um MPU6050 preso ao antebraço informa ao ESP32 os giros detectados. O jogo usa a sequência e o intervalo entre giros para mover ELO, restaurar a rede e carregar o escudo do mascote. Fase, integridade e desempenho continuam entre as três áreas.

Os números de giros e ângulos abaixo são regras do jogo configuráveis. Não são metas de exercício ou prescrição de reabilitação. A montagem, o conforto e a calibração precisam ser conferidos com o dispositivo real antes de usar o controle com uma pessoa.

## Dispositivo

- ESP32, módulo MPU6050, cabo USB de dados e uma fixação confortável para o antebraço.
- Alimente o MPU6050 com **3,3 V**; ligue GND ao GND do ESP32, SDA ao GPIO 21 e SCL ao GPIO 22. O firmware procura os endereços I²C `0x68` e `0x69`.
- Fixe o **eixo X do módulo ao longo do antebraço**. A rotação em torno desse eixo faz o acelerômetro medir as posições para cima e para baixo. O antebraço deve ficar em uma postura na qual essa inclinação seja perceptível pelo sensor.
- Ao ligar, mantenha o antebraço na posição central e estável por cerca de **2 segundos** para calibrar. A tela mostra a solicitação durante a calibração. Cada giro deve sair da posição central, alcançar um lado e voltar ao centro antes do próximo registro.

O firmware está em `firmware/recode_pedal/recode_pedal.ino`. O nome antigo da pasta foi mantido para não quebrar caminhos do projeto; o código agora usa **MPU6050**, sem pedal nem chave magnética. Abra o arquivo na Arduino IDE com a placa ESP32 configurada e grave no dispositivo. Se cima/baixo aparecerem invertidos, altere `DIRECTION_SIGN` de `1` para `-1` no início do firmware e grave novamente.

### Ambiente preparado a partir do laboratório do professor

O exemplo `MovimentoSprite` do laboratório usa **libGDX**, **jSerialComm**, uma thread para ler a serial e uma ferramenta para listar portas. O ReCode mantém esses componentes no jogo existente. A thread está em `desktop/src/main/java/br/edu/recode/SerialPedalSource.java`; a listagem, em `SerialPortLister.java`. O exemplo do laboratório transmite textos como `Sentido: 1` em **9600 baud**. O firmware deste jogo transmite o JSON abaixo em **115200 baud**, pois a campanha também precisa do instante e do total de movimentos. Não grave o exemplo serial do laboratório no ESP32 para controlar o ReCode.

Antes de receber o equipamento, abra `Jogar-ReCode.cmd` para testar toda a campanha pelo teclado. Para conferir as portas disponíveis, execute `./Jogar-ReCode.ps1 -ListPorts` no PowerShell. O computador pode mostrar portas Bluetooth ou de outros dispositivos; escolha a porta que aparecer para o ESP32 depois de conectá-lo.

Para preparar a programação do ESP32, instale a [Arduino IDE](https://www.arduino.cc/en/software). Nas preferências, adicione o [índice estável de placas da Espressif](https://espressif.github.io/arduino-esp32/package_esp32_index.json) e instale **esp32 by Espressif Systems** pelo Gerenciador de Placas. Se tiver Arduino CLI, `./Preparar-ESP32.ps1 -InstallCore` instala o pacote e compila o sketch para o ESP32 clássico genérico. Sem CLI, o mesmo script mostra os passos para a IDE. Se a placa entregue for de outra variante, selecione o modelo exato na IDE ou informe o identificador dela em `-Board`; a pinagem I²C também precisa ser conferida para essa placa. A biblioteca `Wire` vem com a plataforma Arduino ESP32; o sketch não depende de bibliotecas externas para o MPU6050.

Quando o material chegar, conecte o MPU6050, grave `firmware/recode_pedal/recode_pedal.ino` e confira o JSON no Monitor Serial em 115200. **Feche o Monitor Serial antes de abrir o jogo**, porque a porta deve ficar livre. No Windows, abra `Jogar-ReCode-ESP32.cmd`, escolha a porta listada e jogue. Você também pode especificá-la diretamente: `./Jogar-ReCode.ps1 -Sensor -Port COM3`. Troque `COM3` pela porta real. Se a porta não aparecer, confira o cabo USB de dados e o driver USB da placa.

O programa lê a aceleração a cada 20 ms, suaviza o sinal e considera um giro quando a inclinação relativa ao centro alcança `25°`. Ele exige retorno à faixa central de `10°` antes de registrar outro giro e aplica um intervalo mínimo de `250 ms`. Leituras com aceleração total muito distante de 1 g são ignoradas para não confundir movimentos bruscos com postura. Esses limites são **parâmetros técnicos ajustáveis**, a conferir na montagem real.

O ESP32 envia uma linha JSON a cada 100 ms em **115200 baud**:

```json
{"v":2,"seq":42,"ms":123456,"mode":"forearm","movement":7,"lastMovementMs":122900,"direction":-1,"state":"ok"}
```

`movement` é o total acumulado de giros; `lastMovementMs` é o instante do último giro; `direction` vale `-1` para a posição inferior, `1` para a superior e `0` antes do primeiro gesto. Durante a calibração, `state` vale `calibrating`; se o MPU6050 não responder, vale `sensor_error`. O jogo mostra essas condições e só pontua quadros com `state: ok`. O firmware antigo de pedal não é aceito.

As referências de registradores e alimentação são o [mapa de registradores do MPU6050](https://invensense.tdk.com/wp-content/uploads/2015/02/MPU-6000-Register-Map1.pdf) e a [especificação do fabricante](https://invensense.tdk.com/wp-content/uploads/2015/02/MPU-6000-Datasheet.pdf).

## Jogar

O desktop usa JDK 17, libGDX 1.14.2 e jSerialComm 2.11.4. Na pasta `desktop/`, execute:

```text
gradle run -Pport=COM3
```

Troque `COM3` pela porta do ESP32. A primeira mensagem válida estabelece o ponto de partida; só os giros seguintes avançam o jogo. Se a conexão ficar sem dados por mais de 2 segundos, a progressão para até a comunicação retornar.

**Sem o sensor conectado no Windows:** abra `Jogar-ReCode.cmd`. No menu, pressione `Enter` para jogar. `F` alterna o lado do giro; as setas **baixo** e **cima** simulam diretamente cada direção. No modo teclado, um toque representa um giro detectado. Também é possível usar `gradle run -Pkeyboard=true`. O modo automático `gradle run -Psimulate=true` gera giros alternados para conferir a campanha.

No menu, use setas ou clique e confirme com `Enter`. Cada fase começa com uma apresentação do objetivo e aguarda `Enter` ou toque para iniciar; os giros feitos nessa tela não contam. Ao concluir uma fase, o jogo mostra um resumo e aguarda a pessoa escolher quando continuar. Não há contagem regressiva nessa pausa entre áreas.

Durante o jogo, `Espaço`, `P` ou `Esc` abre a pausa. Ela permite continuar, voltar ao menu ou iniciar outra campanha. Voltar ao menu preserva a tentativa atual; giros durante a pausa não contam. No menu principal e na pausa, **Efeitos de fundo: suaves** reduz os movimentos decorativos e o tremor ao receber um ataque. A câmera continua acompanhando o avanço de ELO. Essa preferência vale durante a execução atual do jogo.

## As três fases

1. **Circuitos Básicos:** os giros de antebraço conduzem ELO por três reparos e fragmentos de energia.
2. **Rede Instável:** os nós são estabilizados por sequências de giros regulares. O jogo compara cada intervalo com o anterior do próprio jogador; não há velocidade mínima.
3. **Sistema Central:** Nó Zero bloqueia alternadamente as linhas. Depois de três giros individuais de precisão, ELO para na chave. **Gire para a posição inferior para descer**, ou para a **superior para subir**, conforme a indicação na tela. Um giro no sentido oposto mantém ELO na linha atual. Após acionar a chave, complete os pares de ritmo e estabilidade para restaurar o escudo e avançar.

Na Rede Instável e no Sistema Central, três pares de giros regulares carregam o escudo do mascote por **15 segundos de jogo**. Ele bloqueia os ataques de Nó Zero. O resultado final preserva os giros, objetivos, pares regulares, fragmentos e tempo de movimento de cada fase.

Para conferir a lógica sem abrir a janela, execute `gradle verifyCampaign` na pasta `desktop/`. Para abrir a fase 3 separadamente, use `gradle run -Pphase=3 -Pkeyboard=true`. Os resultados da tentativa aparecem ao final da campanha, com giros, objetivos, pares regulares e tempo com movimento por fase. Esses números descrevem apenas o desempenho no jogo.

## Conferência com o MPU6050

1. Ligue o ESP32 e observe o monitor serial em 115200 baud. `state` deve passar de `calibrating` para `ok` com o antebraço estável.
2. Volte ao centro entre giros. Confira que cada gesto aumenta `movement` uma vez e que `direction` corresponde ao lado mostrado no jogo.
3. Abra a campanha na porta serial, gire o antebraço em ritmo confortável e confira a progressão e a indicação de movimentos por minuto.
4. Na fase 3, pare na chave de rota; confirme que o giro pedido muda ELO de linha e o giro no sentido contrário não muda.

O MPU6050 contém acelerômetro e giroscópio, mas este firmware usa **somente o acelerômetro** para estimar a inclinação em relação à postura inicial. A calibração e o resultado dependem da posição e da fixação do módulo; se o antebraço ou o sensor forem montados em outro eixo, ajuste a orientação e repita a conferência.
